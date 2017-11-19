(ns cameleon.repo-test
  (:require #?(:cljs [cljs.test    :as t
                      :refer-macros [is are deftest testing]]
               :clj  [clojure.test :as t
                      :refer        [is are deftest testing]])
            [cameleon.repo :as repo]
            ))

(deftest parse-uri-test
  (is (= {:uri "",
          :origin :unknown,
          :resource-type :unknown,
          :parsed? true}
         (repo/parse-uri "")))
  (is (= {:uri "{a {random} input}",
          :origin :unknown,
          :resource-type :unknown,
          :parsed? true}
         (repo/parse-uri "{a {random} input}")))
  (is (= {:uri "https://example.com:80/path/to/resource",
          :origin :remote,
          :resource-type :unknown,
          :scheme "https",
          :protocol "https:",
          :hostname "example.com",
          :port "80",
          :host "example.com:80",
          :pathname "/path/to/resource",
          :path "/path/to/resource",
          :parsed? true,
          }
         (repo/parse-uri "https://example.com:80/path/to/resource")))
  (is (= {:uri "git@github.com:fmjrey/cameleon.git",
          :origin :github,
          :resource-type :git-repo,
          :repo-owner "fmjrey",
          :repo-name "cameleon",
          :scheme "git",
          :protocol "git:",
          :hostname "github.com",
          :host "github.com",
          :pathname "fmjrey/cameleon.git",
          :path "fmjrey/cameleon.git",
          :parsed? true,
          }
         (repo/parse-uri "git@github.com:fmjrey/cameleon.git")))
  (is (= (repo/parse-uri "ssh@github.com:fmjrey/cameleon.git")
         {:uri "ssh@github.com:fmjrey/cameleon.git",
          :origin :github,
          :resource-type :git-repo,
          :repo-owner "fmjrey",
          :repo-name "cameleon",
          :scheme "ssh",
          :protocol "ssh:",
          :hostname "github.com",
          :host "github.com",
          :pathname "fmjrey/cameleon.git",
          :path "fmjrey/cameleon.git",
          :parsed? true,
          }))
  (is (= (repo/parse-uri "https://github.com/fmjrey/cameleon/blob/master/project.clj")
         {:uri "https://github.com/fmjrey/cameleon/blob/master/project.clj",
          :origin :github,
          :resource-type :file,
          :repo-owner "fmjrey",
          :repo-name "cameleon",
          :repo-branch "master",
          :repo-path "project.clj",
          :scheme "https",
          :protocol "https:",
          :hostname "github.com",
          :host "github.com",
          :pathname "/fmjrey/cameleon/blob/master/project.clj",
          :path "/fmjrey/cameleon/blob/master/project.clj",
          :parsed? true,
          }))
  (is (= {:uri "https://github.com/fmjrey/cameleon/tree/master/src/cljc",
          :origin :github,
          :resource-type :dir,
          :repo-owner "fmjrey",
          :repo-name "cameleon",
          :repo-branch "master",
          :repo-path "src/cljc",
          :scheme "https",
          :protocol "https:",
          :hostname "github.com",
          :host "github.com",
          :pathname "/fmjrey/cameleon/tree/master/src/cljc",
          :path "/fmjrey/cameleon/tree/master/src/cljc",
          :parsed? true,
          }
         (repo/parse-uri "https://github.com/fmjrey/cameleon/tree/master/src/cljc")))
  (is (= {:uri "https://github.com/fmjrey/cameleon/dummy/master/src/cljc",
          :origin :github,
          :resource-type :unknown,
          :scheme "https",
          :protocol "https:",
          :hostname "github.com",
          :host "github.com",
          :pathname "/fmjrey/cameleon/dummy/master/src/cljc",
          :path "/fmjrey/cameleon/dummy/master/src/cljc",
          :parsed? true,
          }
         (repo/parse-uri "https://github.com/fmjrey/cameleon/dummy/master/src/cljc")))
  (is (= {:uri "project.clj",
          :origin :local,
          :resource-type :file,
          :pathname "project.clj",
          :path "project.clj",
          :parsed? true,
          }
         (repo/parse-uri "project.clj")))
  (is (= {:uri "src/cljc",
          :origin :local,
          :resource-type :dir,
          :pathname "src/cljc",
          :path "src/cljc",
          :parsed? true,
          }
         (repo/parse-uri "src/cljc")))
  )
