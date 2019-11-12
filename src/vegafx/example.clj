(ns vegafx.example
  (:require [vegafx.core :as vfx]
            [vegafx.config :as config]
            [vegafx.nashorn :as headless]
            [clojure.java.io :as io]))

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

(defn remote-data [spec]
  (clojure.string/replace spec "data/" "https://raw.githubusercontent.com/vega/vega/master/docs/data/"))

(defn vega-example [name & {:keys [show? root] :or {show? true root "./examples/vega/"}}]
  (vfx/vega->image (remote-data (slurp (str "./examples/specs/" name ".vg.json")))
                   (str root name) :show? show?))

;;requires a non-standard vega extension using d3.
(def invalid-examples #{"projections"
                        ;;vl hangs on this one for some reason.
                        "interactive_bin_extent"
                        "scatter_image"
                        "text_multiline"
                        })
(defn list-examples []
  (->> (io/file "./examples/specs/")
       file-seq
       (filter (complement invalid-examples))
       (drop 1)
       (map #(clojure.string/replace (.getName %) ".vg.json" ""))))

(defn batch-examples []
  (doseq [x (list-examples)]
    (println [:rendering x])
    (try (vega-example x :show? false)
         (catch Exception e (println [:example x :failed-to-render!])))))


(defn vega-example-headless [name & {:keys [show? root]
                                     :or {root "./examples/vega/nashorn/"}}]
  (headless/vega->image (remote-data (slurp (str "./examples/specs/" name ".vg.json")))
                   (str root name) :show? show?))

(defn headless-batch []
  (doseq [f (list-examples)]
    (let [res   (promise)
          task   (future (try (do (vega-example-headless f)
                                  (deliver res :success))
                              (catch Exception e
                                (deliver res [:failed (.getMessage e)]))))
          timer  (future (Thread/sleep 3000) (when-not (realized? res)
                                               (do (deliver res :timed-out)
                                                   (future-cancel task))))]
      (println [f @res]))))

(defn list-vl-examples []
  (->> (io/file "./examples/specs/vl/")
       file-seq
       (filter (complement invalid-examples))
       (drop 1)
       (map #(clojure.string/replace (.getName %) ".vl.json" ""))))

(defn vl-example [name & {:keys [show? root] :or {show? true root "./examples/vega-lite/"}}]
  (vfx/vega->image (remote-data (slurp (str "./examples/specs/vl/" name ".vl.json")))
                   (str root name) :show? show?))

(defn batch-examples-vl
  ([xs]
   (doseq [x xs]
     (println [:rendering x])
     (try (vl-example x :show? false)
          (catch Exception e (println [:example x :failed-to-render!])))))
  ([] (batch-examples-vl (list-vl-examples))))

(defn headless-batch-vl
  ([xs]
  (doseq [f xs]
    (let [res   (promise)
          task   (future (try (do (vega-example-headless f)
                                  (deliver res :success))
                              (catch Exception e
                                (deliver res [:failed (.getMessage e)]))))
          timer  (future (Thread/sleep 3000) (when-not (realized? res)
                                               (do (deliver res :timed-out)
                                                   (future-cancel task))))]
      (println [f @res]))))
