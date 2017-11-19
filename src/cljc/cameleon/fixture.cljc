(ns cameleon.fixture
  (:require [cameleon.io :as io]
   ))

(defn fixture
  "Return a fixture to be used with {clojure|cljs}.test
  given before and after functions that take no arguments."
  [before-fn after-fn]
  #?(:clj (fn [tests]
            (before-fn)
            (tests)
            (after-fn))
     :cljs {:before before-fn
            :after after-fn}))

(def fixture-dir "test/fixtures")

(defn fixture-file
  [fixture-name]
  (io/to-file fixture-dir (str fixture-name ".edn")))

(defn write-fixture
  "Write fixture into a file in fixture-dir. The file argument must be
  a relative path, use cameleon.io/write-to to write in another directory."
  [file content]
  (println "Writing" (str file))
  (io/write-to fixture-dir file content))

(defn read-fixture
  "Read fixture from a file in fixture-dir. The file argument must be
  a relative path, use cameleon.io/read-from to read from another directory."
  [file]
  (println "Reading" (str file))
  (io/read-from fixture-dir file))

(defn read-edn-fixture
  "Read fixture from a file in fixture-dir. The file argument must be
  a relative path, use cameleon.io/read-edn-from to read from another directory."
  [file]
  (println "Reading" (str file))
  (io/read-edn-from fixture-dir file))
