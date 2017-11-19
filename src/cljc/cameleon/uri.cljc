(ns cameleon.uri
  (:require [clojure.string :as str]
            [lambdaisland.uri :as uri]
            ))

(defn url-string?
  "True if the argument is a string that starts with \"http\"."
  [s]
  (and (string? s) (-> s str/trim (str/starts-with? "http"))))

(defn parse
  "Parse a URL/URI into a map in the form:
  {:uri \"http://whatever.com:80/path?p=v\", ; the given url
   :protocol \"http:\",
   :scheme \"http\",
   :hostname \"whatever.com\",
   :host \"whatever.com:80\",
   :pathname \"/path\",
   :path \"/path?p=v\",
   :query \"p=v\",
   :parsed? true ; always added
  }
  It also recognise git and ssh connection strings in the format
  ssh@example.com:path/to/something
  "
  [url]
  (let [{:keys [scheme host port path query] :as parsed-uri} (uri/uri url)]
    (cond-> (into {} (filter second parsed-uri))
      true (-> (assoc :uri url)
               (assoc :parsed? true))
      host (assoc :hostname host)
      path (assoc :pathname path)
      (and (nil? host) scheme) (cond->
                                 (str/starts-with? scheme "ssh@")
                                 (-> (assoc :scheme "ssh")
                                     (assoc :protocol "ssh:")
                                     (assoc :hostname (subs scheme 4))
                                     (assoc :host (subs scheme 4)))
                                 (str/starts-with? scheme "git@")
                                 (-> (assoc :scheme "git")
                                     (assoc :protocol "git:")
                                     (assoc :hostname (subs scheme 4))
                                     (assoc :host (subs scheme 4))))
      (and host scheme) (assoc :protocol (str scheme \:))
      port (assoc :host (str host \: port))
      query (assoc :path (str path \? query))
      )))

