(ns vegafx.rasterize
  (:import [org.apache.batik.transcoder.image ImageTranscoder
            JPEGTranscoder PNGTranscoder]
           [org.apache.batik.transcoder TranscoderInput TranscoderOutput]
           [org.apache.commons.io IOUtils])
  (:require [clojure.java.io :as io]
            [clojure.string]))

(def defaults
  {:background java.awt.Color/WHITE })

(def ^:dynamic *options* defaults)
;;Note: this respects the background options like normal.
;;we can override either from vega or during rasterization.

(defn with-background [^ImageTranscoder t color]
  (if color
    (doto t
      (.addTranscodingHint
       ImageTranscoder/KEY_BACKGROUND_COLOR
       color))
    t))

(defn string->bytes ^bytes [^String svg]
  (IOUtils/toByteArray svg))

(defn save-as-jpeg [svg])
;;for now we define a path..

(defn svg-stream [x]
  (if  (clojure.string/includes?  x "<svg")
    (-> x
        java.io.StringReader.)
    (-> x
        io/file
        io/as-url
        str)))

(defn save-as-png  [path tgt & {:keys [background]}]
  (with-open [^java.io.OutputStream ostream (io/output-stream
                                             (io/file tgt))]
    (let [background        (or background
                                (get *options* :background))
          ^PNGTranscoder    t      (-> (PNGTranscoder.)
                                       (with-background background))
          ^TranscoderInput  input  (-> path
                                       svg-stream
                                       (TranscoderInput.))
          ^TranscoderOutput output (TranscoderOutput. ostream)]
      (.transcode t input output))))

#_(defn spit-image-bytes [{:keys [type buffer]} tgt]
  (let [ext (case type
              "data:image/jpeg;base64" "jpeg"
              "data:image/png;base64"  "png"
              "jpg")
        path (str tgt "." ext)]
    (with-open [out (io/output-stream (io/file path))]
      (.write out ^bytes buffer))))

;;higher quality?
#_(defn save-as-png  [path tgt & {:keys [background-color]}]
  (with-open [^java.io.OutputStream ostream (io/output-stream
                                             (io/file tgt))
              ^java.io.ByteArrayOutputStream os
                                   (java.io.ByteArrayOutputStream.)]
    (let [background        (or background-color
                                (get *options* :background-color))
          ^PNGTranscoder    t      (-> (PNGTranscoder.)
                                       (with-background background))
          ^TranscoderInput  input  (-> path
                                       io/file
                                       io/as-url
                                       slurp
                                       string->bytes
                                       (java.io.ByteArrayInputStream.)
                                       (TranscoderInput.))
          ^TranscoderOutput output (TranscoderOutput. os)
          _                        (.transcode t input output)]
      (.write ostream (.toByteArray os)))))
