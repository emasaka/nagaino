(ns nagaino.test.expandurls
  (:use [nagaino.expandurls]
	[clojure.test] ))

;;; tests for nagaino.expandurls

(deftest test-read-from-resource-file
  (is (map? (read-from-resource-file "config.clj"))) )

(deftest test-seq->prefix-search-regex
  (let [re (seq->prefix-search-regex ["aa" "bb" "x."])]
    (is (re-find re "aaa"))
    (is (re-find re "bbb"))
    (is (re-find re "x."))
    (is (not (re-find re "xaa")))
    (is (not (re-find re "xbb")))
    (is (not (re-find re "xy"))) ))

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

(deftest test-parse-location-res
  (is (parse-location-res {:status 301
                           :headers {"location" "http://example.com"} })
      ["http://example.com" nil] )
  (is (parse-location-res {:status 404})
      [nil 404] ))

(deftest test-keywordize
  (is (keywordize {"foo" 3 "bar" 5}) {:foo 3 :bar 5}) )

(deftest test-parse-bitly-res
  (let [r1 (parse-bitly-res {:status 200 :body "{\"status_code\": 200, \"data\": {\"expand\": [{\"short_url\": \"http://foo.example.com/\", \"long_url\": \"http://example.com/foo/\"}, {\"short_url\": \"http://bar.example.com/\", \"long_url\": \"http://example.com/bar/\"}]}, \"status_txt\": \"OK\"}"} ["http://foo.example.com/" "http://bar.example.com/"]) ]
    (is r1 [{:short_url "http://foo.example.com/"
             :long_url "http://example.com/foo/" }
            {:short_url "http://bar.example.com/"
             :long_url "http://example.com/bar/" } ]))
  (let [r2 (parse-bitly-res {:status 404}
                            ["http://foo.example.com/" "http://bar.example.com/"])]
    (is (map #(:long_url) r2) [nil nil]) )
  (let [r3 (parse-bitly-res {:status 200 :body "{\"status_code\": 403, \"status_txt\": \"RATE_LIMIT_EXCEEDED\", \"data\" : null}"} ["http://foo.example.com/" "http://bar.example.com/"])]
    (is (map #(:long_url) r3) [nil nil]) ))


(deftest test-parse-htnto-res
  (let [r1 (parse-htnto-res {:status 200 :body "{\"status_code\": \"200\", \"data\": {\"expand\": [{\"short_url\": \"http://foo.example.com/\", \"long_url\": \"http://example.com/foo/\"}, {\"short_url\": \"http://bar.example.com/\", \"long_url\": \"http://example.com/bar/\"}]}, \"status_txt\": \"OK\"}"} ["http://foo.example.com/" "http://bar.example.com/"]) ]
    (is r1 [{:short_url "http://foo.example.com/"
             :long_url "http://example.com/foo/" }
            {:short_url "http://bar.example.com/"
             :long_url "http://example.com/bar/" } ]))
  (let [r2 (parse-htnto-res {:status 404}
                            ["http://foo.example.com/" "http://bar.example.com/"])]
    (is (map #(:long_url) r2) [nil nil]) ))

(deftest test-update-done
  (let [long-url "http://example.jp/"
	n-url (string->nagaino-url "http://example.com/")
	n-url-1 (add-url n-url long-url)
	n-url-done (update-done n-url-1) ]
    (is (contains? n-url-done :done?))
    (is (= (:long_url n-url-done) long-url)) ))

(deftest test-update-done
  (let [n-url-1 (string->nagaino-url "http://t.co/")
	n-url-2 (add-url n-url-1 "http://example.com/") ]
    (is (-> n-url-1 update-done :done? not))
    (is (-> n-url-2 update-done :done?)) ))

(deftest test-update-looped
  (let [url "http://example.com/"
	n-url-1 (add-url (string->nagaino-url url) url)
	n-url-2 (update-looped n-url-1) ]
    (is (contains? n-url-2 :done?))
    (is (:error n-url-2))
    (is (= (rest (:long_url n-url-2)) ())) ))

(deftest test-add-url
  (let [lst '("http://example.com/1")
	m {:long_url_path lst}
	item "http://example.com/2"
	m2 (add-url m item) ]
    (is (= (first (:long_url_path m2)) item))
    (is (= (rest (:long_url_path m2)) lst)) ))

(deftest test-expand-from-table
  (let [table {"http://example.com/1" (struct Expm "http://example.com/1"
					      "http://example.com/2" )
	       "http://example.com/2" (struct Expm "http://example.com/2"
					      "http://example.com/3" )
	       "http://example.com/11" (struct Expm "http://example.com/11"
					       "http://example.com/12" )
	       "http://example.com/21" nil
	       "http://example.com/31" (struct Expm "http://example.com/31"
					       nil "Not Found" )}
	n1 {:long_url_path '("http://example.com/1")}
	n2 {:long_url_path '("http://example.com/11")}
	n3 {:long_url_path '("http://example.com/21")}
	n4 {:long_url_path '("http://example.com/31")} ]
    (is (= (:long_url_path (expand-from-table table n1))
	   '("http://example.com/3" "http://example.com/2"
	     "http://example.com/1" )))
    (is (= (:long_url_path (expand-from-table table n2))
	   '("http://example.com/12" "http://example.com/11") ))
    (let [r (expand-from-table table n3)]
      (is r n3 ))
    (let [r (expand-from-table table n4)]
      (is (= (:long_url_path r) '("http://example.com/31")))
      (is (:done? r))
      (is (:error r)) ) ))
