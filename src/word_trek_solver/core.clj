(ns word-trek-solver.core
  "Inspiration taken from http://stackoverflow.com/questions/746082/how-to-find-list-of-possible-words-from-a-letter-matrix-boggle-solver/"
  (:require [clojure.java.io :as io]
            [clojure.string :as st]
            [clucy.core :as clucy]))

;;; Declarations

(declare trie load-dictionary-trie
         prefix? dfs
         perfect-square?
         dfs-pred)

;;; API

(defn solve
  "Given a string of 'letters', and optionally a collection of desired
  'word-sizes', output a collection of words that can be formed by
  assembling the letters into a matrix.

  The length of 'letters' must be a perfect square (i.e. the number of rows must
  be equal to the number of columns.)

  'word-sizes' is a collection of integers representing the acceptable word
  lengths in the solution."
  ([letters] (solve letters (range (count letters))))
  ([letters word-sizes]
   (let [trie (load-dictionary-trie)
         len (count letters)
         dim (and (perfect-square? len) (int (Math/sqrt len)))
         valid-size? (set word-sizes)]
     (assert dim "Input string length must be a perfect square.")
     (->> (mapv vec (partition dim (st/lower-case letters)))
          (dfs (dfs-pred word-sizes (load-dictionary-trie)))
          (filter (comp valid-size? count))))))

;;; Private

(defn- dfs-pred
  [word-sizes trie]
  (let [max-length (apply max word-sizes)]
    (fn [[i j s path]]
      (and (< (count s) max-length)
           (prefix? trie s)))))

(defn- perfect-square?
  "Based on the 'Babylonian Algorithm'
  https://en.wikipedia.org/wiki/Methods_of_computing_square_roots"
  [n]
  (boolean
   (when (> n 1)
     (loop [x (quot n 2)
            seen #{x}]
       (if (= (* x x) n)
         true
         (let [x (quot (+ x (quot n x)) 2)]
           (if (seen x)
             false
             (recur x (conj seen x)))))))))

(defn- trie
  [lst]
  (let [index (clucy/memory-index)]
    (apply clucy/add index (mapv (fn [word] {:v word}) lst))
    index))

(defn- prefix?
  [trie prefix]
  (pos? (:_total-hits (meta (clucy/search trie (str prefix "*") 1)))))

(def ^:private dictionary
  (delay (set (map st/lower-case (st/split (slurp (io/resource "words_100000.txt")) #"\n")))))

(def ^:private dictionary-trie
  (delay (trie @dictionary)))

(defn- load-dictionary-trie
  []
  @dictionary-trie)

(defn- word?
  [w]
  (boolean (@dictionary w)))

(defn- dfs
  "Run a depth-first search against the provided 'matrix', preventing any nodes
  that fail a check against 'pred' from being added to the queue. We are using a
  more imperative approach here in order to avoid as many intermediary
  representations as possible. A volatile for the queue, and a transient for the
  words has been used."
  ([matrix] (dfs (constantly true) matrix))
  ([pred matrix]
   (let [queue (volatile! [])
         words (transient #{})
         rows (count matrix)
         cols (count (first matrix))
         get-letter (fn [i j] (get-in matrix [j i]))]

     ;; seed the queue
     (dotimes [i cols]
       (dotimes [j rows]
         (let [c (get-letter i j)]
           (when (<= 97 (int c) 123)
             (let [node [i j (str c) #{[i j]}]]
               (when (pred node)
                 (vswap! queue conj node)))))))

     ;; run dfs
     (loop []
       (when-let [queue* (not-empty @queue)]
         (let [[i j s path-set] (first queue*)]
           (vswap! queue next)

           ;; for every node adjacent to the current node, inspect the word it
           ;; constructs, and then add it to the queue provided it is
           ;; 1) on the matrix, 2) is a word and 3) passes the 'pred' function
           ;; running against it.
           (doseq [[di dj] [[1 0] [1 -1] [0 -1] [-1 -1]
                            [-1 0] [-1 1] [0 1] [1 1]]]
             (let [i2 (+ i di)
                   j2 (+ j dj)]

               ;; verify the node is on the matrix, and has not yet been seen by
               ;; the path this node has followed.
               (when (and (>= i2 0) (< i2 cols)
                          (>= j2 0) (< j2 cols)
                          (not (path-set [i2 j2])))
                 (let [s2 (str s (get-letter i2 j2))
                       node [i2 j2 s2 (conj path-set [i2 j2])]]

                   ;; when a word is found, add it to the list (a set ensures no
                   ;; duplicate words are added)
                   (when (word? s2)
                     (conj! words s2))

                   ;; when the node passes the 'pred' function test, add it to
                   ;; the queue to be inspected later.
                   (when (pred node)
                     (vswap! queue conj node)))))))
         (recur)))

     ;; return a persistent data structure (i.e. the immutable data structure we
     ;; are used to in Clojure) of the words discovered.
     (persistent! words))))
