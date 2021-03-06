(ns nagaino.test.cache
  (:require [nagaino.cache :refer :all]
            [clojure.test :refer :all] ))

;;; helpers

(defn elm= [a b]
  (= (group-by identity a) (group-by identity b)) )

(deftest test-test-helpers
  (is (elm= '(a b c) '(a b c)))
  (is (elm= '(a b c) '(c b a)))
  (is (not (elm= '(a b c) '(a b)))) )

;;; tests for nagaino.cache

(deftest test-split-mongo-url
  (is (= (split-mongo-url "mongodb://user:pass@example.com:27017/db")
	 {:match "mongodb://user:pass@example.com:27017/db",
	  :user "user" :pass "pass" :host "example.com" :port "27017"
	  :db "db" }))
  (is (= (split-mongo-url "mongodb://example.com:27017/db")
	 {:match "mongodb://example.com:27017/db",
	  :user nil :pass nil :host "example.com" :port "27017" :db "db" })) )

(deftest test-find-first
  (is (= (find-first even? (iterate inc 1)) 2)) )

(deftest test-lists-starts-before
  (let [lst '("aa" "bb" "cc" "dd")]
    (is (elm= (lists-starts-before lst "aa" ()) ()))
    (is (elm= (lists-starts-before lst "bb" ())
	      '(("aa" "bb" "cc" "dd")) ))
    (is (elm= (lists-starts-before lst "cc" ())
	      '(("bb" "cc" "dd") ("aa" "bb" "cc" "dd")) ))
    (is (elm= (lists-starts-before lst "dd" ())
	      '(("bb" "cc" "dd") ("cc" "dd") ("aa" "bb" "cc" "dd")) )) ))
