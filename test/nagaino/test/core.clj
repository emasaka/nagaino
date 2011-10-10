(ns nagaino.test.core
  (:use [nagaino.core]
	[clojure.test] ))

(deftest test-transform-result
  (let [urls [{:short_url "short1" :long_url "long1"}
	      {:short_url "short2" :long_url "long2"}
	      {:short_url "short3" :long_url "long3"} ]]
    (is (= (transform-result "json_hash" urls)
	   {"short1" {:short_url "short1" :long_url "long1"}
	    "short2" {:short_url "short2" :long_url "long2"}
	    "short3" {:short_url "short3" :long_url "long3"}} ))
    (is (= (transform-result "json_hash_simple" urls)
	   {"short1" "long1", "short2" "long2", "short3" "long3"} ))
    (is (= (transform-result "json" urls) urls)) ))
