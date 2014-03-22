(ns tma-admin.secretary
  (:require
   [clojure.pprint :as pp]
   [tma-admin.queries :as q]
   [tma-admin.dates :as tyme]
   [clojure.core.incubator :as incubator]
   [lamina.core :refer [channel enqueue read-channel]]
   [dire.core :refer [with-postcondition with-handler supervise]]))


;; 2013-12-10 - the secretary files documents away, and then later gets them back again.
;; Nobody knows if the documents are in database or the cache, except the secretary.


(defn persist-item [item]
  {:pre [
         (map? item)
         (= (type (:item-type item)) java.lang.String)
         (= (type (:item-name item)) java.lang.String)
         ]}
  (q/persist-item item))

(defn remove-item [item]
  {:pre [
         (map? item)
         (= (type (:item-type item)) java.lang.String)
         (= (type (:item-name item)) java.lang.String)
         ]}
  (q/remove-item item))

(defn get-distinct []
  {:post [(= (type %) clojure.lang.LazySeq)]}
  (q/get-distinct)) 

(defn get-user-info-for-search []
  (q/get-user-info-for-search))

(defn get-count [request]
  {:pre [
         (map? request)
         ]
   :post [
          (= (type %) java.lang.Long)
          (or (pos? %) (zero? %))
          ]}
  (q/get-count request))




(def ^:private interactions (atom {}))

(defn get-from-interactions [vector-of-locators]
  {:pre [(= (type vector-of-locators) clojure.lang.PersistentVector)]}
  (get-in @interactions vector-of-locators))

(defn add-to-interactions [vector-of-locators value-to-be-added]
  {:pre [(= (type vector-of-locators) clojure.lang.PersistentVector)]}
  (swap! interactions (fn [map-of-interactions]
                        (assoc-in map-of-interactions vector-of-locators value-to-be-added))))

(defn delete-from-interactions [vector-of-locators]
  {:pre [(= (type vector-of-locators) clojure.lang.PersistentVector)]}
  (swap! interactions (fn [map-of-interactions]
                        (incubator/dissoc-in map-of-interactions vector-of-locators))))






(def ^:private cache-channel (channel))

(defn- add-to-cache-channel [vector-of-locators & args]
  (let [time-to-sleep (if (first args)
                        (first args)
                        3600000)]
  (enqueue cache-channel (fn []
                           (do
                             (. java.lang.Thread sleep time-to-sleep)
                             (println (tyme/current-time-as-string) " We have slept for  " (str time-to-sleep) " miliseconds, so we will now delete from cache the item at this key: ")
                             (pp/pprint vector-of-locators)
                             (delete-from-interactions vector-of-locators))))))

(defn- start-cache-worker []
  (loop [closure-with-vector-of-locators @(read-channel cache-channel)]
    (closure-with-vector-of-locators)
    (recur @(read-channel cache-channel))))

(defn start-channels []
  (dotimes [_ 6]
    (future (q/start-mongo-worker)))
  (dotimes [_ 6]
    (future (start-cache-worker))))




(defn fetch [function-to-call request]
  {:pre [
         (map? request)
         (= (type function-to-call) java.lang.String)
         ]}
  "2014-02-05 - the function get-current-item is somewhat unique in that we need to invaliate the cache quickly, after 10 seconds, whereas all other items can remain in the cache for a long time."
  (let [fetched-data (supervise #'q/process-some-fetch-function function-to-call request)]
    (add-to-interactions [:current (:uri request) function-to-call] fetched-data)
    (add-to-cache-channel [:current (:uri request) function-to-call])
    (add-to-cache-channel [:current (:uri request) "get-current-item"] 10000)))

(defn get-old-versions [item-name]
  (q/get-old-versions item-name))


(with-handler #'q/process-some-fetch-function
  "What if something throws an exception while this chain of functions tries to get data?"
  java.lang.NullPointerException
  (fn [e & args] (do  (println "q/process-some-fetch-function has raised a java.lang.NullPointerException.")
                      (println (str e))
                      (println "This is the query function that was being called: \"" (str (first args)) "\"")
                      (println "These were the arguments: ")
                      (doseq [x (rest args)]
                        (do  (println " one of the arguments: ")
                             (println (str x))))
                      (str  "An error has occurred. The programmer may have forgotten to define a function for the CSS ID being called."))))
