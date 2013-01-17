(ns mutable-map.cached-local-storage
  ""
  (:use 
    [mutable-map.core :only [IMutableKVMapWatchable notify-kvmap-watches add-kvmap-watch remove-kvmap-watch IMutableKVMap maybe-keys empty! update! update!* pr-edn-str read-edn-string sync-mutable-maps into!]])
  (:require 
    [mutable-map.atomic-map]
    [mutable-map.local-storage]))


(defn make-cached-local-storage-atomic-map
  "Returns a mutable atomic-map that holds a cached, two-way sync'ed copy of 
  the local storage.
  All updates to the returned kvmap will be passed thru to the local storage.
  If the local storage is changed thru the implemented assoc! and dissoc! protocols, 
  then those changes will be reflected in the returned atomic-map.
  "
  []
  (let [kvm (mutable-map.atomic-map/make-atomic-map)
        ls (mutable-map.local-storage/get-local-storage)]
    (into! kvm ls)
    (sync-mutable-maps ls kvm)
    (sync-mutable-maps kvm ls)
    kvm))
    
