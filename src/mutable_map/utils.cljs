(ns mutable-map.utils
  ""
  (:use 
    [mutable-map.protocols :only [IMutableKVMapWatchable notify-kvmap-watches add-kvmap-watch remove-kvmap-watch IMutableKVMap maybe-keys empty! update! update!* pr-edn-str read-edn-string]])
  (:require [cljs.reader :as reader]))

;; missing core fns :-(

(defn update** 
  ""
  [m k f & args]
  (let [no-value (atom {})
        v (get m k no-value)]
    (if (= v no-value)
      m
      (assoc m k (apply f v args)))))

;; (defn dissoc-in
;;   "Removes an entry in a nested associative structure.
;;    (= (dissoc-in {:a {:b 1 :c 2} :d {:e 3 :f 4}} [:a :c])
;;        {:a {:b 1} :d {:e 3 :f 4}})"
;;   ([m keys]
;;     (if (= 1 (count keys))
;;       (dissoc m (first keys))
;;       (let [ks (butlast keys)
;;             k (last keys)]
;;         (assoc-in m ks (dissoc (get-in m ks) k))))))
;; 
;; (defn dissoc-in
;;   [m ks]
;;   (let [path (butlast ks)]
;;     (if (get-in m path)
;;       (update-in m path dissoc (last ks))
;;       m))))


(defn dissoc-in*
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure.
  Nested key-values of key-list must all be associative structures 
  up to the last one - exception is thrown when not."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in* nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))


;; (satisfies? ILookup m)
(defn get-in*
  "Returns the current immutable value in a mutable nested associative structure,
  where ks is a sequence of keys. Returns nil if the key is not present,
  or the not-found value if supplied."
  {:added "1.2"
   :static true}
  ([m ks]
     (reduce get m ks))
  ([m ks not-found]
     (loop [sentinel lookup-sentinel
            m m
            ks (seq ks)]
       (if ks
         (if-not (satisfies? ILookup m)
           not-found
           (let [m (get m (first ks) sentinel)]
             (if (identical? sentinel m)
               not-found
               (recur sentinel m (next ks)))))
         m))))


;; use undefined variable/value to communicate no-value in watcher-fns
(def ^:private no-value)

;;

(defn into!
  "Copy/overwrites all kvs of src-map into dest-map.
  Uses 'maybe-keys' to iterate over src-map kvs.
  Overwrites existing values in dest-map,
  but leaves non-effected kvs alone."
  ([dest-map src-map]
    (when (and (satisfies? IMutableKVMap src-map)
               (satisfies? IMutableKVMap dest-map))
      (doseq [k (maybe-keys src-map)]
        (assoc! dest-map k (get src-map k))))))


(defn sync-mutable-maps
  "Register watcher functions with the mutable key-value map 
  src-map to update and keep in sync dest-map.
  Single key-values can be sync'ed by specifying 
  src-map-key and dest-map-key.
  The whole src-map is sync'ed to dest-map by omitting any key info.
  Returns the registered watcher-fn or nil if anything wrong.
  The registered fn-key for the watcher-fn is the fn itself.
  src-map and dest-map should satisfy IMutableKVMapWatchable."
  ([src-map src-map-key dest-map dest-map-key]
    (when (and (satisfies? IMutableKVMapWatchable src-map)
               (satisfies? IMutableKVMapWatchable dest-map))
      (let [f (fn [fnkey this mapkey oldval newval]
                (if (undefined? newval)
                  (dissoc! dest-map dest-map-key)
                  (assoc! dest-map dest-map-key newval)))]
        (add-kvmap-watch src-map src-map-key f f)
        f)))
  ([src-map dest-map]
    (when (and (satisfies? IMutableKVMapWatchable src-map)
               (satisfies? IMutableKVMapWatchable dest-map))
      (let [f (fn [fnkey this mapkey oldval newval]
                (if (undefined? newval)
                  (dissoc! dest-map mapkey)
                  (assoc! dest-map mapkey newval)))]
        (add-kvmap-watch src-map f f)
        f))))
