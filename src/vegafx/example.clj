(ns vegafx.example
  (:require [vegafx.core :as vfx]
            [vegafx.config :as config]))

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

;;temp
(defn test-batch []
  (let [colors ["red" "white" "blue"]]
    (doseq [c colors]
      (let [spec (assoc test-spec "background" c)
            tgt  (str "./examples/" c)]
        (println [:spitting c])
        (vfx/vega->image spec tgt)
        (vfx/vega->image spec tgt :format :svg)
        (vfx/vega->html  spec tgt)))))


(defn theme-batch []
  (doseq [t config/themes]
    (let [tgt  (str "./examples/" t)]
      (binding [vfx/*options* (assoc vfx/*options* "theme" t)]
        (println [:spitting t])
        (vfx/vega->image test-spec tgt)
        (vfx/vega->image test-spec tgt :format :svg)
        (vfx/vega->html  test-spec tgt)))))
