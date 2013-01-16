(ns test-mutable-kvmap)


;; local storage test

;; choose one of the following for testing
(def ls (ls/get-local-storage))
(def ls (kv/make-mutable-kvmap))

[
(count ls)
(empty! ls)
(count ls)
]
[
(assoc! ls :a 111)
(count ls)
(get ls :a)
(:a ls)
(get ls :z :nothing-there)
]
[
(= (get (assoc! ls "b" "B") "b") "B")
(= (get (assoc! ls 'b 'B) 'b) 'B)
(= (get (assoc! ls :b :B) :b) :B)
(= (get (assoc! ls nil nil) nil) nil)
(= (get (assoc! ls ["b"] ["B"]) ["b"]) ["B"])
(= (get (assoc! ls #{"b"} #{"B"}) #{"b"}) #{"B"})
(= (get (assoc! ls {:b "b"} {:B "B"}) {:b "b"}) {:B "B"})
]
[
(count ls)
(maybe-keys ls)
(map #(get ls %) (maybe-keys ls))
]
[
(= (get (dissoc! ls "b") "b" :nothing) :nothing)
(= (get (dissoc! ls 'b) 'b :nothing)  :nothing)
(= (get (dissoc! ls :b) :b :nothing)  :nothing)
(= (get (dissoc! ls nil) nil :nothing)  :nothing)
(= (get (dissoc! ls ["b"]) ["b"] :nothing) :nothing)
(= (get (dissoc! ls #{"b"}) #{"b"} :nothing) :nothing)
(= (get (dissoc! ls {:b "b"}) {:b "b"} :nothing) :nothing)
]
[
(count ls)
(maybe-keys ls)
(map #(get ls %) (maybe-keys ls))
]
[
(:a ls)
(update! ls :a inc)
(:a ls)
(update! ls :a (fn [& a] (apply + a)) 10 20)
(:a ls)
]
[
(add-kvmap-watch 
  ls 
  :a
  :my-a-key-watcher
  (fn [f-k ls k oldval newval] (println "kvmap-watch-fn(f-k,ls,k,old,new):" f-k ls k oldval newval)))
(:a ls)
(assoc! ls :a 10)
(:a ls)
(dissoc! ls :a)
(get ls :a :nothing-left)
]
[
(add-kvmap-watch 
  ls
  :my-local-storage-watcher
  (fn [f-k ls k oldval newval] (println "kvmap-watch-fn(f-k,ls,k,old,new):" f-k ls k oldval newval)))
(:a ls)
(assoc! ls :a 10)
(:b ls)
(assoc! ls :b 42)
(:a ls)
(dissoc! ls :b)
(get ls :b :nothing-left)
]
[
(assoc! ls :a 20)
(remove-kvmap-watch ls :a :my-a-key-watcher)
(assoc! ls :a 21)
(remove-kvmap-watch ls ls :my-local-storage-watcher)
(assoc! ls :a 22)
]

;;;

