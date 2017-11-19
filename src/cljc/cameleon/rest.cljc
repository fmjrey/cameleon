(ns cameleon.rest
  (:require [cameleon.http :as http]
            [promesa.core :as p]
  #?@(:clj  [[cheshire.core :as json]
             [byte-streams :as bytes]])
            ))

(defn parse-json [d]
  #?(:cljs (js->clj (js/JSON.parse d) :keywordize-keys true)
     :clj  (json/parse-string (bytes/to-string d) true)))

(defn to-json [d]
  #?(:cljs (js/JSON.stringify (clj->js d))
     :clj  (json/generate-string d)))

(defn json-decode
  [response]
  (update response :body parse-json))

(defn json-encode
  [request]
  (update request :body to-json))

(def json-header {"Content-Type" "application/json"})

(defn async-send
  ([method url body]
   (p/then (http/async-send method url json-header (to-json body))
           json-decode))
  ([http-client method url body]
   (p/then (http/async-send http-client method url json-header (to-json body))
           json-decode)))
