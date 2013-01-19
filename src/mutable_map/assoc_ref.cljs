(ns mutable-map.assoc-ref
  ""
  (:use 
    [mutable-map.core :only [IMutableKVMapWatchable notify-kvmap-watches add-kvmap-watch remove-kvmap-watch IMutableKVMap maybe-keys empty! update! update!* pr-edn-str read-edn-string sync-mutable-maps into!]])
  (:require 
    [mutable-map.atomic-map]
    [mutable-map.local-storage]))


(deftype AssocRef [ks])
;; (def r (AssocRef. [m k0 k1 k2 k3]))

(defn aref [m & ks]
  (AssocRef. (into [m] ks)))
;; (def r (assoc-ref m k0 k1 k2 k3))


(defprotocol IAssocRef
  
  (get* [ar][ar no-value]
  "")
  (assoc* [ar v])
  (dissoc* [ar])
  (update** [ar f args])
  
  )

(defprotocol IMutableAssocRef
  
  (assoc*! [ar v])
  (dissoc*! [ar])
  (update**! [ar f args])
  (clean*! [ar])
  )


(defn update*
  ""
  [ar f & args] (-update* ar f args))


(extend-type AssocRef
  IAssocRef
  (get* 
    ([ar] (get* ar nil))
    ([ar no-value]
      (let [ars (.-ks ar)
            [m k & ks] ars]
;;         (println "ars m k ks: " ars m k ks)
        (if k
          (if (seq ks)
            (mutable-map.core/get-in* m (rest ars) no-value)
            (get m k no-value))
          m))))
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
  )
