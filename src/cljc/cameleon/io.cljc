(ns cameleon.io
  (:require
   #?@(:clj  [[clojure.edn :as edn]
              [clojure.java.io :as io]
              [byte-streams :as bytes]]
       :cljs [[cljs.reader :as reader]
              [cljs-node-io.core :as io :refer [slurp spit]]
              [cljs-node-io.file :refer [File]]])
       [fipp.edn]
   )
  #?(:clj (:import [java.io File]))
  )

#?(:cljs (def os (js/require "os")))
#?(:cljs (def path (js/require "path")))

(def sep
  "Separator character for paths."
  #?(:cljs (.-sep path)
     :clj (java.io.File/separator)))

(defn tmpdir
  "The temporary file directory. Does not create a temporary directory."
  []
  #?(:clj (System/getProperty "java.io.tmpdir")
     :cljs (.tmpdir os)))

(defn to-file
  "Return a file object, given its name and an optional parent directory.
  String and file objects are supported for both arguments."
  ([file]
   #?(:clj (if (instance? java.io.File file)
             file
             (File. file))
      :cljs (io/file file)))
  ([dir file]
   (-> dir
       to-file
       (#?(:clj File. :cljs io/file) file))))

(defn exists?
  "True if given file/dir exists, false or nil otherwise."
  [file]
  (some-> file
          to-file
          .exists))

(defn dir?
  "True if given file exists and is a directory, false or nil otherwise."
  [file]
  (let [d (to-file file)]
    (and (exists? d) (.isDirectory d))))

(defn write-to
  "Write the given content to a file.
  File and dir can be anything understood by to-file.
  Content is anything that clojure.core/spit or cljs-node-io.core/spit
  can handle. Typically, a string is written as-is, a clojure data structure
  is written in EDN format without pretty formatting.
  See also write-edn-to to output data structures into a pretty EDN format."
  ([file content]
   (spit (to-file file) content))
  ([dir file content]
   (spit (to-file dir file) content)))

(defn read-from
  "Return the content from a file as a string.
  File and dir can be anything understood by to-file."
  ([file]
   (slurp (to-file file)))
  ([dir file]
   (slurp (to-file dir file))))

(defn to-string
  "Return a string from the given string or byte stream."
  [s]
  (#?(:cljs str
      :clj bytes/to-string) s))

(defn read-edn
  "Read clojure data from the given EDN string."
  [content]
  (#?(:clj edn/read-string
      :cljs reader/read-string) content))

(defn read-edn-from
  "Read clojure data from the given file.
  File and dir can be anything understood by to-file."
  ([file]
   (-> (read-from file)
       read-edn
       ))
  ([dir file]
   (-> (read-from dir file)
       read-edn
       )))

(defn pretty-edn
  "Return a pretty printed EDN string from clojure data."
  [data]
  (with-out-str (fipp.edn/pprint data)))

(defn write-edn-to
  "Write the given data to a file in a pretty format.
  File and dir can be anything understood by to-file.
  See also write-to which can output EDN without prettying."
  ([file data]
   (write-to file (pretty-edn data)))
  ([dir file data]
   (write-to dir file (pretty-edn data))))
