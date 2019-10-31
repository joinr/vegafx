;;Dumb templating.  We just shove a vega spec into
;;the /resources/chart.html inline to avoid CORS
;;nonsense.
(ns vegafx.template
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(def template (slurp (io/resource "chart.html")))

;;Given a string of vega-spec, we splice it into the template.
;;Assumes that an external library already passed us something
;;conformant to a vega spec, e.g. JSON text.
(defn chart-html [vega-spec-txt]
  (clojure.string/replace template ":THE-SPEC" vega-spec-txt))

(defn temp-html [prefix]
  (doto (fs/temp-file prefix ".html")
    (.deleteOnExit)))

(defn spit-chart
  ([path spec]
   (let [f (fs/file path)]
     (spit f (vegafx.template/chart-html spec))
     f))
  ([spec]
   (spit-chart (temp-html "vegafx") spec)))

(defn test-html []
  (chart-html (slurp (io/resource "bar-chart.vg.json"))))
