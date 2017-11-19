(ns cameleon.repo
  (:require [clojure.string :as str]
            [cameleon.io :as io]
            [cameleon.uri-matcher :as urimat]
            [sibiro.core :as sb]))

(def uris
  (urimat/compile-uris
   [[:github {:hosts ["github.com"]
              :routes [[":owner/:name"                  :git-repo]
                       ["/:owner/:name"                 :git-repo]
                       ["/:owner/:name/blob/:branch/:*" :file]
                       ["/:owner/:name/tree/:branch/:*" :dir]
                       ["/:*"                           :unknown]]}]
    [:github {:hosts ["raw.githubusercontent.com"
                      "raw.github.com"
                      "rawgithub.com"
                      #"(?:cdn\.)?rawgit.com"]
              :routes [["/:owner/:name/:branch/:*"  :file]
                       ["/:*"                       :unknown]]}]
    [:remote {:hosts [#".+"]
              :routes [[":*" :unknown]]}]
    [:unknown {:routes [[":*" :unknown]]}]
    ]))

(defn- add-origin-and-resource-type
  [parsed-uri {:keys [main-handler route-handler]}]
  (-> parsed-uri
      (assoc :origin main-handler)
      (assoc :resource-type route-handler)))

(defn remove-from-end [s end]
  (if (str/ends-with? s end)
    (subs s 0 (- (count s) (count end)))
    s))

(defn- add-owner-name
  [parsed-uri {{:keys [owner name]} :route-params}]
  (-> parsed-uri
      (assoc :repo-owner owner)
      (assoc :repo-name (remove-from-end name ".git"))))

(defn- add-branch-and-path
  [parsed-uri {{:keys [branch *]} :route-params}]
  (-> parsed-uri
      (assoc :repo-path *)
      (assoc :repo-branch branch)))

(defmulti parse-path
  (fn [parsed-uri matched-uri]
    [(:main-handler matched-uri) (:route-handler matched-uri)]))

(defmethod parse-path [:github :git-repo]
  [parsed-uri matched-uri]
  (-> parsed-uri
      (add-origin-and-resource-type matched-uri)
      (add-owner-name matched-uri)))

(defn- parse-repo-file-or-dir
  [parsed-uri matched-uri]
  (-> parsed-uri
      (add-origin-and-resource-type matched-uri)
      (add-owner-name matched-uri)
      (add-branch-and-path matched-uri)))

(defmethod parse-path [:github :dir]
  [parsed-uri matched-uri]
  (parse-repo-file-or-dir parsed-uri matched-uri))

(defmethod parse-path [:github :file]
  [parsed-uri matched-uri]
  (parse-repo-file-or-dir parsed-uri matched-uri))

(defmethod parse-path [:github :unknown]
  [parsed-uri matched-uri]
  (add-origin-and-resource-type parsed-uri matched-uri))

(defmethod parse-path [:remote :unknown]
  [parsed-uri matched-uri]
  (add-origin-and-resource-type parsed-uri matched-uri))

(defmethod parse-path [:unknown :unknown]
  [parsed-uri matched-uri]
  (add-origin-and-resource-type parsed-uri matched-uri))

(defmethod parse-path :default
  [parsed-uri matched-uri]
  (assoc parsed-uri :error "Unrecognised host and path"))

(defn parse-local
  [parsed-uri]
  (let [filepath (:pathname parsed-uri)
        file (when-not (str/blank? filepath) (io/to-file filepath))
        exists? (io/exists? file)
        dir? (io/dir? file)
        contains-git-dir? (when dir? (io/dir? (io/to-file file ".git")))]
    (cond-> parsed-uri
      (not exists?) (-> (assoc :origin :unknown)
                        (assoc :resource-type :unknown)
                        (dissoc :pathname)
                        (dissoc :path))
      exists? (assoc :origin :local)
      (and exists? (not dir?)) (assoc :resource-type :file)
      dir? (cond->
             contains-git-dir?
               (-> (assoc :resource-type :git-repo)
                   (assoc :repo-name (remove-from-end (.getName file) ".git")))
             (not contains-git-dir?)
               (assoc :resource-type :dir)))))

(def local-schemes #{"file"})
(def remote-schemes #{"http" "https" "ftp" "ftps" "ssh" "git" "scp" "sftp"})

(defn parse-uri
  "Extract repo information from a URI (local file or URL).
  Returns a map as returned by cameleon.uri/parse, augmented with:
  {:origin _        ; either :local, :remote, :unknown, or a key in host-regexes
   :resource-type _ ; either :git-repo, :file, :dir, or :unknown
   ;; below keys are optionally added depending on what's recognised
   :repo-owner _    ; owner of the repository
   :repo-name _     ; name of the repository
   :repo-branch _   ; branch/tag/commit
   :repo-path _     ; path to file or dir in repository
   }"
  [uri]
  (let [{{:keys [scheme hostname pathname] :as parsed-uri} :parsed-uri
         :as matched-uri}
        (urimat/match-uri uris uri)]
    (cond
      (nil? matched-uri) {:uri uri
                           :origin :unknown
                           :resource-type :unknown
                           :parsed? true}
      ;; local
      (or (nil? scheme) (local-schemes scheme))
      (parse-local parsed-uri)
      ;; remote
      (remote-schemes scheme)
      (parse-path parsed-uri matched-uri)
      ;; unknown protocol
      :else (-> parsed-uri
                (assoc :origin :unknown)
                (assoc :resource-type :unknown))
      )))
