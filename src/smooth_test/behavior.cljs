(ns smooth-test.behavior)

(defn other [] (+ 1 1))
(defn andanother [] 1)
(defn sample [] (* 5 (other)))

(defn mock [rv] (fn [&rest] rv))
