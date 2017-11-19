(ns cameleon.io-test
  (:require #?(:cljs [cljs.test    :as t
                      :refer-macros [is are deftest testing use-fixtures async]]
               :clj  [clojure.test :as t
                      :refer        [is are deftest testing use-fixtures]])
            [clojure.string :as str]
            [cameleon.io :as io]
            ))

(deftest tmpdir-exists-test
  (is (not (str/blank? (io/tmpdir))))
  (is (io/exists? (io/tmpdir)))
  (is (io/dir? (io/tmpdir))))

(def content {:number 1,
              :string "string"
              :boolean false
              :keyword :keyword
              :list '(1 2)
              :vector [1 2]
              :map {:a 1 :b 2}
              :set #{:a :b :c}})

(deftest io-tests
  (let [tmp-file (io/to-file (io/tmpdir) "io-tests.tmp")]
    (io/write-to tmp-file content)
    (is (= content (io/read-edn-from tmp-file)))))
