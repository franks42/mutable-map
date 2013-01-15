(ns mutable-kvmap.protocols
  ""
  )

;; define a no-value variable to distinguish between nil and no-value/non-existent
;; (def no-value :absolutely-non-existent-value)
(def no-value "uri:http://clojure.org/uri/no-value")


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
    If mapkey equals this kvmap then the watcher-fns registered on the kvmap itself are removed.")
    )

