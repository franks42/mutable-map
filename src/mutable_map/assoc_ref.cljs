;; (ns mutable-map.assoc-ref
;;   ""
;;   (:use 
;;     [mutable-map.core :only [IMutableKVMapWatchable notify-kvmap-watches add-kvmap-watch remove-kvmap-watch IMutableKVMap maybe-keys empty! update! update!* pr-edn-str read-edn-string sync-mutable-maps into!]])
;;   (:require 
;;     [mutable-map.atomic-map]
;;     [mutable-map.local-storage]))


(deftype AssocRef [ks])
;; (def r (AssocRef. [m k0 k1 k2 k3]))

(defn aref [m & ks]
  (if (= (type m) AssocRef)
    (AssocRef. (into (.-ks m) ks))
    (AssocRef. (into [m] ks))))
;; (def r (assoc-ref m k0 k1 k2 k3))

(defn up-aref [ar]
  (when-let [bl (butlast (.-ks ar))]
    (apply aref bl)))

(defn jsref [& ks]
    (AssocRef. (into [js/window] 
                     (flatten (map #(clojure.string/split (str %) #"\.") ks)))))


(defprotocol IAssocLookup
  
  (get-in* [m ks][m ks no-value]
  ""))


(defprotocol IAssocRef
  
  (get* [ar][ar no-value]
  "")
  (assoc* [ar v])
  (dissoc* [ar])
  (-update* [ar f args])
  
  )

(defprotocol IMutableAssocRef
  
  (assoc*! [ar v])
  (dissoc*! [ar])
  (-update*! [ar f args])
  (clean*! [ar])
  )


(defn update*
  ""
  [ar f & args] (-update* ar f args))

(defn update*!
  ""
  [ar f & args] (-update*! ar f args))


(extend-type AssocRef
  IAssocRef
  (get* 
    ([ar] (get* ar nil))
    ([ar no-value]
      (let [ars (.-ks ar)
            m (first ars)]
        (if (undefined? m)
          no-value
            (if-let [sks (seq (rest ars))]
              (if (= js/window m)
                (apply aget m (rest ars))
                ;; at least one key
                (if-let [sks1 (seq sks)]
                  ;; at least 2 keys
                  (mutable-map.core/get-in** m (rest ars) no-value)
                  (get m k no-value)))
              m)))))
          
  (assoc* [ar v]
    )

  (dissoc* [ar]
    )

  (update** [ar f args]
    )


  IMutableAssocRef
  (assoc*! [ar v]
    )
  (dissoc*! [ar]
    )
  (update**! [ar f args]
    )
  (clean*! [ar]
    )
  
  IDeref
  
  (-deref [ar] 
    (get* ar))
  
  IFn
  
  (-invoke
;;     ([ar]
;;       (let [ur @(up-aref ar)]
;;         (if (or (nil? ur)(= ur js/window))
;;         (.call @ar)
;;         (.call @ar ur))))
;;     ([ar a1]
;;       (let [ur @(up-aref ar)]
;;         (if (or (nil? ur)(= ur js/window))
;;         (.call @ar a1)
;;         (.call @ar ur a1))))
    ([ar](.call @ar @(up-aref ar)))
    ([ar a1](.call @ar @(up-aref ar) (clj->js a1)))
    ([ar a1 a2](.call @ar @(up-aref ar) (clj->js a1) (clj->js a2)))
    ([ar a1 a2 a3](.call @ar @(up-aref ar) (clj->js a1) (clj->js a2) (clj->js a3)))
    ([ar a1 a2 a3 a4](.call @ar @(up-aref ar) (clj->js a1) (clj->js a2) (clj->js a3) (clj->js a4)))
    ([ar a1 a2 a3 a4 a5](.call @ar @(up-aref ar) (clj->js a1) (clj->js a2) (clj->js a3) (clj->js a4) (clj->js a5)))
    )

  )
