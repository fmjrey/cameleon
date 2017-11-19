(ns cameleon.http
  (:require [mount.core :refer [defstate]]
            [cameleon.utils :as utils]
            [httpurr.client :as http]
    #?(:clj [httpurr.client.aleph])
   #?(:cljs [httpurr.client.xhr :as xhr])
   #?(:cljs [httpurr.client.node :as node])
            [promesa.core :as p]
            [cameleon.io :as io]
    ))

;; Needed to use mount in cljs
;; see https://github.com/tolitius/mount/blob/master/doc/clojurescript.md
#?(:clj (mount.core/in-cljc-mode))

#?(:clj (defstate jvm-client
          :start httpurr.client.aleph/client))

#?(:cljs (defstate browser-client
           :start xhr/client))

#?(:cljs (defstate nodejs-client
           :start node/client))

(def clients
  #?(:clj {:jvm @jvm-client}
     :cljs {:nodejs @nodejs-client
            :browser @browser-client}))

(defstate client
  :start (get clients (utils/platform)))

(defn default-client
  "Return the default http client (depends on the target/runtime platform)."
  []
  @client)

(defn body-to-string
  "Return the response map with its body converted to string."
  [response]
  ;; NOTE: using update-in to convert body does not always work, as if it's
  ;; reading the input stream before anything else can
  (let [s (-> response :body io/to-string)]
    (assoc response :body s)))

(defn async-send
  ([req]
   (async-send (default-client) req))
  ([http-client req]
   (http/send! http-client req))
  ([method url headers body]
   (async-send (default-client) method url headers body))
  ([http-client method url headers body]
   (async-send http-client
               {:method method
                :url url
                :body body
                :headers (into {} headers)})))
