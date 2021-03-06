(ns mutable-map.core
  "Protocols and helper-fns implementations for mutable maps.
  This is the main namespace to require/refer."
  )


;; use undefined variable/value to communicate no-value in watcher-fns
;; very cljs specific unfortunately
(def ^:private no-value)


;; encoding fns to accommodate js-interoperability
;; by encoding strings as strings, "this is str" instead of "\"this is str\""
;; edn encoded string is prefixed by clj-edn-prefix ("clj-edn:"),
;; except for string-type, which is just a string
;; this encoding scheme allows for keys and values to be read and written as pure strings
;; and will allow js-interoperability for js-specific keys and values

(def ^{:dynamic true} *pure-edn-encoding* false)

;; prefix to use for edn-encoding of non-string types
(def clj-edn-prefix "clj-edn:")

(defn pr-edn-str
  "Equivalent of pr-str, but when *pure-edn-encoding* is false,
  edn-encoding will be prepended with clj-edn-prefix for non-strings"
  [o]
  (if *pure-edn-encoding*
    (pr-str o)
    (if (string? o)
      o 
      (str clj-edn-prefix (pr-str o)))))

(defn read-edn-string
  "Equivalent of read-string, but when *pure-edn-encoding* is false,
  edn-encoded string is expected to be prepended with clj-edn-prefix for non-strings"
  [s]
  (if *pure-edn-encoding*
    (cljs.reader/read-string s)
    (let [n (count clj-edn-prefix)]
      (if (= (subs s 0 n) clj-edn-prefix)
        (cljs.reader/read-string (subs s n))
        s))))


;; protocol definitions

(defprotocol IMutableKVMapWatchable
  "A mutable kvmap is a key-value store that lends itself to
  a map-like interface of one level deep.
  This protocol defines a watchers/pubsub interface,
  where watchers can be registered on the key as well as the whole store.
  Mimics the IWatchable interface with the addition of a mapkey to identify 
  the key that should be watched."
  (notify-kvmap-watches [this mapkey oldval newval] 
    "Notifies all registered watcher-fns of the changed value for mapkey.")
  (add-kvmap-watch [this fnkey watcher-fn][this mapkey fnkey watcher-fn]
    "Registers a watcher-fn for mapkey value changes with a watch-fn id of fnkey.
    When value changes, the following is called:
    (watcher-fn fnkey this mapkey oldval newval)
    When this kvmap is passed without a mapkey, or if the mapkey equals this kvmap,
    then the watcher-fn is registered on the kvmap itself, and will be notified 
    for every change of every key-value.")
  (remove-kvmap-watch [this][this mapkey][this mapkey fnkey]
    "Remove all the watcher-fns registered on all the mapkeys of this kvmap,
    or remove all the watcher-fns registered on the mapkey,
    or remove the single watcher-fn registered on the mapkey with id fnkey.
    If mapkey equals this kvmap then the watcher-fns registered on the kvmap itself
    are removed.")
    )

(defprotocol IMutableKVMap
  ""
  (update!* [this k f args]
    "'Updates' a value in key-value list, where k is a
  key and f is a function that will take the old value associated with that key
  and any supplied args and return the new value. Update is made in-place.
  If any levels do not exist, hash-maps will be created.
  Watcher-fns will be notified for affected key.")
  (empty! [this]
    "Will empty the mutable map while notifying all watcher-fns appropriately.")
  (maybe-keys [this]
    "Standardized interface for obtaining a current list of keys from a mutable kvmap.
  The name 'maybe-keys' reflects the imperfect knowledge obtained...")  
  )

;; todo...
(defprotocol IMutableMap
  ""
  (assoc-in! [m ks v]
    "Associates a value in a nested associative structure, 
    where ks is a sequence of keys and v is the new value.
    If any levels do not exist, hash-maps will be created.
    Returns the changed mutable map.")
  (dissoc-in! [this ks]
    "Removes the value in a nested associative structure refered to by ks,
    which is a sequence of keys.
    Returns the changed mutable map.")
  (update-in!* [m ks f args]
    "'Updates' a value in a nested associative structure, where ks is a
    sequence of keys and f is a function that will take the old value
    and any supplied args and return the new value, and returns a new
    nested structure.  If any levels do not exist, hash-maps will be created.")
  )


;; helper fns to make it all look seamless


(defn get-in**
  "Returns the value in a nested associative structure,
    where ks is a sequence of keys. Returns nil if the key
    is not present, or the not-found value if supplied.
    This is a bug-fixed version that returns not-found for non-assoc structures."
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


;; just transforming "& args" to "args" to accommodate protocol shortcomings ;-)
(defn update! 
  "'Updates' a value in key-value list, where k is a
  key and f is a function that will take the old value associated with that key
  and any supplied args and return the new value. Update is made in-place.
  If any levels do not exist, hash-maps will be created.
  Watcher-fns will be notified for affected key."
  [this k f & args]
  (update!* this k f args))


(defn update-in! 
  "'Updates' a value in key-value list, where k is a
  key and f is a function that will take the old value associated with that key
  and any supplied args and return the new value. Update is made in-place.
  If any levels do not exist, hash-maps will be created.
  Watcher-fns will be notified for affected key."
  [this ks f & args]
  (update-in!* this ks f args))


;; generic implementations independent of specific deftype

(defn into!
  "Copy/overwrites all kvs of src-map into dest-map.
  dest-map must satisfy ITransientAssociative (assoc!)
  src-map can be map or mutable map satisfying IMutableKVMap (maybe-keys).
  Existing values in dest-map are overwritten.
  Associated dest-map's watcher-fns are notified when registered.
  Returns dest-map."
  ([dest-map src-map]
    (when (satisfies? IMutableKVMap dest-map)
      (let [ks (if (satisfies? IMutableKVMap src-map)
                 (maybe-keys src-map)
                 (keys src-map))]
        (doseq [k ks]
          (assoc! dest-map k (get src-map k)))))
    dest-map))


(defn sync-mutable-maps
  "Register watcher functions with the mutable key-value map 
  src-map to update and keep in with sync dest-map.
  Single key-values can be sync'ed by specifying 
  src-map-key and dest-key-map. If dest-map-key is omitted, 
  then the src-key-map will be used for the dest-map as well.
  The whole src-map is sync'ed to dest-map by omitting any specific key.
  Returns the registered watcher-fn or nil if anything wrong.
  The registered fn-key for the watcher-fn is the fn itself.
  src-map and dest-map should satisfy IMutableKVMapWatchable."
  ([src-map src-map-key dest-map]
    (sync-mutable-maps src-map src-map-key dest-map src-map-key))
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
