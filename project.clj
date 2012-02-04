(defproject nagaino "0.0.1-SNAPSHOT"
  :description "bulk expander of shortened URLs"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [clj-http "0.3.1"]
                 [compojure "1.0.1"]
                 [hiccup "0.3.8"]
                 [ring "1.0.2"]
                 [congomongo "0.1.7"] ]
  :dev-dependencies [[swank-clojure "1.3.4"]]
  :main nagaino.core )
