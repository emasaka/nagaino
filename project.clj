(defproject nagaino "0.0.1-SNAPSHOT"
  :description "bulk expander of shortened URLs"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.0.0"]
                 [compojure "1.2.0"]
                 [hiccup "1.0.5"]
                 [ring "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [congomongo "0.4.4"] ]
  :main nagaino.core )
