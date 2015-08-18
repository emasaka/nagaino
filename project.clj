(defproject nagaino "0.0.1-SNAPSHOT"
  :description "bulk expander of shortened URLs"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-http "1.0.1"]
                 [compojure "1.3.4"]
                 [hiccup "1.0.5"]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-servlet "1.3.2"]
                 [congomongo "0.4.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [me.geso/regexp-trie "1.0.2"] ]
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}}
  :plugins [[lein-ring "0.9.6"]]
  :ring {:handler nagaino.core/app}
  :clean-targets ^{:protect false} [:target-path
                                    "resources/public/js/nagainolet.js" ]
  :min-lein-version "2.0.0" )
