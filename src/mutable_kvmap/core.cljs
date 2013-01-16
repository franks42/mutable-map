(ns mutable-kvmap.core
  ""
  (:use 
    [mutable-kvmap.protocols :only [IMutableKVMapWatchable notify-kvmap-watches add-kvmap-watch remove-kvmap-watch IMutableKVMap maybe-keys empty! update!]]
  	)
  (:require [cljs.reader :as reader]
            [mutable-kvmap.utils]
    ))

;; use undefined variable/value to communicate no-value in watcher-fns
(def ^:private no-value)

(deftype MutableKVMap [kvmap-atom kvmap-key-watchers-atom kvmap-watchers-atom])

(defn make-mutable-kvmap [] (MutableKVMap. (atom {}) (atom {}) (atom {})))

(extend-type MutableKVMap
  
  ILookup
  
  (-lookup
    ([kvm mapkey]
      (-lookup kvm mapkey nil))
    ([kvm mapkey not-found] 
      (let [kvm-atm (.-kvmap-atom kvm)]
        (get @kvm-atm mapkey not-found))))

  ICounted
  
  (-count [kvm] 
    (count (deref (.-kvmap-atom kvm))))

  IFn
  
  (-invoke
    ([kvm k]
      (-lookup kvm k))
    ([kvm k not-found]
      (-lookup kvm k not-found))) 

  ITransientAssociative
  
  ;; not an atomic operation!!!
  ;; oldval may have changed before swap! is called, 
  ;; which would yield the wrong oldval in the watcher-fn call
  ;; probably won't matter in most situations... be aware, be warned
  (-assoc! [kvm mapkey newval]
    (let [kvm-atm (.-kvmap-atom kvm)
          oldval (-lookup kvm mapkey no-value)]
      (when-not (or (and (undefined? newval)(undefined? oldval))
                    (= oldval newval))
        (if (undefined? newval)
          (swap! kvm-atm dissoc mapkey)
          (swap! kvm-atm assoc mapkey newval))
        (notify-kvmap-watches kvm mapkey oldval newval)))
    kvm)

  ITransientMap
  
  (-dissoc! [kvm mapkey]
    (let [kvm-atm (.-kvmap-atom kvm)
          oldval (-lookup kvm mapkey no-value)]
      (when-not (undefined? oldval)
        (swap! kvm-atm dissoc mapkey)
        (notify-kvmap-watches kvm mapkey oldval no-value)))
    kvm)

  IMutableKVMapWatchable
  
  (notify-kvmap-watches [kvm mapkey oldval newval]
    (let [kvm-watchers-atm (.-kvmap-watchers-atom kvm)]
      (when-let [fns-map @kvm-watchers-atm]
        (doseq [k-f fns-map]
          ((val k-f) (key k-f) kvm mapkey oldval newval))))
    (let [kvm-key-watchers-atm (.-kvmap-key-watchers-atom kvm)]
      (when-let [fns-map (get @kvm-key-watchers-atm mapkey nil)]
        (doseq [k-f fns-map]
          ((val k-f) (key k-f) kvm mapkey oldval newval))))
    kvm)
    
  (add-kvmap-watch 
    ([kvm fnkey f]
      (swap! (.-kvmap-watchers-atom kvm) assoc fnkey f)
      kvm)
    ([kvm mapkey fnkey f]
      (swap! (.-kvmap-key-watchers-atom kvm) assoc-in [mapkey fnkey] f)
      kvm))
      
  (remove-kvmap-watch
    ([kvm mapkey fnkey]
      (if (= kvm mapkey)
        (swap! (.-kvmap-watchers-atom kvm) dissoc fnkey)
        (let [fns-map (get-in @(.-kvmap-key-watchers-atom kvm) [mapkey])
              new-fns-map (dissoc fns-map fnkey)]
          (if (empty? new-fns-map)
            (swap! (.-kvmap-key-watchers-atom kvm) dissoc mapkey)
            (swap! (.-kvmap-key-watchers-atom kvm) assoc-in [mapkey] new-fns-map))))
      kvm)
    ([kvm mapkey]
      (if (= kvm mapkey)
        (reset! (.-kvmap-watchers-atom kvm) {})
        (swap! (.-kvmap-key-watchers-atom kvm) dissoc mapkey))
      kvm)
    ([kvm]
      (reset! (.-kvmap-watchers-atom kvm) {})
      (reset! (.-kvmap-key-watchers-atom kvm) {})
      kvm))

  ;; deref'ing the kvmap gives you an immutable map instance to work with.
  ;; so does any get/lookup get you an immutable key-value
  IDeref
  
  (-deref [kvm] 
    (deref (.-kvmap-atom kvm)))

  IMutableKVMap
    
  (maybe-keys [kvm]
    (keys @kvm))
  
  (empty! [kvm]
    (doseq [k (maybe-keys kvm)]
      (dissoc! kvm k)))

  (update! [kvm mapkey f & args]
    (let [kvm-atm (.-kvmap-atom kvm)
          oldval (-lookup kvm mapkey no-value)]
      (when-not (undefined? oldval)
        (let [newval (apply f oldval args)]
          (if (undefined? newval)
            (swap! kvm-atm dissoc mapkey)
            (swap! kvm-atm assoc mapkey newval))
          (notify-kvmap-watches kvm mapkey oldval newval))))
    kvm)

  ;; ITransientCollection
  )

