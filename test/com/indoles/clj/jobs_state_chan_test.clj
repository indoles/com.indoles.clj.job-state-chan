(ns com.indoles.clj.jobs-state-chan-test
  (:require [clojure.test :refer :all]
            [com.indoles.clj.jobs-state-chan :refer :all]))

(defn- sleep-job [s]
  (fn [] (Thread/sleep s)))

(deftest a-test
  (let [ch (init)]
    (dotimes [i 5]
      (send-queue-job (sleep-job 5000) ch)
      (Thread/sleep 1000))
    (send-try-dequeue-job 5 ch)
    (testing "Try dequeue job."
      (is (= 3 (count (:queued (state ch))))))))

(defn- odd-alone-even-all [executing queued]
  (if (and (empty? executing)
           (first queued)
           (odd? (:id (first queued))))
    (one-at-a-time executing queued)
    (vec (filter #(even? (:id %1)) queued))))

(deftest b-test
  (let [ch (init odd-alone-even-all)]
    (dotimes [i 10]
      (send-queue-job (sleep-job 5000) ch)
      (Thread/sleep 1000))
    (testing "Only even jobs left."
      (is (= (count (:queued (state ch))) 4)))))

