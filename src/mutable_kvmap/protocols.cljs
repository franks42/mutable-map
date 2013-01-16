(ns mutable-kvmap.protocols
  ""
  )


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

(defprotocol IMutableMap
  ""
  (get-in*  [m ks] [m ks not-found]
    "Returns the value in a nested associative structure,
    where ks is a sequence of keys. Returns nil if the key
    is not present, or the not-found value if supplied.
    This is a bug-fixed version that returns not-found for non-assoc structures.")
  (assoc-in! [m ks v]
    "Associates a value in a nested associative structure, 
    where ks is a sequence of keys and v is the new value.
    If any levels do not exist, hash-maps will be created.
    Returns the changed mutable map.")
  (dissoc-in! [this ks]
    "")
  (update-in! [m ks f & args]
    "'Updates' a value in a nested associative structure, where ks is a
    sequence of keys and f is a function that will take the old value
    and any supplied args and return the new value, and returns a new
    nested structure.  If any levels do not exist, hash-maps will be created.")
  )


(defn update! [this k f & args] (update!* this k f args))
