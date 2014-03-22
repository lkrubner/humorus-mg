(ns tma-admin.queries
  (:require
   [clojure.pprint :as pp]
   [clojure.stacktrace :as stack]
   [tma-admin.monger :as monger]
   [lamina.core :refer [channel enqueue read-channel]]
   [dire.core :refer [with-postcondition with-handler supervise]]))




          
;; 2013-12-26 - This is the only namespace that should have access to monger.





(defn print-error-info [e]
  "2012-12-10 - I originally put this in core.clj, but it is really just a utility function, so I will put it here."
  (prn "Exception in the main function: " e)
  (prn (apply str "The print-cause-trace with chained exceptions is:" (stack/print-cause-trace e)))
  (prn (apply str "The print-stack-trace standard clojure is:" (stack/print-stack-trace e))))





(def ^:private mongo-channel (channel))

(defn- add-to-mongo-channel [action item]
  {:pre [
         (or (= action "persist")  (= action "remove"))
         (map? item)
         (= (type (:item-type item)) java.lang.String)
         (= (type (:item-name item)) java.lang.String)
         ]}
  (enqueue mongo-channel
           (if (= action "remove")
             (fn [] (monger/remove-this-item item))
             (fn [] (try
                      (monger/persist-this-item item)
                      (catch Exception e (print-error-info e)))))))

(defn start-mongo-worker []
  (loop [closure-with-item-inside @(read-channel mongo-channel)]
    (closure-with-item-inside)
    (recur @(read-channel mongo-channel))))

(defn persist-item [item]
  {:pre [
         (map? item)
         (= (type (:item-type item)) java.lang.String)
         (= (type (:item-name item)) java.lang.String)
         ]}
  (add-to-mongo-channel "persist" item))

(defn remove-item [item]
  {:pre [
         (map? item)
         (= (type (:item-type item)) java.lang.String)
         (= (type (:item-name item)) java.lang.String)
         ]}
  (add-to-mongo-channel "remove" item))








(defn get-distinct []
  {:post [(= (type %) clojure.lang.LazySeq)]}
  (monger/get-distinct :item-type)) 

(defn get-count [request]
  {:pre [
         (map? request)
         (= (type (get-in request [:params :item-type])) java.lang.String)
         ]
   :post [
          (= (type %) java.lang.Long)
          (or (pos? %) (zero? %))
          ]}
  (monger/get-count (get-in request [:params :item-type]))) 

(defn get-user-info-for-search []
  (monger/find-these-items { :item-type "users" } [:item-name :username :email :first-name :last-name]))

(defn- paginate-results [request]
  {:pre [         
         (map? request)
         (= (type (get-in request [:params :item-type])) java.lang.String)
         (if (get-in request [:params :which-page])
           (pos? (Integer/parseInt (get-in request [:params :which-page])))
           true)
         ]}
  (supervise #'monger/paginate-results (get-in request [:params :item-type]) (if (get-in request [:params :which-page])
                                                                               (get-in request [:params :which-page])
                                                                               "0")))

(defn- get-current-item [request]
  {:pre [
         (map? request)
         (get-in request [:params :item-name])
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  (monger/find-this-item (get-in request [:params :item-name])))

(defn- get-children-items [request]
  {:pre [
         (map? request)
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  (monger/get-children-items (first (get-current-item request)))) 
                             
(defn- get-parent-items [request]
  {:pre [
         (map? request)
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  (monger/get-parent-items (first (get-current-item request)))) 

(defn- execute-some-fetch-function [function-to-call-to-get-data-from-monger request]
  {:pre [
         (= (type function-to-call-to-get-data-from-monger) java.lang.String)
         (map? request)
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  (let [function-name-as-symbol (symbol function-to-call-to-get-data-from-monger)
        function-name-as-function (ns-resolve 'tma-admin.queries function-name-as-symbol)
        fetched-data (function-name-as-function request)]
    (if (= (type fetched-data) clojure.lang.LazySeq)
      fetched-data
      (lazy-seq (vector fetched-data)))))

(defn- sorting-fetched-data-to-get-unique-item-names [function-to-call-to-get-data-from-monger request]
  (reduce
   (fn [map-of-unique-item-name next-item]
     (assoc map-of-unique-item-name (:item-name next-item) next-item))
   {}
   (supervise #'execute-some-fetch-function function-to-call-to-get-data-from-monger request)))

(defn- some-fetch-function [function-to-call-to-get-data-from-monger request] 
  "2013-12-21 - We enforce a format of using the item name as a string key that points to the map. We need to realize the lazyseq we get back from execute-some-fetch-function"
  (reduce
   (fn [vector-of-items [k v]]
     (conj vector-of-items v))
   []
   (sorting-fetched-data-to-get-unique-item-names function-to-call-to-get-data-from-monger request)))

(defn process-some-fetch-function [function-to-call-to-get-data-from-monger request] 
  {:pre [
         (= (type function-to-call-to-get-data-from-monger) java.lang.String)
         (map? request)
         ]
   :post [(vector? %)]}
  (let [fetched-data (supervise #'some-fetch-function function-to-call-to-get-data-from-monger request)]
    (if fetched-data
      fetched-data
      [])))

(defn get-old-versions [item-name]
  {:pre [ (= (type item-name) java.lang.String)]
   :post [(vector? %)]}
  (reduce
   (fn [vector-of-items next-item]
     (conj vector-of-items next-item))
   []
   (monger/find-these-items { :item-name item-name } )))

  


(with-handler #'monger/paginate-results
  "What if monger throws an exception?"
  java.lang.NullPointerException
  (fn [e & args] (do  (println "monger/paginate-results has raised a java.lang.NullPointerException.")
                      (println (str e))
                      (println "This is the item-type: \"" (str (first args)) "\"")
                      (println "This is the which-page number: \"" (str (second args)) "\"")
                      (println "These should only be 2 arguments: ")
                      (doseq [x args]
                        (do  (println " one of the arguments: ")
                             (println (str x)))))))
              



