;;Experimental support for rendering
;;with Nashorn
(ns vegafx.nashorn
  (:require [clojure.java.io :as io]
            [vegafx.rasterize :as raster])
  (:import [javax.script ScriptEngineManager]
           [jdk.nashorn.api.scripting NashornScriptEngineFactory]
           [org.mozilla.javascript Context Scriptable] ))

;;copied over...will centralize this...
(def default-opts
  {"background" "#fff" ;;ensure a solid background or else we get black/transparent.
   "theme"      "default"
   "actions"    true
   :backend     :rhino})

(def ^:dynamic *options* default-opts)

(defprotocol IJS
  (eval-js [this txt])
  (load-js-file [this path]))

(def rhino-ctx (atom nil))

(defn ->rhino []
  (let [^Context    cx  (Context/enter)
        ^Scriptable scope  (.initStandardObjects cx)
        r {:context cx :scope scope}]
    (swap! rhino-ctx (fn [old]
                       (when old (Context/exit))
                       r))))

(def rhino (->rhino))

(defn eval-rhino! [^String source]
  (some-> @rhino-ctx :context
          (.evaluateString (:scope @rhino-ctx) source "MySource" 1 nil)))

(defn load-rhino! [^String path]
  (let [file (io/file path)
        url (io/as-url file)]
    (with-open [rdr (io/reader url)]
      (some-> @rhino-ctx :context
              (.evaluateReader (:scope @rhino-ctx) ^java.io.Reader rdr path 1 nil)))))


(def nashorn
  (-> (NashornScriptEngineFactory.)
      (.getScriptEngine (into-array String ["--language=es6" "-scripting"]))))

(extend-protocol IJS
  jdk.nashorn.api.scripting.NashornScriptEngine
  (eval-js [this txt]
    (.eval ^jdk.nashorn.api.scripting.NashornScriptEngine this ^String txt))
  (load-js-file [this path]
    (eval-js! (str "load('"
                   (str (io/as-url (io/file path)))
                   "')")))
  clojure.lang.PersistentArrayMap
  (eval-js [this txt]
    (eval-rhino! txt))
  (load-js-file [this path]
    (load-rhino! path)))

(defn eval-js! [^String source & {:keys [backend]}]
  (eval-js (case (or backend (get *options* :backend :rhino))
             :nashorn nashorn
             rhino) source))

(defn load-file! [^String path  & {:keys [backend]}]
  (load-js-file (case (or backend (get *options* :backend :rhino))
                  :nashorn nashorn
                  rhino) path))

(defn load-nashorn []
  (do (println [:loading-promise])
      (eval-js! "load('classpath:net/arnx/nashorn/lib/promise.js')")
      (println [:loading-vega])
      ;;(load-file! "./vega3.js")
      ;;change this to use io/resource...
      (load-file! "./resources/vega3.3.1.js")
      (println [:loading-clojure-interop])
      (load-file! "./resources/nashornclj.js")
      ))

(defn load-rhino []
  (do (println [:loading-vega])
      (load-file! "./resources/vega3.3.1.js")
      (println [:loading-clojure-interop])
      (load-file! "./resources/rhinoclj.js")
    ))

;;make sure we have our clojure substrate loaded.

(defn spec->svg [spec]
  (->> (str "viewToSVG(specToView(" spec "))")
       eval-js!
       deref))

(defn vega->image [spec tgt & {:keys [show? format options]
                               :or {format :png
                                    options *options*}}]
  (let [svg (spec->svg spec)]
    (case format
      :svg (spit (str tgt ".svg") svg)
      (raster/save-as-png svg (str tgt ".png")))))

