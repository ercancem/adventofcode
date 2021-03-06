(ns advent2018.day16
  (:require [clojure.string :as s]))

(defn read-ints [s]
  (mapv read-string (re-seq #"[-]?\d+" s)))

(defn read-section1 [[a b c _]]
  (mapv read-ints [a b c]))

(defn immediate [x y] y)
(defn register  [x y] (nth x y))

(defn make-op [f g h]
  (fn [regs a b c]
    (update-in regs [c] (fn [_] (f (g regs a) (h regs b))))))

(defn make-regs [f]
  (make-op f register register))

(defn make-imm [f]
  (make-op f register immediate))

(defn make-test [f g h]
  (make-op (fn [a b] (if (f a b) 1 0)) g h))

(def opcodes ;; :key -> (fn [[r0 r1 r2 r3] a b c] -> [r0 r1 r2 r3])
  {
   :addr (make-regs +) :addi (make-imm +)
   :mulr (make-regs *) :muli (make-imm *)
   :banr (make-regs bit-and) :bani (make-imm bit-and)
   :borr (make-regs bit-or) :bori (make-imm bit-or)
   :setr (make-regs (fn [a _] a)) :seti (make-op (fn [a _] a) immediate immediate)
   :gtir (make-test > immediate register)
   :gtri (make-test > register immediate)
   :gtrr (make-test > register register)
   :eqir (make-test = immediate register)
   :eqri (make-test = register immediate)
   :eqrr (make-test = register register)
   })

(defn read-input []
  (let [lines    (s/split-lines (slurp "resources/day16.txt"))
        [s1 s2]  (->> lines
                      (partition-all 4)
                      (split-with #(not= ["" ""] (take 2 %))))
        section1 (mapv read-section1 s1)
        section2 (->> s2
                      (reduce concat)
                      (drop-while s/blank?)
                      (mapv read-ints))]
    [section1 section2]))

;; ((:mulr opcodes) [3 2 1 1] 2 1 2) => [3 2 2 1]
;; ((:addi opcodes) [3 2 1 1] 2 1 2) => [3 2 2 1]
;; ((:seti opcodes) [3 2 1 1] 2 1 2) => [3 2 2 1]

(def example-1 [[3 2 1 1] [9 2 1 2] [3 2 2 1]])

(defn good-sample-part1? [[regs-before [_ a b c] regs-after]]
  (let [fns (vals opcodes)]
    (->> (map #(apply % [regs-before a b c]) fns)
         (filter #{regs-after})
         count
         (< 2))))

;; (good-sample? example-1)

(defn part1 [] ;; [[regs-before opcode regs-after] ... ]
  (->> (filter good-sample-part1? (first (read-input)))
       count))

;; (part1)

;; part 2 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn by-opcode [n [_ [opcode _ _ _] _]]
  (= n opcode))

(defn good-sample? [f [regs-before [_ a b c] regs-after]]
  (= regs-after
     (apply f [regs-before a b c])))

;; Given a number, returns the key from opcodes that validates all samples including it

(defn opcode-name-by-number [n remaining-opcodes]
  (let [samples (filter (partial by-opcode n) (first (read-input)))]
    (loop [remaining-opcodes remaining-opcodes found []]
      (if (seq remaining-opcodes)
        (let [[name f] (first remaining-opcodes)
              filterfn (partial good-sample? f)
              matches  (->> samples (filter filterfn) count)]
          (if (= (count samples) matches)
            (recur (rest remaining-opcodes) (conj found name))
            (recur (rest remaining-opcodes) found)))
        found))))

;; This is a non-performing way to build the number->opcode map since some opcodes
;; satisfy all the samples (eg. :bori or :bani) so we start building with those
;; mappings that are exclusive (eg. 0 -> :bani) and removing opcodes and numbers
;; from the pool until all resolve. It takes longer but it works automatically.

(defn build-opcode-map []
  (loop [remaining-opcodes opcodes
         remaining-ids (into [] (range 16))
         kvs {}]
    (if (seq remaining-ids)
      (let [id        (first remaining-ids)
            ops-by-id (opcode-name-by-number id remaining-opcodes)]
        (if (= 1 (count ops-by-id))
          (let [opname (first ops-by-id)]
            (recur (dissoc remaining-opcodes opname) ;; opcode is used, spend it
                   (subvec remaining-ids 1)          ;; like rest, but still a vector
                   (assoc kvs id opname)))
          (recur remaining-opcodes
                 (conj (subvec remaining-ids 1) id) ;; move to the END for retrying
                 kvs)))
      kvs)))

;; (build-opcode-map)

(defn execute-instruction [opcodes-map regs [opid a b c]]
  (let [f (get opcodes (get opcodes-map opid))]
    (apply f [regs a b c])))

(defn part2 []
  (let [exec-instr   (partial execute-instruction (build-opcode-map))
        instructions (second (read-input))]
    (reduce exec-instr [0 0 0 0] instructions)))

;; (part2)
