(ns cameleon.uri-matcher-test
  (:require #?(:cljs [cljs.test    :as t
                      :refer-macros [is are deftest testing]]
               :clj  [clojure.test :as t
                      :refer        [is are deftest testing]])
            [clojure.data :as data]
            [cameleon.uri-matcher :as urimat]
            [sibiro.core :as sb]))

(deftest match-hostname-test
  (are [host-regexes hostname result]
      (= result (urimat/match-hostname host-regexes hostname))
    nil "github.com" true
    [] "github.com" true
    ["github.com"] nil nil
    ["github.com"] "github.com" "github.com"
    [#"(?:.+\.)?github.com"] "github.com" "github.com"
    [#"(?:.+\.)?github.com"] nil nil
    ["github.com" #"(?:.+\.)?github.com"] "github.com" "github.com"
    ["whatever.com" #"(?:.+\.)?github.com"] "github.com" "github.com"
    ["github.com" #"(?:.+\.)?whatever.com"] "github.com" "github.com"
    ["github.com" #"(?:.+\.)?github.com"] "whatever.com" nil
    ["github.com" #"(?:.+\.)?github.com"] "whatevergithub.com" nil
    ["github.com" "whatevergithub.com"] "whatevergithub.com" "whatevergithub.com"
    ))

(deftest match-pathname-test
  (is (= {} (urimat/match-pathname nil "/whatever")))
  (is (= {} (urimat/match-pathname nil "/whatever" :get)))
  (is (= {} (urimat/match-pathname nil "whatever" :post)))
  (is (= {} (urimat/match-pathname nil "" :post)))
  (is (= {} (urimat/match-pathname nil nil :post)))
  (is (= {} (urimat/match-pathname nil nil)))
  (is (= {} (urimat/match-pathname nil nil nil)))
  (let [routes (sb/compile-routes
                [[:get "/:owner/:name"                 :repo]
                 [:any "/:owner/:name/blob/:branch/:*" :file]
                 [:get "/:owner/:name/tree/:branch/:*" :dir]
                 [:any "/:*"                           :unknown]])]
    (are [pathname method result]
        (= result (urimat/match-pathname routes pathname method))
      "/fmjrey/cameleon" :get {:route-handler :repo,
                           :route-params {:owner "fmjrey", :name "cameleon"},
                           :alternatives '({:route-handler :unknown,
                                            :route-params {:* "fmjrey/cameleon"}})}
      "/fmjrey/cameleon" :put {:route-handler :unknown,
                           :route-params {:* "fmjrey/cameleon"},
                           :alternatives ()}
      "fmjrey/cameleon"  :put nil
      )))

(def uris
  [[:github {:hosts ["github.com"]
             :routes [[:get "/:owner/:name"                 :repo]
                      [:any "/:owner/:name/blob/:branch/:*" :file]
                      [:get "/:owner/:name/tree/:branch/:*" :dir]
                      [:any "/:*"                           :unknown]]}]
   ;; http verb is optional
   [:github-raw {:hosts ["raw.githubusercontent.com" "raw.github.com"]
                 :routes [["/:owner/:name/:branch/:*"  :file]]}]
   ;; order matters: other github subdomains matched with a regex
   [:github-subdomain {:hosts [#"(?:.+\.)?github.com"]
                       :routes [["/:*" :any-path]]}]
   ;; hosts with no routes => matching on host only, any path matches
   [:github-pages {:hosts ["pages.github.com"]}]
   ;; routes with no hosts => matching on path only, any host matches
   [:login {:routes [["/login" :login-handler]]}]
   ])

(deftest compile-uris-test
  (is (= nil (urimat/compile-uris nil)))
  (is (= nil (urimat/compile-uris [])))
  (is (= [[nil {:routes [[:get "/:owner/:name" :repo]
                         [:any "/:owner/:name/blob/:branch/:*" :file]
                         [:get "/:owner/:name/tree/:branch/:*" :dir]
                         [:any "/:*" :unknown]]}]
          [nil {:routes [["/:owner/:name/:branch/:*" :file]]}]
          [nil {:routes [["/:*" :any-path]]}]
          nil
          [nil {:routes [["/login" :login-handler]]}]]
         (first (data/diff uris (urimat/compile-uris uris))))))

(deftest match-uri-test
  (is (= nil (urimat/match-uri nil nil nil)))
  (is (= nil (urimat/match-uri nil "whatever" :get)))
  (is (= nil (urimat/match-uri nil "whatever" nil)))
  (is (= nil (urimat/match-uri nil nil :get)))
  (is (= nil (urimat/match-uri [] nil nil)))
  (is (= nil (urimat/match-uri [] "whatever" :get)))
  (is (= nil (urimat/match-uri [] "whatever" nil)))
  (is (= nil (urimat/match-uri [] nil :get)))
  (let [uris (urimat/compile-uris uris)]
    (are [method uri result] (= result (urimat/match-uri uris uri method))
      nil nil nil
      nil "whatever" nil
      :get "whatever" nil
      :get "github.com" nil
      :get "/github.com" nil
      :get "github.com/" nil
      :get "github.com:" nil
      :get "github.com:/" nil
      :get "whatever.github.com/path" nil
      :get "https://whatever.github.com/path"
           {:parsed-uri {:uri "https://whatever.github.com/path",
                         :scheme "https",
                         :protocol "https:",
                         :hostname "whatever.github.com",
                         :host "whatever.github.com",
                         :pathname "/path",
                         :path "/path",
                         :parsed? true},
            :method :get,
            :main-handler :github-subdomain,
            :route-handler :any-path,
            :route-params {:* "path"},
            :alternatives (),
            :alt ()}
      :get "https://.github.com/path" nil
      :get "https://github.com/path"
           {:parsed-uri {:uri "https://github.com/path",
                         :scheme "https",
                         :protocol "https:",
                         :hostname "github.com",
                         :host "github.com",
                         :pathname "/path",
                         :path "/path",
                         :parsed? true},
            :method :get,
            :main-handler :github,
            :route-handler :unknown,
            :route-params {:* "path"},
            :alternatives (),
            :alt '({:parsed-uri {:uri "https://github.com/path",
                                 :scheme "https",
                                 :protocol "https:",
                                 :hostname "github.com",
                                 :host "github.com",
                                 :pathname "/path",
                                 :path "/path",
                                 :parsed? true},
                    :method :get,
                    :main-handler :github-subdomain,
                    :route-handler :any-path,
                    :route-params {:* "path"},
                    :alternatives ()})}
      :get "https://raw.github.com/path"
           {:parsed-uri {:uri "https://raw.github.com/path",
                         :scheme "https",
                         :protocol "https:",
                         :hostname "raw.github.com",
                         :host "raw.github.com",
                         :pathname "/path",
                         :path "/path",
                         :parsed? true},
            :method :get,
            :main-handler :github-subdomain,
            :route-handler :any-path,
            :route-params {:* "path"},
            :alternatives (),
            :alt ()}
      :get "https://raw.github.com/login"
           {:parsed-uri {:uri "https://raw.github.com/login",
                         :scheme "https",
                         :protocol "https:",
                         :hostname "raw.github.com",
                         :host "raw.github.com",
                         :pathname "/login",
                         :path "/login",
                         :parsed? true},
            :method :get,
            :main-handler :github-subdomain,
            :route-handler :any-path,
            :route-params {:* "login"},
            :alternatives (),
            :alt '({:parsed-uri {:uri "https://raw.github.com/login",
                                 :scheme "https",
                                 :protocol "https:",
                                 :hostname "raw.github.com",
                                 :host "raw.github.com",
                                 :pathname "/login",
                                 :path "/login",
                                 :parsed? true},
                    :method :get,
                    :main-handler :login,
                    :route-handler :login-handler,
                    :route-params {},
                    :alternatives ()})}
      :get "https://pages.github.com/login"
           {:parsed-uri {:uri "https://pages.github.com/login",
                         :scheme "https",
                         :protocol "https:",
                         :hostname "pages.github.com",
                         :host "pages.github.com",
                         :pathname "/login",
                         :path "/login",
                         :parsed? true},
            :method :get,
            :main-handler :github-subdomain,
            :route-handler :any-path,
            :route-params {:* "login"},
            :alternatives (),
            :alt '({:parsed-uri {:uri "https://pages.github.com/login",
                                 :scheme "https",
                                 :protocol "https:",
                                 :hostname "pages.github.com",
                                 :host "pages.github.com",
                                 :pathname "/login",
                                 :path "/login",
                                 :parsed? true},
                    :method :get,
                    :main-handler :github-pages}
                   {:parsed-uri {:uri "https://pages.github.com/login",
                                 :scheme "https",
                                 :protocol "https:",
                                 :hostname "pages.github.com",
                                 :host "pages.github.com",
                                 :pathname "/login",
                                 :path "/login",
                                 :parsed? true},
                    :method :get,
                    :main-handler :login,
                    :route-handler :login-handler,
                    :route-params {},
                    :alternatives ()})}
      :get "https://github.com/fmjrey/cameleon"
           {:parsed-uri {:uri "https://github.com/fmjrey/cameleon",
                         :scheme "https",
                         :protocol "https:",
                         :hostname "github.com",
                         :host "github.com",
                         :pathname "/fmjrey/cameleon",
                         :path "/fmjrey/cameleon",
                         :parsed? true},
            :method :get,
            :main-handler :github,
            :route-handler :repo,
            :route-params {:owner "fmjrey",
                           :name "cameleon"},
            :alternatives '({:route-handler :unknown,
                             :route-params {:* "fmjrey/cameleon"}}),
            :alt '({:parsed-uri {:uri "https://github.com/fmjrey/cameleon",
                                 :scheme "https",
                                 :protocol "https:",
                                 :hostname "github.com",
                                 :host "github.com",
                                 :pathname "/fmjrey/cameleon",
                                 :path "/fmjrey/cameleon",
                                 :parsed? true},
                    :method :get,
                    :main-handler :github-subdomain,
                    :route-handler :any-path,
                    :route-params {:* "fmjrey/cameleon"},
                    :alternatives ()})}
      :put "https://github.com/fmjrey/cameleon"
           {:parsed-uri {:uri "https://github.com/fmjrey/cameleon",
                         :scheme "https",
                         :protocol "https:",
                         :hostname "github.com",
                         :host "github.com",
                         :pathname "/fmjrey/cameleon",
                         :path "/fmjrey/cameleon",
                         :parsed? true},
            :method :put,
            :main-handler :github,
            :route-handler :unknown,
            :route-params {:* "fmjrey/cameleon"},
            :alternatives (),
            :alt '({:parsed-uri {:uri "https://github.com/fmjrey/cameleon",
                                 :scheme "https",
                                 :protocol "https:",
                                 :hostname "github.com",
                                 :host "github.com",
                                 :pathname "/fmjrey/cameleon",
                                 :path "/fmjrey/cameleon",
                                 :parsed? true},
                    :method :put,
                    :main-handler :github-subdomain,
                    :route-handler :any-path,
                    :route-params {:* "fmjrey/cameleon"},
                    :alternatives ()})}
      :put "https://github.com/fmjrey/cameleon/blob/master/project.clj"
           {:parsed-uri {:uri "https://github.com/fmjrey/cameleon/blob/master/project.clj",
                         :scheme "https",
                         :protocol "https:",
                         :hostname "github.com",
                         :host "github.com",
                         :pathname "/fmjrey/cameleon/blob/master/project.clj",
                         :path "/fmjrey/cameleon/blob/master/project.clj",
                         :parsed? true},
            :method :put,
            :main-handler :github,
            :route-handler :file,
            :route-params {:owner "fmjrey",
                           :name "cameleon",
                           :branch "master",
                           :* "project.clj"},
            :alternatives '({:route-handler :unknown,
                             :route-params {:* "fmjrey/cameleon/blob/master/project.clj"}}),
            :alt '({:parsed-uri {:uri "https://github.com/fmjrey/cameleon/blob/master/project.clj",
                                 :scheme "https",
                                 :protocol "https:",
                                 :hostname "github.com",
                                 :host "github.com",
                                 :pathname "/fmjrey/cameleon/blob/master/project.clj",
                                 :path "/fmjrey/cameleon/blob/master/project.clj",
                                 :parsed? true},
                    :method :put,
                    :main-handler :github-subdomain,
                    :route-handler :any-path,
                    :route-params {:* "fmjrey/cameleon/blob/master/project.clj"},
                    :alternatives ()})}
           )))
