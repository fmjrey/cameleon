(ns cameleon.uri-test
  (:require #?(:cljs [cljs.test    :as t
                      :refer-macros [is are deftest testing]]
               :clj  [clojure.test :as t
                      :refer        [is are deftest testing]])
            [cameleon.uri :as uri]
            ))

(deftest parse-test
  (is (= (uri/parse "")
         {:uri "",
          :parsed? true}))
  (is (= (uri/parse "whatever")
         {:uri "whatever",
          :pathname "whatever",
          :path "whatever",
          :parsed? true}))
  (is (= (uri/parse "whatever:")
         {:uri "whatever:",
          :scheme "whatever",
          :parsed? true}))
  (is (= (uri/parse "whatever:/")
         {:uri "whatever:/",
          :scheme "whatever",
          :pathname "/",
          :path "/",
          :parsed? true}))
  (is (= (uri/parse "https://example.com:80/path/to/resource")
         {:uri "https://example.com:80/path/to/resource",
          :scheme "https",
          :protocol "https:",
          :hostname "example.com",
          :port "80",
          :host "example.com:80",
          :pathname "/path/to/resource",
          :path "/path/to/resource",
          :parsed? true,
          }))
  (is (= (uri/parse "https://example.com:80/path/to/resource?param=value")
         {:uri "https://example.com:80/path/to/resource?param=value",
          :scheme "https",
          :protocol "https:",
          :hostname "example.com",
          :port "80",
          :host "example.com:80",
          :pathname "/path/to/resource",
          :query "param=value",
          :path "/path/to/resource?param=value",
          :parsed? true,
          }))
  (is (= (uri/parse "https://john:doe@example.com:80/path/to/resource?param=value")
         {:uri "https://john:doe@example.com:80/path/to/resource?param=value",
          :scheme "https",
          :protocol "https:",
          :hostname "example.com",
          :port "80",
          :host "example.com:80",
          :pathname "/path/to/resource",
          :query "param=value",
          :path "/path/to/resource?param=value",
          :user "john",
          :password "doe",
          :parsed? true,
          }))
  (is (= (uri/parse "https://john:doe@example.com:80/path/to/resource?param=value#place")
         {:uri "https://john:doe@example.com:80/path/to/resource?param=value#place",
          :scheme "https",
          :protocol "https:",
          :hostname "example.com",
          :port "80",
          :host "example.com:80",
          :pathname "/path/to/resource",
          :query "param=value",
          :path "/path/to/resource?param=value",
          :user "john",
          :password "doe",
          :fragment "place",
          :parsed? true,
          }))
  (is (= (uri/parse "git@github.com:fmjrey/cameleon.git")
         {:uri "git@github.com:fmjrey/cameleon.git",
          :scheme "git",
          :protocol "git:",
          :hostname "github.com",
          :host "github.com",
          :pathname "fmjrey/cameleon.git",
          :path "fmjrey/cameleon.git",
          :parsed? true,
          }))
  (is (= (uri/parse "ssh@github.com:fmjrey/cameleon.git")
         {:uri "ssh@github.com:fmjrey/cameleon.git",
          :scheme "ssh",
          :protocol "ssh:",
          :hostname "github.com",
          :host "github.com",
          :pathname "fmjrey/cameleon.git",
          :path "fmjrey/cameleon.git",
          :parsed? true,
          }))
  )
