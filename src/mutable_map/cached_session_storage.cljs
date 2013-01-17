(ns mutable-map.cached-session-storage
  ""
  (:use 
    [mutable-map.core :only [IMutableKVMapWatchable notify-kvmap-watches add-kvmap-watch remove-kvmap-watch IMutableKVMap maybe-keys empty! update! update!* pr-edn-str read-edn-string]])
  (:require 
    [mutable-map.atomic-map]
    [mutable-map.session-storage]))


(defn make-cached-session-storage-atomic-map
  "Returns a mutable kvmap that holds a cached, two-way sync'ed copy of 
  the local storage.
  All updates to the returned kvmap will be passed thru to the local storage.
  If the local storage is changed thru the implemented assoc! and dissoc! protocols, 
  then those changes will be reflected in the returned kvmap.
  "
  []
  (let [kvm (mutable-map.atomic-map/make-atomic-map)
        ls (mutable-map.session-storage/get-session-storage)]
    (into! kvm ls)
    (sync-mutable-maps ls kvm)
    (sync-mutable-maps kvm ls)
    kvm))

