(ns cameleon.utils-test
  (:require #?(:cljs [cljs.test    :as t :refer-macros [is are deftest testing]]
               :clj  [clojure.test :as t :refer        [is are deftest testing]])
            [cameleon.utils :as utils]
            ))

(deftest update-frequency-tests
  (t/is (= {:a 1, :b 2, :c 1}
           (utils/update-frequency {:a 1, :b 1, :c 1} :b)))
  (t/is (= {:a 1, :b 3, :c 1}
           (utils/update-frequency {:a 1, :b 1, :c 1} :b 2)))
  (t/is (= {:a 1, :b 1, :c 1}
           (utils/update-frequency {:a 1, :c 1} :b)))
  (t/is (= {:a 1, :b 2, :c 1}
           (utils/update-frequency {:a 1, :c 1} :b 2))))
