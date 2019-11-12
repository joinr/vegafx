(defproject vegafx "0.2.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.mozilla/rhino "1.7.11"]
                 [cheshire "5.9.0"]
                 [net.arnx/nashorn-promise "0.1.2"]
                 [org.apache.xmlgraphics/batik-transcoder "1.11"]
                 [org.apache.xmlgraphics/batik-codec "1.11"]]
  :profiles {;;cribbed from fn-fx
             :openjfx11      ^:leaky   ; Ensure these dependencies "leak" through to the POM and JAR tasks
             {:dependencies [[org.openjfx/javafx-controls "11.0.2"]
                             [org.openjfx/javafx-swing    "11.0.2"]
                             [org.openjfx/javafx-media    "11.0.2"]
                             [org.openjfx/javafx-fxml     "11.0.2"]
                             [org.openjfx/javafx-web      "11.0.2"]
                             [org.openjfx/javafx-graphics "11.0.2"]]}}
  :jvm-options ["-Dnashorn.args=--language=es6"]
  :repl-options {}
  )
