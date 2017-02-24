(ns word-trek-solver.core
  (:require [clojure.core.async :refer [<!! >!! thread chan close!]]
            [clojure.string :as st]

            [clojure.java.io :as io]

            [loom.graph :as g]
            [loom.alg :as galg]))

;;; Declarations

(declare power-of-two?
         paths-memoized
         paths->words
         word?)

;;; API

(defn solve
  [letters word-sizes]
  (let [len (count letters)
        max-length (apply max word-sizes)
        size (and (power-of-two? len) (int (Math/sqrt len)))
        workers 4]
    (assert (< max-length 10) "Disable this assertion for higher order computation.")
    (assert size "Input string length must be power of two.")
    (let [valid-size? (set word-sizes)
          letter-grid (mapv vec (partition size letters))
          letter-array (char-array letters)
          letter-fn (fn [[i j]]
                      (aget ^chars letter-array
                            (+ (* j size) i)))]
      (->> (paths-memoized max-length size)
           (paths->words letter-fn workers word-sizes)
           (filter word?)
           (sort-by count)))))

;;; Private

(def words (set (st/split (slurp (io/resource "words_100000.txt")) #"\n")))
(def word? (comp boolean words))

(defn power-of-two?
  [x]
  (and (integer? x)
       (not= 0 x)
       (zero? (bit-and x (dec x)))))

(defn graph
  [size]
  (->> (for [i (range size)
             j (range size)]
         [[[i j] [i (dec j)]]
          [[i j] [i (inc j)]]

          [[i j] [(inc i) j]]
          [[i j] [(dec i) j]]

          [[i j] [(inc i) (inc j)]]
          [[i j] [(inc i) (dec j)]]

          [[i j] [(dec i) (inc j)]]
          [[i j] [(dec i) (dec j)]]])
       (apply concat)
       (filter (fn [[[i1 j1] [i2 j2]]]
                 (and (<= 0 i1 (dec size))
                      (<= 0 j1 (dec size))
                      (<= 0 i2 (dec size))
                      (<= 0 j2 (dec size)))))
       (map set)
       distinct
       (map vec)
       (apply g/graph)))

(defn paths
  ([max-length size]
   (let [graph (graph size)]
     (mapcat (fn [node]
               (paths max-length graph [node]))
             (:nodeset graph))))
  ([max-length graph path]
   (or (when (<= (count path) max-length)
         (let [node (last path)]
           (when-let [neighbors (not-empty (get-in graph [:adj node]))]
             (let [graph (g/remove-nodes graph node)]
               (mapcat (fn [nb] (paths max-length graph (conj path nb))) neighbors)))))
       [path])))

(def paths-memoized (memoize paths))

(defn path->word
  [letter-fn path]
  (apply str (map letter-fn path)))

(defn paths->words
  [letter-fn workers word-sizes paths]
  (let [group-size (int (/ (count paths) workers))]
    (doall
     (->> paths
          (partition group-size group-size [])
          (mapv (fn [path-group]
                  (thread
                    (try
                      (let [result (transient #{})]
                        (doseq [path path-group]
                          (let [word (path->word letter-fn path)]
                            (doseq [word-size word-sizes]
                              (when (<= word-size (count word))
                                (let [word (.substring word 0 word-size)]
                                  (conj! result word))))))
                        (persistent! result))
                      (catch Exception e
                        (println e))))))
          (mapv (fn [ch] (<!! ch)))
          (apply concat)
          distinct))))
