(defproject nagaino "0.0.1-SNAPSHOT"
  :description "bulk expander of shortened URLs"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.0.1"]
                 [compojure "1.3.1"]
                 [hiccup "1.0.5"]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.2"]
                 [ring/ring-servlet "1.3.2"]
                 [congomongo "0.4.4"]
                 [org.clojure/tools.logging "0.3.1"]
                 [me.geso/regexp-trie "0.1.10"] ]
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}}
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler nagaino.core/app}
  :clean-targets ^{:protect false} [:target-path
                                    "resources/public/js/nagainolet.js" ]
  :min-lein-version "2.0.0" )
