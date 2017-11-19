(ns cameleon.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [cameleon.utils-test]
            [cameleon.io-test]
            [cameleon.uri-test]
            [cameleon.uri-matcher-test]
            [cameleon.http-client-mock-test]
            [cameleon.http-test]
            [cameleon.rest-test]
            [cameleon.repo-test]
            [cljs.nodejs :as nodejs]))

(enable-console-print!)
(.install (js/require "source-map-support"))
(nodejs/enable-util-print!)

(doo-tests
           'cameleon.utils-test
           'cameleon.io-test
           'cameleon.uri-test
           'cameleon.uri-matcher-test
           'cameleon.http-client-mock-test
           'cameleon.http-test
           'cameleon.rest-test
           'cameleon.repo-test
           )
