(ns nagaino.test.expandurls
  (:use [nagaino.expandurls]
	[clojure.test] ))

;;; tests for nagaino.expandurls

(deftest test-seq->prefix-search-regex
  (let [re (seq->prefix-search-regex ["aa" "bb" ".*"])]
    (is (re-find re "aaa"))
    (is (re-find re "bbb"))
    (is (re-find re ".*"))
    (is (not (re-find re "xaa")))
    (is (not (re-find re "xbb")))
    (is (not (re-find re "x.*"))) ))

(deftest test-nagaino-url
  (let [url "http://example.com/"
	n-url (string->nagaino-url url) ]
    (is (= (:short_url n-url) url))
    (is (not (:done? n-url)))
    (let [n-url-1 (assoc n-url :done? true :cached true)
	  n-url-1-map (nagaino-url->map n-url-1) ]
      (is (not (contains? n-url-1-map :cached)))
      (is (not (contains? n-url-1-map :done)))
      (is (not (contains? n-url-1-map :error))) )
    (let [n-url-2 (assoc n-url :done? true :error true)
	  n-url-2-map (nagaino-url->map n-url-2) ]
      (is (not (contains? n-url-2-map :cached)))
      (is (not (contains? n-url-2-map :done)))
      (is (contains? n-url-2-map :error)) )))

(deftest test-map-array->map
  (is (= (map-array->map [{:k :a, :v 10} {:k :b, :v 20} {:k :c, :v 30}]
			 :k :v )
	 {:a 10, :b 20, :c, 30} )))

(deftest test-do-update-done
  (let [long-url "http://example.jp/"
	n-url (string->nagaino-url "http://example.com/")
	n-url-1 (assoc-cons n-url :long_url_path long-url)
	n-url-done (do-update-done n-url-1) ]
    (is (contains? n-url-done :done?))
    (is (= (:long_url n-url-done) long-url)) ))

(deftest test-update-done
  (let [n-url-1 (string->nagaino-url "http://t.co/")
	n-url-2 (assoc-cons n-url-1 :long_url_path "http://example.com/") ]
    (is (-> n-url-1 update-done :done? not))
    (is (-> n-url-2 update-done :done?)) ))

(deftest test-update-looped
  (let [url "http://example.com/"
	n-url-1 (assoc-cons (string->nagaino-url url) :long_url_path url)
	n-url-2 (update-looped n-url-1) ]
    (is (contains? n-url-2 :done?))
    (is (:error n-url-2))
    (is (= (rest (:long_url n-url-2)) ())) ))

(deftest test-assoc-cons
  (let [lst '(a b)
	m {:lst lst}
	item 'aa
	m2 (assoc-cons m :lst item) ]
    (is (= (first (:lst m2)) item))
    (is (= (rest (:lst m2)) lst)) ))
