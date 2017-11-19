(defproject cameleon "0.1.0-SNAPSHOT"
  :description "Clojure(Script) utility library for file and HTTP io, HTTP mocking, and URI matching."
  :url "https://github.com/fmjrey/cameleon"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 ;[org.clojure/core.async "0.3.443"]
                 ;; Clojure only dependencies
                 [dk.ative/docjure "1.11.0"]
                 [aleph "0.4.4"]
                 [cheshire "5.8.0"]
                 ;; Clojure(Script) dependencies
                 [mount "0.1.11"]
                 [funcool/promesa "1.9.0"]
                 [funcool/httpurr "1.0.0"]
                 [functionalbytes/sibiro "0.1.5"]
                 [lambdaisland/uri "1.1.0"]
                 [fipp "0.6.12" :exclusions [org.clojure/core.rrb-vector]]
                 [quantum/org.clojure.core.rrb-vector "0.0.12"] ;; see https://github.com/brandonbloom/fipp/issues/42
                 ;; ClojureScript only dependencies
                 [org.clojure/clojurescript "1.9.946"]
                 [cljsjs/nodejs-externs "1.0.4-1"]
                 [cljs-node-io "0.5.0"]
                 ]
  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :test-paths ["test/clj" "test/cljc" "test/cljs"]
  :profiles {:dev {:dependencies [[lein-doo "0.1.8"]]
                   :resource-paths ["test/fixtures"]
                   }}
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.8"]
            [lein-npm "0.6.2"]]
  :cljsbuild {:test-commands {"test" ["lein" "cljstest"]}
              :builds {
              :dev {
                :source-paths ["src/cljs" "src/cljc"]
                :compiler {:main cameleon.core
                           :output-to "cljsbuild/main.js"
                           :output-dir "cljsbuild/target/dev" ; For temporary files
                           :optimizations :none
                           :source-map true
                           :parallel-build true
                           :pretty-print true}}
              :test {
                :source-paths ["src/cljs" "src/cljc" "test/cljc" "test/cljs"]
                :compiler {:main cameleon.test-runner
                           :output-to "cljsbuild/test.js"
                           :output-dir "cljsbuild/target/test" ; For temporary files
                           :optimizations :simple
                           :target :nodejs
                           :source-map "cljsbuild/test.js.map"
                           :parallel-build true
                           :pretty-print true}}
                    }
              }
  :npm {:devDependencies [["source-map-support" "*"]]
       }
  :aliases {"cljstest" ["do"
                        ["npm" "install"]
                        ["doo" "node" "test" "once"]]
            })
