;;Experimental support for rendering
;;with Nashorn
(ns vegafx.nashorn
  (:require [clojure.java.io :as io]
            [vegafx.rasterize :as raster])
  (:import [javax.script ScriptEngineManager]))

;;copied over...will centralize this...
(def default-opts
  {"background" "#fff" ;;ensure a solid background or else we get black/transparent.
   "theme"      "default"
   "actions"    true})

(def ^:dynamic *options* default-opts)

(def nashorn
  (.. (javax.script.ScriptEngineManager.)
      (getEngineByName "nashorn")))

(defn eval-js! [^String source]
  (.eval ^jdk.nashorn.api.scripting.NashornScriptEngine nashorn source))

(defn load-file! [^String path]
  (eval-js! (str "load('"
                 (str (io/as-url (io/file path)))
                 "')")))

(do (println [:loading-promise])
    (eval-js! "load('classpath:net/arnx/nashorn/lib/promise.js')")
    (println [:loading-vega])
    ;;(load-file! "./vega3.js")
    ;;change this to use io/resource...
    (load-file! "./resources/vega3.3.1.js")
    (println [:loading-clojure-interop])
    (load-file! "./resources/nashornclj.js"))

(defn eval-file! [^String path]
  (.eval ^jdk.nashorn.api.scripting.NashornScriptEngine nashorn
         ^java.io.FileReader (java.io.FileReader. path)))

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

