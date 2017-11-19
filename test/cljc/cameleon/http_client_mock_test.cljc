(ns cameleon.http-client-mock-test
  (:require #?(:cljs [cljs.test    :as t
                      :refer-macros [is are deftest testing use-fixtures async]]
               :clj  [clojure.test :as t
                      :refer        [is are deftest testing use-fixtures]])
            [mount.core :as mount]
            [clojure.string :as string]
            [cameleon.io :as io]
            [cameleon.http :as http]
            [cameleon.http-test :as http-test]
            [cameleon.http-client-mock :as mock]
            [promesa.core :as p]
            [httpurr.protocols :as hp]
    #?(:clj [byte-streams :as bytes])
            ))

(def test-url "http://httpstat.us/200")
(def test-response-body "200 OK")

(deftest client-returning-test
  (let [mock-client (mock/client-returning test-response-body)
        p (http/async-send mock-client :get test-url nil nil)]
    #?(:cljs (async done
                    (p/then p (fn [r]
                                (is (= test-response-body (-> r :body str)))
                                (done))))
       :clj (is (= test-response-body (-> @p :body bytes/to-string))))))

(deftest failing-client-test
  (let [mock-client (mock/failing-client)
        p (http/async-send mock-client :get test-url nil nil)]
    #?(:cljs (async done
                    (-> p
                        (p/then (fn [r]
                                  (is false "Error not thrown.")
                                  (done)))
                        (p/catch (fn [e]
                                   (is (mock/mock-error? e))
                                   (done)))))
       :clj (is (thrown? Throwable (-> @p :body bytes/to-string))))))

(def mock-uris [[:whatever {:hosts ["whatever.com"]
                            :routes [["/path1" :path1]
                                     ["/path2" :path2]]}]
                [:test {:hosts ["test.com"]
                        :routes [["/path" :path]]}]
                [:any {:routes [["/login" :login]]}]])

(def mock-responses {[:whatever :path1] {:status 200
                                         :body "whatever path1"}
                     [:whatever :path2] {:status 200
                                         :body "whatever path2"}
                     [:test :path] {:status 200
                                    :body "test path"}
                     [:any :login] (fn [request matched-uri]
                                     {:status 401
                                      :body (str "Unauthorised login to "
                                                 (-> matched-uri
                                                     :parsed-uri
                                                     :hostname))})})
(def urls ["https://test.com/path"
           "https://whatever.com/path1"
           "https://test.com/path"
           "https://whatever.com/path2"
           "https://test.com/path"
           "https://test.com/login"
           "https://test.com/path"])

(def expected-responses [{:status 200, :body "test path"}
                         {:status 200, :body "whatever path1"}
                         {:status 200, :body "test path"}
                         {:status 200, :body "whatever path2"}
                         {:status 200, :body "test path"}
                         {:status 401, :body "Unauthorised login to test.com"}
                         {:status 200, :body "test path"}])

(deftest client-mock-test
  (let [mock-client (mock/mock-from mock-uris mock-responses)
        p (->> urls
               (map #(http/async-send mock-client :get % nil nil))
               p/all)]
    #?(:cljs (async done
                    (p/then p (fn [r]
                                (is (= expected-responses r))
                                (done))))
       :clj (is (= expected-responses @p)))))

(defn- status-200->404
  [{status :status :as response}]
  (if (= 200 status)
    {:status 404
     :body "404 Not found"}
    response))

(defn- response-handler-200->404
  [request options response-object]
  (reify hp/Response
    (-success? [_] (hp/-success? response-object))
    (-response [_] (status-200->404 (hp/-response response-object)))
    (-error [_] (hp/-error response-object))))

(deftest wrap-response-test
  (let [client (mock/mock-from mock-uris mock-responses)
        mock-client (mock/wrap-response client response-handler-200->404)
        p (->> urls
               (map #(http/async-send mock-client :get % nil nil))
               p/all)
        expected-responses (mapv status-200->404 expected-responses)]
    #?(:cljs (async done
                    (p/then p (fn [r]
                                (is (= expected-responses r))
                                (done))))
       :clj (is (= expected-responses @p)))))

(defn- path1<->2
  [s]
  (cond
    (string/includes? s "1") (string/replace s "1" "2")
    (string/includes? s "2") (string/replace s "2" "1")
    :else s))

(defn request-handler-path1<->2
  [request options]
  [(update-in request [:url] path1<->2) options])

(deftest wrap-request-test
  (let [client (mock/mock-from mock-uris mock-responses)
        mock-client (mock/wrap-request client request-handler-path1<->2)
        p (->> urls
               (map #(http/async-send mock-client :get % nil nil))
               p/all)
        expected-responses (mapv #(update-in % [:body] path1<->2)
                                 expected-responses)]
    #?(:cljs (async done
                    (p/then p (fn [r]
                                (is (= expected-responses r))
                                (done))))
       :clj (is (= expected-responses @p)))))

(deftest override-test
  (let [overriden-client (mock/client-returning 404 {} "Not found")
        overriding-client (mock/mock-from mock-uris mock-responses)
        mock-client (mock/override overriding-client overriden-client)
        p (->> (conj urls "http://not.in.mock/url")
               (map #(http/async-send mock-client :get % nil nil))
               p/all)
        expected-responses (conj expected-responses {:status 404,
                                                     :body "Not found"})]
    #?(:cljs (async done
                    (p/then p (fn [r]
                                (is (= expected-responses r))
                                (done))))
       :clj (is (= expected-responses @p)))))

(def expected-session
  {:req-opts-resp
   [[{:method  :get, :url     "https://test.com/path",
      :body    nil, :headers {}}
     {}
     {:success? true,
      :response {:status 200, :body "test path"},
      :error    nil}]
    [{:method  :get, :url     "https://whatever.com/path1",
      :body    nil, :headers {}}
     {}
     {:success? true,
      :response {:status 200, :body "whatever path1"},
      :error    nil}]
    [{:method  :get, :url     "https://test.com/path",
      :body    nil, :headers {}}
     {}
     {:success? true,
      :response {:status 200, :body "test path"},
      :error    nil}]
    [{:method  :get, :url     "https://whatever.com/path2",
      :body    nil, :headers {}}
     {}
     {:success? true,
      :response {:status 200, :body "whatever path2"},
      :error    nil}]
    [{:method  :get, :url     "https://test.com/path",
      :body    nil, :headers {}}
     {}
     {:success? true,
      :response {:status 200, :body "test path"},
      :error    nil}]
    [{:method  :get, :url     "https://test.com/login",
      :body    nil, :headers {}}
     {}
     {:success? true,
      :response
      {:status 401, :body "Unauthorised login to test.com"},
      :error    nil}]
    [{:method  :get, :url     "https://test.com/path",
      :body    nil, :headers {}}
     {}
     {:success? true,
      :response {:status 200, :body "test path"},
      :error    nil}]],
   :req-opts->responses
   {[{:method  :get, :url     "https://whatever.com/path2",
      :body    nil, :headers {}}
     {}]
    [{:success? true,
      :response {:status 200, :body "whatever path2"},
      :error    nil}],
    [{:method  :get, :url     "https://test.com/path",
      :body    nil, :headers {}}
     {}]
    [{:success? true, :response {:status 200, :body "test path"}, :error nil}
     {:success? true, :response {:status 200, :body "test path"}, :error nil}
     {:success? true, :response {:status 200, :body "test path"}, :error nil}
     {:success? true, :response {:status 200, :body "test path"}, :error nil}],
    [{:method  :get, :url     "https://whatever.com/path1",
      :body    nil, :headers {}}
     {}]
    [{:success? true, :response {:status 200, :body "whatever path1"}, :error nil}],
    [{:method  :get, :url     "https://test.com/login",
      :body    nil, :headers {}}
     {}]
    [{:success? true,
      :response
      {:status 401, :body "Unauthorised login to test.com"}, :error nil}]},
   :serialised? true})

(deftest write-read-session-test
  (let [tmp-file (io/to-file (io/tmpdir) "mock-session.tmp")]
    (mock/write-session tmp-file expected-session)
    (is (= expected-session (mock/read-session tmp-file)))))

(deftest record-into-test
  (let [recorded-client (mock/mock-from mock-uris mock-responses)
        session (mock/new-session)
        mock-client (mock/record-into session recorded-client)
        p (->> urls
               (map #(http/async-send mock-client :get % nil nil))
               p/all)
        test (fn [#?(:cljs done) r]
               (is (= expected-responses r))
               (is (= (:req-opts->responses expected-session)
                      (:req-opts->responses (mock/make-serialisable @session))))
               #?(:cljs (done)))]
    #?(:cljs (async done
                    (p/then p (partial test done)))
       :clj (test @p))))

(deftest mock-from-recorded-test
  (let [mock-client (mock/mock-from expected-session)
        p (->> urls
               (map #(http/async-send mock-client :get % nil nil))
               p/all)]
    #?(:cljs (async done
                    (p/then p (fn [r]
                                (is (= expected-responses r))
                                (done))))
       :clj (is (= expected-responses @p)))))

(deftest mock-from-recorded-rotation-test
  (let [twice (fn [v] (vec (concat v v)))
        mock-client (mock/mock-from expected-session)
        p (->> (twice urls)
               (map #(http/async-send mock-client :get % nil nil))
               p/all)
        expected-responses (twice expected-responses)]
    #?(:cljs (async done
                    (p/then p (fn [r]
                                (is (= expected-responses r))
                                (done))))
       :clj (is (= expected-responses @p)))))

(deftest mock-from-recorded-not-found-test
  (let [added-url "https://whatever.com/login"
        mock-client (mock/mock-from expected-session)
        p (->> (conj urls added-url)
               (map #(http/async-send mock-client :get % nil nil))
               p/all)]
    #?(:cljs (async done
                    (-> p
                        (p/then (fn [r]
                                  (is false "Error not thrown.")
                                  (done)))
                        (p/catch (fn [e]
                                   (is (mock/mock-error? e))
                                   (done)))))
       :clj (is (thrown? Throwable (-> @p :body bytes/to-string))))))

;; Ordering of promises is not guaranteed especially on the JVM.
;; By repeating the above tests we increase the chances of
;; detecting concurrency and ordering issues.
(t/deftest rinse-repeat
  (dotimes [_ 100]
    (record-into-test)
    ))
