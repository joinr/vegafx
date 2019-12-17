;;Experimental support for rendering
;;with Nashorn
(ns vegafx.nashorn
  (:require [clojure.java.io :as io]
            [vegafx.rasterize :as raster])
  (:import [javax.script ScriptEngineManager SimpleBindings]
           [jdk.nashorn.api.scripting NashornScriptEngineFactory]
           [org.mozilla.javascript Context Scriptable ScriptableObject] ))

;;copied over...will centralize this...
(def default-opts
  {"background" "#fff" ;;ensure a solid background or else we get black/transparent.
   "theme"      "default"
   "actions"    true
   :backend     :nashorn #_:rhino})

(def ^:dynamic *options* default-opts)

(defprotocol IJS
  (eval-js [this txt])
  (load-js-file [this path]))


;;failed experiment messing with rhino...
(def rhino-ctx (atom nil))

(defn put-rhino!
  ([scope name obj]
   (doto scope
     (ScriptableObject/putProperty name (Context/javaToJS obj scope)
                                   )))
  ([name obj] (put-rhino! (:scope @rhino-ctx) name obj)))

(defn get-rhino!
  ([scope name obj]
   (ScriptableObject/getProperty scope name))
  ([name obj] (get-rhino! (:scope @rhino-ctx) name)))

(defn ->rhino []
  (let [^Context    cx  (Context/enter)
        ^Scriptable scope  (.initStandardObjects cx)
        r {:context cx :scope scope}]
    (swap! rhino-ctx (fn [old]
                       (when old (Context/exit))
                       r))))

(def rhino (->rhino))

(defn unrwap [obj]
  (if (instance? org.mozilla.javascript.NativeJavaObject obj)
    (.unwrap ^org.mozilla.javascript.NativeJavaObject obj)
    obj))

(defn eval-rhino! [^String source]
  (some-> @rhino-ctx :context
          (.evaluateString (:scope @rhino-ctx) source "MySource" 1 nil)
          unrwap))

(defn load-rhino! [^String path]
  (eval-rhino! (slurp (io/file path))))


;;NAshorn bindings.
(def nashorn
  (-> (NashornScriptEngineFactory.)
      (.getScriptEngine (into-array String ["--language=es6" "-scripting"]))))

(extend-protocol IJS
  jdk.nashorn.api.scripting.NashornScriptEngine
  (eval-js [this txt]
    (.eval ^jdk.nashorn.api.scripting.NashornScriptEngine this ^String txt))
  (load-js-file [this path]
    (eval-js this (str "load('"
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

;;THis should load nashorn with polyfills necessary
;;for vega...
(defn load-nashorn []
  (do (println [:loading-polyfills])
      (load-file! "resources/nashornpolyfill.js")
      (load-file! "resources/polyfill.min.js")
      (load-file! "resources/fetch.umd.min.js")
      (load-file! "resources/runtime.min.js")
      (println [:loading-vega])
      (load-file! "resources/vega5es5.js")
      (println [:loading-clojure-interop])
      (load-file! "resources/nashornclj.js")
      ))

(defn relative-resource [path]
  (slurp (.getResourceAsStream (.getClassLoader clojure.lang.Symbol) path)))

;;failed rhino experiment.
(defn load-rhino []
  (do (put-rhino! "cljPromise" clojure.core/promise)
      (put-rhino! "cljDeliver" clojure.core/deliver)
      (println [:loading-timer])
      (load-file! "timer.js" #_(relative-resource "net/arnx/nashorn/lib/promise.js"))
      (println [:loading-promise])
      (load-file! "polypromise.js" #_(relative-resource "net/arnx/nashorn/lib/promise.js"))
      (println [:loading-vega])
      (load-file! "./resources/vega3.3.1.js")
      (println [:loading-clojure-interop])
      (load-file! "./resources/rhinoclj.js")
    ))

#_(load-rhino)
(load-nashorn)
;;make sure we have our clojure substrate loaded.

(defn bindings [m]
  (let [^SimpleBindings bs (SimpleBindings.)]
    (doseq [[k v] m]
      (.put bs (name k) v))
    bs))

(defn spec->svg [spec]
  (->> (str "viewToSVG(specToView(" spec "))")
       eval-js!
       deref))

(defn spec->svg-ref [spec]
  (->> (str "viewToSVG(specToView(" spec "))")
       eval-js!
       ))

(defn vega->image [spec tgt & {:keys [show? format options]
                               :or {format :png
                                    options *options*}}]
  (let [svg (spec->svg spec)]
    (case format
      :svg (spit (str tgt ".svg") svg)
      (raster/save-as-png svg (str tgt ".png")))))

