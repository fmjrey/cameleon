(ns cameleon.utils
  (:require [clojure.string :as string]))

(defn platform
  "Runtime detection of the host platform.
  Returns either :jvm, :browser, or :nodejs"
  []
  #?(:clj :jvm
     :cljs (if (= cljs.core/*target* "nodejs")
             :nodejs
             :browser)))

(defn update-frequency
  "Return a new frequency map (as in clojure.core/frequencies)
  where the frequency for k is incremented by 1 or a given number.
  Examples:
  (update-frequency {:a 1, :b 1, :c 1} :b)
  => {:a 1, :b 2, :c 1}
  (update-frequency {:a 1, :b 1, :c 1} :d 2)
  => {:a 1, :b 2, :c 1, :d 2}
  "
  ([m k] (update-frequency m k 1))
  ([m k n]
   (if (contains? m k)
     (update-in m [k] + n)
     (assoc m k n))))

(defn ->map
  "Convert any object into a map.
  Returns nil when argument is nil.
  Returns argument if it's already a map.
  Inspired by https://stackoverflow.com/questions/32467299/clojurescript-convert-arbitrary-javascript-object-to-clojure-script-map"
  [o]
  (cond
    (nil? o) nil
    (map? o) o
    :else
    #?(:cljs (-> (fn [result key]
                   (let [v (aget o key)]
                     (if (= "function" (goog/typeOf v))
                       result
                       (assoc result key v))))
                 (reduce {} (.getKeys goog/object o)))
       :clj (bean o))))

(defn- ->map-with-message
  "Same as ->map except it adds a :message entry, if not already present,
  with the argument converted to string."
  [e]
  (let [m (->map e)]
    (if (contains? :message m)
      m
      (assoc m :message (str e)))))

(defn error->map
  "Convert an error or exception into a map.
  Returns nil when argument is nil.
  Returns argument if it's already a map.
  Return a map with at least a :message entry otherwise."
  [e]
  (cond
    (nil? e) nil
    (map? e) e
    (string? e) {:message e}
    #?@(:clj [(instance? Throwable e) (Throwable->map e)])
    :else (-> (or (ex-data e) {})
              (into #?(:clj (->map e)
                       :cljs (if (instance? js/Error e)
                               {:type (type e)
                                :message (.-message e)
                                :cause (.-cause e)}
                               (->map e))))
              ->map-with-message)))
