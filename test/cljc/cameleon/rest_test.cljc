(ns cameleon.rest-test
  (:require #?(:cljs [cljs.test    :as t
                      :refer-macros [is are deftest testing use-fixtures async]]
               :clj  [clojure.test :as t
                      :refer        [is are deftest testing use-fixtures]])
            [cameleon.http-test :as http-test]
            [cameleon.rest :as rest]
            [promesa.core :as p]
            ))

(use-fixtures :once (http-test/http-fixture (namespace ::_)))

(def test-url "http://jsonplaceholder.typicode.com/posts")
(def test-request-body {:title "foo" :body "bar" :userId 1})

(deftest post-test
  (let [p (-> (rest/async-send :post test-url test-request-body)
              (p/then #(-> % :body :id)))]
    #?(:cljs (async done
                    (p/then p (fn [n]
                                (is (= 101 n))
                                (done))))
       :clj (is (= 101 @p)))))
