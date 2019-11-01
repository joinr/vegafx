;;namespace for storing info about
;;plotting config, specs, etc.
(ns vegafx.config)

(def modes
  #{"vega" "vega-lite"})

(def renderer #{"canvas" "svg"})
(def themes
  #{"default"
    "excel"
    "ggplot2"
    "quartz"
    "vox"
    "fivethirtyeight"
    "dark"
    "latimes"
    "urbaninstitute"})
