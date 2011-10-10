(ns nagaino.test.cache
  (:use [nagaino.cache]
	[clojure.test] ))

;;; helpers

(defn elm= [a b]
  (= (set a) (set b)) )

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

(deftest test-lists-starts-before
  (let [lst '("aa" "bb" "cc" "dd")]
    (is (elm= (lists-starts-before lst "aa" ()) ()))
    (is (elm= (lists-starts-before lst "bb" ())
	      '(("aa" "bb" "cc" "dd")) ))
    (is (elm= (lists-starts-before lst "cc" ())
	      '(("bb" "cc" "dd") ("aa" "bb" "cc" "dd")) ))
    (is (elm= (lists-starts-before lst "dd" ())
	      '(("bb" "cc" "dd") ("cc" "dd") ("aa" "bb" "cc" "dd")) )) ))
