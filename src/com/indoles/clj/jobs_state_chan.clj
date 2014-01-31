(ns com.indoles.clj.jobs-state-chan
  (:require [com.indoles.clj.state-chan :as sc]
            [clojure.core.async :as async]))

;; return vector of jobs to execute, nil if none should be started
(defn one-at-a-time [executing queued]
  (when (and (empty? executing) (first queued))
    [(first queued)]))

(defn init
  ([startable-fn] (let [ch (sc/init {:executing [] :queued []})]
                    (async/go (async/>!
                               ch #(assoc %1 :ch ch :startable-fn startable-fn)))
                                        ; do a sync
                    (sc/state ch)
                    ch))
  ([] (init one-at-a-time)))

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

(defn- execute-job [job e]
  (async/go ((:fn job))
            (send-complete-execution (:id job) (:ch e))
            (-> e (update-in [:executing] conj job)
                (update-in [:queued] (vec (filter #(not (= (:id job) ))))))))

(defn- try-to-execute [e]
  (if-let [startables ( (:startable-fn e) (:executing e) (:queued e) )]
    (loop [i startables
           new-e e]
      (if-let [start-me (first i)]
        (let [executing (:executing new-e)
              queued (:queued new-e)
              new-executing (conj executing start-me)
              new-queued (vec (filter #(not (= (:id start-me)
                                               (:id %1)))
                                      queued))]
          (async/go ((:fn start-me))
                    (send-complete-execution (:id start-me) (:ch e)))
          (recur (vec (rest startables))
                 (-> new-e (assoc-in [:executing] new-executing)
                     (assoc-in [:queued] new-queued))))
        ;; started all we need to start
        new-e))
    ;; this means no new jobs were started
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
    (try-to-execute (assoc-in e [:queued] (vec new-queued)))))

(defn send-queue-job [f ch]
  (async/go (async/>! ch (fn [e] (queue-job (make-job f e) e)))))

(defn send-complete-execution [id ch]
  (async/go (async/>! ch (fn [e] (complete-execution id e)))))

(defn send-try-dequeue-job [jid ch]
  (async/go (async/>! ch (fn [e] (try-dequeue-job jid e)))))
