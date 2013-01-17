mutable-map
=============

Protocols and implementation of mutable-map abstractions for atomic-map, local-storage and session-storage.

A mutable-map is essentially a map where changes are made in-place... not very functional, but unfortunately, we have to live with those mutable artifacts in the real-world.

###Basic Interfaces

The available operations are similar to the immutable one, except for the bang, i.e. "!". So we have assoc!, dissoc!, update!, assoc-in!, dissoc-in!, and update-in!, which change the map.
To query the mutable map, we have the familiar operations, like get, get-in, and functional gets for keywords and such. All these gets return immutable data-structures to work safely with ;-)

In addition, there are protocols defined to register watcher-function on the changes of the map values. The granularity of the watcher is per key or the whole map. Those watcher fns are very similar to those defined for atoms and other refs, except they the interface of the registered call-back function includes the key or the nested key-sequence where the change was made. This allows for a more fine-grained one-way dependency graph.

###mutual-map Implementations

The three implementation are the following

* atomic-map  
	A map wrapped in an atom thru which all the changes are channeled/managed. 
	
* local-storage  
	A mutable-map abstraction for the HTML5 Local Storage. 

* session-storage  
	A mutable-map abstraction for the HTML5 Session Storage. 

All three implementations conform to the exact same basic interfaces, and there are no restriction on the data-types for keys and values. The HTML5 storage mechanisms require string formats for the keys and values, but the mutable-map implementations hide the necessary edn-encodings/decodings behind the interfaces. 

###Notifications

As long as the changes to the Local and Session Storage are made thru this mutable-map interface, then the exact same change-notification system will work.

Even changes to the Local and Session Storage made in other windows, will invoke the locally registered notification functions. Those remote changes can even be made by native javascript calls thru the HTML5 storage API.

Note however, that any native javascript library that makes local changes, will not fire off the events that are needed for the cljs-notification... a limitation of the browser implementations. (maybe there are ways around this... investigate...)

###Acknowledgement

This code is based on an initial shoreleave-browser 0.2.2 implementation - hopefully some of this code can make its way back...

