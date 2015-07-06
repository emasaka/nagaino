(ns nagaino.test.core
  (:require [nagaino.core :refer :all]
            [clojure.test :refer :all]
            [ring.mock.request :as mock] ))

(deftest test-transform-result
  (let [urls [{:short_url "short1" :long_url "long1"}
	      {:short_url "short2" :long_url "long2"}
	      {:short_url "short3" :long_url "long3"} ]]
    (is (= (transform-result "json_hash" urls)
	   {"short1" {:short_url "short1" :long_url "long1"}
	    "short2" {:short_url "short2" :long_url "long2"}
	    "short3" {:short_url "short3" :long_url "long3"}} ))
    (is (= (transform-result "json_simple" urls)
	   {"short1" "long1", "short2" "long2", "short3" "long3"} ))
    (is (= (transform-result "json" urls) urls)) ))

(deftest test-app
  (testing "top page route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (.startsWith (some-> response :headers (get "Content-Type"))
                       "text/html" )) )))
