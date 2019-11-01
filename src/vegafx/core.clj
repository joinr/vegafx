;;Derived from original work by Chris Neurnberger
;;https://gist.github.com/cnuernber/25e14a8b3a56c52faa8874afe5954ef8
(ns vegafx.core
  (:import [javafx.application Platform]
           [javafx.beans.value ChangeListener]
           [javafx.concurrent Worker$State]
           [javafx.embed.swing JFXPanel]
           [javafx.scene Scene]
           [javafx.scene.web WebView WebEvent WebEngine]
           [javafx.stage Stage]
           [javafx.embed.swing SwingFXUtils]
           [javax.imageio ImageIO])
  (:require [clojure.java.io :as io]
            [cheshire.core :as cheshire]))

;;looking at a better way to thread options, this is bush league.
(def default-opts
  {"background" "#fff" ;;ensure a solid background or else we get black/transparent.
   "theme"      "default"
   "actions"    true})

(def ^:dynamic *options* default-opts)

;;quickie translated  from https://stackoverflow.com/a/23979996
(defn b64->image-bytes ^bytes [data]
  (let [[type image]  (clojure.string/split  data #",")
        ^bytes image-bytes (javax.xml.bind.DatatypeConverter/parseBase64Binary
                            ^String image)]
    {:type type
     :buffer image-bytes}))

(defn spit-image-bytes [{:keys [type buffer]} tgt]
  (let [ext (case type
              "data:image/jpeg;base64" "jpeg"
              "data:image/png;base64"  "png"
              "jpg")
        path (str tgt "." ext)]
    (with-open [out (io/output-stream (io/file path))]
      (.write out ^bytes buffer))))

(defn spit-image [data tgt]
  (-> data b64->image-bytes (spit-image-bytes tgt)))

(defonce force-toolkit-init (javafx.embed.swing.JFXPanel.))

(defn run-later*
  [f]
  (javafx.application.Platform/runLater f))

(defmacro run-later
  [& body]
  `(run-later* (fn [] ~@body)))

(defn run-now*
  [f]
  (let [result (promise)]
    (run-later
     (deliver result (try (f) (catch Throwable e e))))
    @result))

(defmacro run-now
  [& body]
  `(run-now* (fn [] ~@body)))

(def ^:dynamic *buffer* 20)
(defonce ^:dynamic *web-view* (atom nil))

(defn eval-js [^String script]
  (let [e (.getEngine ^WebView  @*web-view*)]
    (run-now
     (.executeScript ^WebEngine e script))))

(def embed (slurp (clojure.java.io/resource "vegaembed.js")))

(defn vega-script [spec & {:keys [options] :or {options *options*}}]
  (let [options (cheshire/generate-string options)
        spec (if (string? spec)
               spec
               (cheshire/generate-string spec))]
    (-> embed
        (clojure.string/replace  "var options = {}"
                                 (str "var options = " options))
        (clojure.string/replace  "var spec = \"THE-SPEC\""
                                (str "var spec = " spec)))))

(defn static-script [script]
  (clojure.string/replace script "noisy = true" "noisy = false"))

(defn chart-html [edn & {:keys [options] :or {options *options*}}]
  (clojure.string/replace
   (slurp (io/resource "index.html"))
   "</body>"
   (clojure.string/join \newline ["</body>"
                                  "<script>"
                                  (static-script (vega-script edn :options options))
                                  "</script>"])))

;; engine.onAlert = new EventHandler<WebEvent<String>>()
;; {@Override
;;  void handle(WebEvent<String> event)
;;  {if("command:ready".equals(event.getData()))
;;   {//TODO: initialize
;;    }
;;   }
;;  }

;;we wait for an alert from the browser.
(defn ^javafx.event.EventHandler on-ready [f]
  (reify javafx.event.EventHandler
    (handle [this event]
      (f (.getData ^WebEvent event)))))

(def alerts
  {"data:image" :image-ready
   "chart-ready" :ready
   "image-ready" :image-ready})

(defn alert-dispatch [msg]
  (reduce-kv (fn [acc k v]
               (if (clojure.string/includes? msg k)
                 (reduced v)
                 acc)) nil alerts))

(defmulti alert-handler alert-dispatch)
(defmethod alert-handler :default [msg]
  {:alert :generic
   :data  msg})

(defmethod alert-handler :ready [msg]
  {:alert :ready
   :data  msg})

(defmethod alert-handler :image-ready [msg]
  {:alert :image-ready
   :data msg})

;;This a tad off...doesn't include legend.
(defn dimensions
  [engine id]
  {:height (.executeScript engine "document.getElementById('vis').offsetHeight")
   :width (.executeScript engine "document.getElementById('vis').offsetWidth")})

(defn vega-lite
  [edn & {:keys [show? options] :or {options *options*}}]
  (run-now
   (let [web-view (WebView.)
         engine (.getEngine web-view)
         ready  (promise)
         image-url (promise)
         show   (fn [^Stage x] (if show? (doto x (.show)) x))]
     (Platform/setImplicitExit false)
     (.. engine getLoadWorker stateProperty
         (addListener
          (reify ChangeListener
            (changed [this ob old new]
              ;; Wait until the page has loaded.
              (when (= new Worker$State/SUCCEEDED)
                (.executeScript engine (vega-script edn :options options))
                (let [{:keys [height width]} (dimensions engine "vis")]
                  (.setPrefSize web-view
                                (+ *buffer* width)
                                (+ *buffer* height))
                  (doto (Stage.)
                    (.setScene (Scene. web-view))
                    (show))))))))
     (.setOnAlert engine ^javafx.event.EventHandler
                  (on-ready (fn [msg]
                              (let [k (alert-handler msg)]
                                (case (:alert k)
                                  :ready       (deliver ready msg)
                                  :image-ready (deliver image-url msg)
                                  :generic     (deliver image-url msg))))))
     (reset! *web-view* web-view)
     (.loadContent engine (slurp (io/resource "index.html")))
     {:ready-promise ready
      :image-url     image-url})))


(defn capture-image [& {:keys [format] :or {format :png}}]
  (assert #{:png :svg "png" "svg"} format)
  (eval-js (str "getImage('" (name format) "')")))

(defn vega->image [spec tgt & {:keys [show? format options]
                               :or {format :png
                                    options *options*}}]
  (let [{:keys [ready-promise image-url ]} (vega-lite spec :show? show? :options options)
        _    @ready-promise
        _   (capture-image  :format format)]
    (case format
      :svg (spit (str tgt ".svg") @image-url)
      (spit-image @image-url tgt))))

(defn vega->html [spec tgt & {:keys [options] :or {options *options*}}]
  (spit (str tgt ".html") (chart-html spec :options options)))

(comment

  (set! *warn-on-reflection* true)
  (def test-spec {"$schema" "https://vega.github.io/schema/vega-lite/v2.0.json"
                  "data" {"values" [{"a" "A","b" 100}
                                    {"a" "B","b" 55}
                                    {"a" "C","b" 43}
                                    {"a" "D","b" 91}
                                    {"a" "E","b" 81}
                                    {"a" "F","b" 53}
                                    {"a" "G","b" 19}
                                    {"a" "H","b" 87}
                                    {"a" "I","b" 52}]}
                  "mark" "bar"
                  "encoding" {"x" {"field" "a", "type" "ordinal"}
                              "y" {"field" "b", "type" "quantitative"}}
                  "background" "white"})

  ;(vega-lite test-spec)
  ;;  (render-and-save test-spec)
  (def res (vega-lite test-spec))
  (spit-image (-> res :image-url deref) "blah")
  )


;;may still be useful?
#_(defn take-snapshot
    []
    (run-later
     (try
       (let [web-view @*web-view*
             jfx-img (.snapshot web-view
                                (javafx.scene.SnapshotParameters.)
                                nil)
             buf-img (SwingFXUtils/fromFXImage jfx-img nil)]
         (ImageIO/write buf-img "PNG" (java.io.File. "test.png")))
       (catch Throwable e
         (println e)))))

#_(defn render-and-save [spec & {:keys [show?]}]
    (let [plot (vega-lite spec :show? show?)
          _    @(:ready-promise plot)]
      (take-snapshot)))
