(ns com.indoles.clj.jobs-state-chan
  (:require [com.indoles.clj.state-chan :as sc]
            [clojure.core.async :as async]))

(defn init []
  (let [ch (sc/init {:executing [] :queued []})]
    (async/go (async/>! ch #(assoc %1 :ch ch)))
    ch))

(defn state [ch]
  (sc/state ch))

(defn stop [ch]
  (sc/stop ch))

(defn- next-job-id [e]
  (inc (reduce max 0 (map :id (flatten (conj (:executing e) (:queued e)))))))

(defn- make-job [f e]
  {:id (next-job-id e)
   :fn f})

(declare send-complete-execution)

(defn- try-to-execute [e]
  (if (empty? (:executing e))
    (if-let [i (first (:queued e))]
      (do
        (async/go ((:fn i))
                  (send-complete-execution (:id i) (:ch e)))
        (-> e (update-in [:executing] conj i)
            (update-in [:queued] #(vec (drop 1 %1)))))
      e)
    e))

(defn- queue-job [job e]
  (-> e (update-in [:queued] conj job)
      try-to-execute))

(defn- complete-execution [jid e]
  (let [executing (:executing e)
        new-executing (filter #(not (= jid (:id %1))) executing)]
    (try-to-execute (assoc-in e [:executing] (vec new-executing)))))

(defn- try-dequeue-job [jid e]
  (let [queued (:queued e)
        new-queued (filter #(not (= (:id %1) jid)) queued)]
    (assoc-in e [:queued] (vec new-queued))))

(defn send-queue-job [f ch]
  (async/go (async/>! ch (fn [e] (queue-job (make-job f e) e)))))

(defn send-complete-execution [id ch]
  (async/go (async/>! ch (fn [e] (complete-execution id e)))))

(defn send-try-dequeue-job [jid ch]
  (async/go (async/>! ch (fn [e] (try-dequeue-job jid e)))))

(defn- sleep-job [s n]
  (fn [] (Thread/sleep s) (println n)))

(defn- su []
  (def ch (init))
  (dotimes [i 5]
    (send-queue-job (sleep-job 5000 i) ch)
    (Thread/sleep 500))
  (send-try-dequeue-job 5 ch)
  (state ch))

