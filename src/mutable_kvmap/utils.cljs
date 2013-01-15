(ns mutable-kvmap.utils
  ""
  )


;; clj-js-string
(def clj-edn-prefix "clj-edn:")

(defn encode-edn-str [o]
  (if (string? o)
    o 
    (str clj-edn-prefix (pr-str o))))

(defn decode-edn-str [s]
  (let [n (count clj-edn-prefix)]
    (if (= (subs s 0 n) clj-edn-prefix)
      (cljs.reader/read-string (subs s n))
      s)))


;;


(defn sync-mutable-kvmaps
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
                (if (= newval no-val)
                  (dissoc! dest-map dest-map-key)
                  (assoc! dest-map dest-map-key newval)))]
        (add-kvmap-watch src-map src-map-key f f)
        f)))
  ([src-map dest-map]
    (when (and (satisfies? IMutableKVMapWatchable src-map)
               (satisfies? IMutableKVMapWatchable dest-map))
      (let [f (fn [fnkey this mapkey oldval newval]
                (if (= newval no-val)
                  (dissoc! dest-map map-key)
                  (assoc! dest-map map-key newval)))]
        (add-kvmap-watch src-map f f)
        f))))
