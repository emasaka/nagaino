(defproject nagaino "0.0.1-SNAPSHOT"
  :description "bulk expander of shortened URLs"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "2.0.1"]
                 [cheshire "5.5.0"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [congomongo "0.4.7"]
                 [org.clojure/tools.logging "0.3.1"]
                 [me.geso/regexp-trie "1.0.5"] ]
  :profiles {:dev {:dependencies [[ring/ring-mock "0.3.0"]]}}
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler nagaino.core/app}
  :clean-targets ^{:protect false} [:target-path
                                    "resources/public/js/nagainolet.js"
                                    "resources/public/doc/example.html"
                                    "resources/public/hosts.json" ]
  :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
  :min-lein-version "2.0.0" )
