(ns cameleon.uri-matcher
  (:require [clojure.string :as str]
            [cameleon.uri :as uri]
            [sibiro.core :as sb]))

(def route-methods #{:get :post :put :delete :patch :any})

(defn- compile-routes
  [routes]
  (->> routes
       (mapv (fn [[first :as route]]
               (let [method-provided? (route-methods first)]
                 (if method-provided?
                   route
                   (->> route seq (cons :any) vec)))))
        sb/compile-routes))

(defn- compile-patterns-map
  [{:keys [hosts routes] :as patterns-map}]
  (cond-> patterns-map
    (empty? hosts) (dissoc :hosts)
    (empty? routes) (dissoc :routes)
    (and (empty? hosts) (empty? routes)) (do nil)
    (not (empty? routes)) (update-in [:routes] compile-routes)))

(defn compile-uris
  "Compile the given list of uris into a form that can be used by match-uri.
  Takes a list of vector pairs, the first pair item is the main handler object
  and the second is a map containing patterns to be matched against a URI.
  The inner patterns map can contain two optional entries:
  - :hosts which must point to a vector of strings or regex literals, to
    match to an exact hostname or to a set of hosts matching a regex,
  - :routes which must be a list of routes as per functionalbytes/sibiro,
    except the method is optional and defaults to :any if not provided.
  No :hosts entry matches any host, and no :routes entry matches any path.
  Example:
  [[:github {:hosts [\"github.com\"]
             :routes [[:get \"/:owner/:name\"                 :repo]
                      [:any \"/:owner/:name/blob/:branch/:*\" :file]
                      [:get \"/:owner/:name/tree/:branch/:*\" :dir]
                      [:any \"/:*\"                           :unknown]]}]
   ;; http verb is optional
   [:github-raw {:hosts [\"raw.githubusercontent.com\" \"raw.github.com\"]
                 :routes [[\"/:owner/:name/:branch/:*\"  :file]]}]
   ;; order matters: other github subdomains matched with a regex
   [:github-subdomain {:hosts [\"(?:.+\\.)?github.com\"]
                       :routes [[\"/:*\" :any-path]]}]
   ;; hosts with no routes => matching on host only, any path matches
   [:github-pages {:hosts [\"pages.github.com\"]}]
   ;; routes with no hosts => matching on path only, any host matches
   [:login {:routes [[\"/login\" :login-handler]]}]
  ])"
  [uris]
  (when-not (empty? uris)
    (mapv (fn [[main-handler patterns-map]]
                  [main-handler (compile-patterns-map patterns-map)])
          uris)))

(defn match-hostname
  "Matches a given hostname against a vector of hosts strings or regexes.
  Returns the matching string or re-matches result if a match is found.
  Returns true if the list of string or host-regexes is nil or emtpy.
  Returns nil if no match is found.
  "
  [host-regexes hostname]
  (cond
    (and (nil? hostname) (not (empty? host-regexes))) nil
    (empty? host-regexes) true
    :else (some (fn [string-or-regex]
                  (if (string? string-or-regex)
                    (when (= hostname string-or-regex) string-or-regex)
                    (re-matches string-or-regex hostname)))
                host-regexes)))

(defn match-pathname
  "Matches a given pathname and method against a compiled list of routes
  (as compiled by functionalbytes/sibiro).
  An optional method argument must be an http verb keyword (:get, :post, :put,
  :delete, or :patch), which defaults to :get when not provided or set to nil.
  A successful match returns a map which is identical to what match-uri from
  sibirofunctionalbytes/sibiro returns. If routes is nil it returns {}, as
  no routes matches any path.
  "
  ([routes pathname]
   (match-pathname routes pathname :get))
  ([routes pathname method]
   (cond
     (nil? routes) {} ;; no routes matches any path
     pathname (sb/match-uri routes pathname (or method :get))
     :else nil)))

(defn match-uri
  "Matches a given uri with a list of compiled uris (see compile-uris).
  An optional method argument must be an http verb keyword (:get, :post, :put,
  :delete, or :patch), which defaults to :get when not provided or set to nil.
  A succesful match returns a map which is identical to what match-uri from
  sibirofunctionalbytes/sibiro returns, augmented with:
  - a :parsed-uri entry containing the result from cameleon.uri/parse on the uri
  - a :main-handler entry containing the handler object of the matching entry
  - a :alt entry containins a lazy list of alternative matches, similar to what
    :alternatives contains, except the latter is returned by sibiro and contains
    only alternative matching routes within same compiled uri entry.
  In case of a match against an entry with no :routes, the map only
  contains the :parsed-uri and :main-handler entries.
  Returns nil if no match is found."
  ([compiled-uris uri]
   (match-uri compiled-uris uri :get))
  ([compiled-uris uri method]
   (let [{:keys [scheme hostname pathname] :as parsed-uri} (uri/parse uri)
         method (if method method :get)
         matching-hosts (fn [[_ {host-regexes :hosts}]]
                          (match-hostname host-regexes hostname))
         matching-routes (fn [[main-handler {routes :routes}]]
                           (when-let [matched-path (match-pathname routes
                                                                   pathname
                                                                   method)]
                             (-> matched-path
                                 (assoc :main-handler main-handler)
                                 (assoc :parsed-uri parsed-uri)
                                 (assoc :method method))))
         [first-match & alt] (some->> compiled-uris
                                      (filter matching-hosts)
                                      (keep matching-routes))]
     (when first-match
       (assoc first-match :alt (or alt ()))))))
