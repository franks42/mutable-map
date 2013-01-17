(ns test-mutable-kvmap)


;; local storage test

;; choose one of the following for testing
(def ls (ls/get-local-storage))
(def ss (ss/get-session-storage))
(def am (am/make-atomic-map))

(def m ls)
(def m ss)
(def m am)

[
(count m)
(empty! m)
(count m)
]
[
(assoc! m :a 111)
(count m)
(get m :a)
(:a m)
(get m :z :nothing-there)
]
[
(= (get (assoc! m "b" "B") "b") "B")
(= (get (assoc! m 'b 'B) 'b) 'B)
(= (get (assoc! m :b :B) :b) :B)
(= (get (assoc! m nil nil) nil) nil)
(= (get (assoc! m ["b"] ["B"]) ["b"]) ["B"])
(= (get (assoc! m #{"b"} #{"B"}) #{"b"}) #{"B"})
(= (get (assoc! m {:b "b"} {:B "B"}) {:b "b"}) {:B "B"})
]
[
(count m)
(maybe-keys m)
(map #(get m %) (maybe-keys m))
]
[
(= (get (dissoc! m "b") "b" :nothing) :nothing)
(= (get (dissoc! m 'b) 'b :nothing)  :nothing)
(= (get (dissoc! m :b) :b :nothing)  :nothing)
(= (get (dissoc! m nil) nil :nothing)  :nothing)
(= (get (dissoc! m ["b"]) ["b"] :nothing) :nothing)
(= (get (dissoc! m #{"b"}) #{"b"} :nothing) :nothing)
(= (get (dissoc! m {:b "b"}) {:b "b"} :nothing) :nothing)
]
[
(count m)
(maybe-keys m)
(map #(get m %) (maybe-keys m))
]
[
(:a m)
(update! m :a inc)
(:a m)
(update! m :a (fn [& a] (apply + a)) 10 20)
(:a m)
]
[
(add-kvmap-watch 
  m 
  :a
  :my-a-key-watcher
  (fn [f-k m k oldval newval] (println "kvmap-watch-fn(f-k,m,k,old,new):" f-k m k oldval newval)))
(:a m)
(assoc! m :a 10)
(:a m)
(dissoc! m :a)
(get m :a :nothing-left)
]
[
(add-kvmap-watch 
  m
  :my-local-storage-watcher
  (fn [f-k m k oldval newval] (println "kvmap-watch-fn(f-k,m,k,old,new):" f-k m k oldval newval)))
(:a m)
(assoc! m :a 10)
(:b m)
(assoc! m :b 42)
(:a m)
(dissoc! m :b)
(get m :b :nothing-left)
]
[
(assoc! m :a 20)
(remove-kvmap-watch m :a :my-a-key-watcher)
(assoc! m :a 21)
(remove-kvmap-watch m m :my-local-storage-watcher)
(assoc! m :a 22)
]

;;;

