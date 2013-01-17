(ns mutable-map.session-storage
  "An idiomatic interface to the browser's local storage.
  Notice: This code is based on an initial shoreleave-browser 0.2.2 implementation - hopefully some of this code can make its way back..."
  (:use 
    [mutable-map.protocols :only [IMutableKVMapWatchable notify-kvmap-watches add-kvmap-watch remove-kvmap-watch IMutableKVMap maybe-keys empty! update! update!* pr-edn-str read-edn-string]]
  	)
  (:require 
    [mutable-map.atomic-map]
    [mutable-map.utils]
    [cljs.reader :as reader]
    [goog.storage.mechanism.HTML5SessionStorage :as html5ls]))


;; Watchers
;; --------
;;
;; In most applications, you want to trigger actions when data is changed.
;; To support this, Shoreleave's local storage use IWatchable and maintains
;; the watchers in an atom.

(def ls-watchers (atom {}))

;; `sessionStorage` support
;; ----------------------
;;
;; For general information on sessionStorage, please see [Mozilla's docs](https://developer.mozilla.org/en/DOM/Storage#sessionStorage)
;;
;; Shoreleave's sessionStorage support is built against Closure's [interface](http://closure-library.googlecode.com/svn/docs/class_goog_storage_mechanism_HTML5SessionStorage.html)
;;
;; The extension supports the following calls:
;;
;;  * map-style lookup - `(:search-results local-storage "default value")`
;;  * `get` lookups
;;  * `(count local-storage)` - the number of things/keys stored
;;  * `(assoc! local-storage :new-key "saved")` - update or add an item
;;  * `(dissoc! local-storage :saved-results)` - remove an item
;;  * `(empty! local-storage)` - Clear out the sessionStorage store



;; use undefined variable/value to communicate no-value in watcher-fns
(def ^:private no-value)


;; The HTML5 Session Storage is a singleton, i.e. only one instance.
;; Not sure why Closure's goog.storage.mechanism.HTML5SessionStorage
;; constructor yields new variable instances for the same store (???)
;; the following tries to give you always the same var such that
;; you can actually compare them to be equal


(def session-storage (goog.storage.mechanism.HTML5SessionStorage.))

(defn get-session-storage
  "Get the browser's sessionStorage"
  [] session-storage)


(def ls-kvmap-watchers-atom (atom {}))
(def ls-kvmap-key-watchers-atom (atom {}))

(extend-type goog.storage.mechanism.HTML5SessionStorage

  ILookup

  (-lookup
    ([ls k]
      (-lookup ls k nil))
    ([ls k not-found]
      (if-let [v (.get ls (pr-edn-str k))]
        (read-edn-string v)
        not-found)))

  ICounted

  (-count [ls] 
    (.getCount ls))

  IFn
  
  (-invoke
    ([ls k]
      (-lookup ls k))
    ([ls k not-found]
      (-lookup ls k not-found))) 

  ITransientAssociative
  
  (-assoc! [ls k v]
    (let [oldval (get ls k no-value)]
      (when-not (or (and (undefined? v)(undefined? oldval))
                    (= oldval v))
        (if (undefined? v)
          (.remove ls (pr-edn-str k))
          (.set ls (pr-edn-str k) (pr-edn-str v)))
;;         (-notify-watches ls {:key k :value oldval} {:key k :value v})
        (notify-kvmap-watches ls k oldval v)
        ))
    ls)

  ITransientMap
  
  (-dissoc! [ls k]
    (let [oldval (get ls k no-value)]
      (when-not (undefined? oldval)
        (.remove ls (pr-edn-str k))
        ;; next is a hack to communicate the key to the notify-watches context
        ;; protocol doesn't really match well, but this way it "works"
;;         (-notify-watches ls {:key k :value oldval} {:key k :value nil})
        (notify-kvmap-watches ls k oldval no-value)))
    ls)

  ;; ITransientCollection

  ;; ough... jumping thru hoops to fulfill the IWatchable protocol requirements
  ;; the storage lookup key is not the same as the IWatchable's fnkey...
  ;; TODO: need to be able to add multiple watchers per key
  IWatchable
  (-notify-watches [ls oldval newval]
    (let [mapkey (:key oldval)]
      (when-let [fns-map (get @ls-watchers mapkey nil)]
        (doseq [k-f fns-map]
          ;; pass the mapkey instead of the map-ref, 
          ;; because the local storage is a well-known singleton
          ;; and the mapkey is the only useful "ref" to what changed
          ((val k-f) (key k-f) mapkey (:value oldval) (:value newval)))))
;;           ((val k-f) (key k-f) ls (:value oldval) (:value newval)))))
    ls)
  (-add-watch [ls [mapkey fnkey] f]
    (swap! ls-watchers assoc-in [mapkey fnkey] f))
  (-remove-watch [ls [mapkey fnkey]]
    (let [fns-map (get-in @ls-watchers [mapkey])
          new-fns-map (dissoc fns-map fnkey)]
      (if (empty? new-fns-map)
        (swap! ls-watchers dissoc mapkey)
        (swap! ls-watchers assoc-in [mapkey] new-fns-map))))

  
  IMutableKVMapWatchable
  
  (notify-kvmap-watches [ls mapkey oldval newval]
    (when-let [fns-map @ls-kvmap-watchers-atom]
      (doseq [k-f fns-map]
        ((val k-f) (key k-f) ls mapkey oldval newval)))
    (when-let [fns-map (get @ls-kvmap-key-watchers-atom mapkey nil)]
      (doseq [k-f fns-map]
        ((val k-f) (key k-f) ls mapkey oldval newval)))
    ls)
    
  (add-kvmap-watch 
    ([ls fnkey f]
      (swap! ls-kvmap-watchers-atom assoc fnkey f)
      ls)
    ([ls mapkey fnkey f]
      (if (= ls mapkey)
        (add-kvmap-watch ls fnkey f)
        (swap! ls-kvmap-key-watchers-atom assoc-in [mapkey fnkey] f))
      ls))
      
  (remove-kvmap-watch
    ([ls mapkey fnkey]
      (if (= ls mapkey)
        (swap! ls-kvmap-watchers-atom dissoc fnkey)
        (let [fns-map (get-in @ls-kvmap-key-watchers-atom [mapkey])
              new-fns-map (dissoc fns-map fnkey)]
          (if (empty? new-fns-map)
            (swap! ls-kvmap-key-watchers-atom dissoc mapkey)
            (swap! ls-kvmap-key-watchers-atom assoc-in [mapkey] new-fns-map))))
      ls)
    ([ls mapkey]
      (if (= ls mapkey)
        (reset! ls-kvmap-watchers-atom {})
        (swap! ls-kvmap-key-watchers-atom dissoc mapkey))
      ls)
    ([ls]
      (reset! ls-kvmap-watchers-atom {})
      (reset! ls-kvmap-key-watchers-atom {})
      ls))
      
  IMutableKVMap
  
;; Return the current list of keys of the local storage as a list of cljs-values.
;; Note that from the moment that list is generated, it may be out-of-date
;; as the local storage can be changed from other threads of work, 
;; even from other browser windows.
  (maybe-keys [ls]
    (for [i (range (.-length js/sessionStorage))] 
      (read-edn-string (.key js/sessionStorage i))))
;; alternative implementation with goog's iterator... needs work
;;     (let [i (.__iterator__ ls true)] 
;;       (loop [lsks []] 
;;         (let [k (try (.next i) (catch js/Object e))]
;;           (if-not k
;;             lsks
;;             (recur (conj lsks (read-edn-string k)))))))))

  (empty! [ls]
    (let [ks (maybe-keys ls)]
      (.clear ls)
      (doseq [k ks]
        (notify-kvmap-watches ls k nil no-value)))
    ls)

  ;; not an atomic operation...
  (update!* [ls mapkey f args]
    (let [oldval (-lookup ls mapkey no-value)]
      (when-not (undefined? oldval)
        (let [newval (apply f oldval args)]
          (if (undefined? newval)
            (dissoc! ls mapkey)
            (assoc! ls mapkey newval)))))
    ls)
  )


;;;;;;;;

(defn register-session-storage-event-watcher 
  "Register a 'storage' event handler that will notify the registered
  session-storage IMutableKVMapWatchable's watchers.
  The storage event will fire when the local storage is changed from
  within other windows, and could be used to communicate state between
  different windows served from the same domain."
  []
  (.addEventListener js/window
    "storage" 
    (fn [e] 
  ;;     (println "storage event")
      (let [storage-area (.-storageArea e)
            session-storage? (= storage-area js/sessionStorage)]
        (when session-storage?
          (let [ls (get-session-storage)
                mapkey (read-edn-string (.-key e))
                oldValue (.-oldValue e)
                oldval (if oldValue (read-edn-string (.-oldValue e)) no-value)
                newValue (.-newValue e)
                newval (if newValue (read-edn-string newValue) no-value)]
;;         (println "\"storage\" event(mapkey, oldValue, newval, storageArea, session-storage?):" mapkey oldval newval storage-area session-storage?)
        (notify-kvmap-watches ls mapkey oldval newval)))))
    false))


(defn make-cached-session-storage-kvmap
  "Returns a mutable kvmap that holds a cached, two-way sync'ed copy of 
  the local storage.
  All updates to the returned kvmap will be passed thru to the local storage.
  If the local storage is changed thru the implemented assoc! and dissoc! protocols, 
  then those changes will be reflected in the returned kvmap.
  "
  []
  (let [kvm (mutable-map.atomic-map/make-atomic-map)
        ls (get-session-storage)]
    (mutable-map.utils/into! kvm ls)
    (mutable-map.utils/sync-mutable-maps ls kvm)
    (mutable-map.utils/sync-mutable-maps kvm ls)
    kvm))
    
