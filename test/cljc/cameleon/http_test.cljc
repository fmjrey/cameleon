(ns cameleon.http-test
  (:require #?(:cljs [cljs.test    :as t
                      :refer-macros [is are deftest testing use-fixtures async]]
               :clj  [clojure.test :as t
                      :refer        [is are deftest testing use-fixtures]])
            [mount.core :as mount]
            [cameleon.io :as io]
            [cameleon.fixture :as fixture]
            [cameleon.http :as http]
            [cameleon.http-client-mock :as mock]
            [promesa.core :as p]
            ))

(def test-url "http://httpstat.us/200")
(def test-response-body "200 OK")

(defn http-fixture
  "Return a fixture to be used with clojure.test/fixture that can record to
  or read from a fixture file. The argument must be a string which will be
  used to extrapolate the fixture file name inside cameleon.fixture/fixture-dir.
  If the file is present, its content will be used to mock the http requests
  and responses it contains, otherwise it will be created from recording
  http traffic during the life of the fixture."
  [fixture-name]
  (let [fixture-file (fixture/fixture-file fixture-name)
        fixture? (io/exists? fixture-file)
        record? (not fixture?)
        session (when record? (mock/new-session))
        client (cond
                 fixture? (mock/mock-from fixture-file)
                 record? (mock/record-into session)
                 :else (mock/default-client))
        before (fn []
                 (-> (mount/swap {#'cameleon.http/client client})
                     (mount/start)))
        after (fn []
                (mount/stop)
                (when record?
                  (mock/write-session fixture-file @session)))]
    (fixture/fixture before after)))

(use-fixtures :once (http-fixture (namespace ::_)))

(deftest async-send-test
  (let [p (http/async-send :get test-url nil nil)]
  #?(:cljs (async done
                  (p/then p (fn [r]
                              (is (= test-response-body (-> r :body str)))
                              (done))))
    :clj (is (= test-response-body (-> @p :body io/to-string))))))
