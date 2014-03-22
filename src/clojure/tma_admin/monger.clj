(ns tma-admin.monger
  (:import [com.mongodb MongoOptions ServerAddress]
           [org.bson.types ObjectId]
           [org.joda.time.DateTimeZone])
  (:require
   [tma-admin.dates :as tyme]
   [me.raynes.fs :as fs]
   [clojure.pprint :as pp]
   [monger.core :as mg]
   [monger.collection :as mc]
   [monger.conversion :as convert]
   [monger.operators :as operators]
   [monger.joda-time])
  (:refer-clojure :exclude [sort find])
  (:use
   [taoensso.timbre :as timbre :only (trace debug info warn error fatal spy)]
   [clojure.java.io :only [as-file input-stream output-stream] :as io]
   [monger.query]))



(defn establish-database-connection []
  (let [credentials (read-string (slurp (clojure.java.io/resource "config/credentials.edn")))                    
        uri (str "mongodb://" (:username credentials) ":" (:password credentials) "@tailormadeanswers.com/tma")]
    (println uri)
    (mg/connect-via-uri! uri)))

(defn remove-this-item [item]
  {:pre [
         (= (type item) clojure.lang.PersistentHashMap)
         (= (type (:item-name item)) java.lang.String)
         (= (type (:item-type item)) java.lang.String)
         ]}
  (mc/remove  "tma" { :item-type (:item-type item) :item-name (:item-name item) }))

(defn persist-this-item [item]
  {:pre [
         (map? item)
         (= (type (:item-name item)) java.lang.String)
         (= (type (:item-type item)) java.lang.String)
         (if (:created-at item)
           (= (type (:created-at item)) org.joda.time.DateTime)
           true)
         ]}
  (let [item (if (nil? (:created-at item))
               (assoc item :created-at (tyme/current-time-as-datetime))
               item)
        item (assoc item :updated-at (tyme/current-time-as-datetime))
        item (assoc item :_id (ObjectId.))]    
    (mc/insert "tma" item)))
    
(defn find-these-items [where-clause-map & args]
  {:pre [
         (map? where-clause-map)
         (if (first args)
           (= (type (first args)) clojure.lang.PersistentVector)
           true)
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  "2013-12-11 - if there is something in args, we assume it is the vector of fields which limits which fields are returned."
  (if (first args)
    (mc/find-maps "tma" where-clause-map (first args))
    (mc/find-maps "tma" where-clause-map)))

(defn find-this-item [item-name]
  {:pre [
         (= (type item-name) java.lang.String)
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  (with-collection "tma"
    (find { :item-name item-name })
    (sort (array-map :item-name 1 :updated-at -1))
    (limit 1)))

(defn get-distinct [field-name]
  {:pre [(= (type field-name) clojure.lang.Keyword)]
   :post [(= (type %) clojure.lang.LazySeq)]}
  ;; convert values to maps using an anonymous function
  (map
   (fn [field-value]
     (convert/from-db-object field-value true))
   (mc/distinct "tma" field-name)))

(defn get-count [item-type]
  {:pre [
         (= (type item-type) java.lang.String)
         ]
   :post [
          (= (type %) java.lang.Long)
          (or (pos? %) (zero? %))
          ]}
  (mc/count "tma" { :item-type item-type}))

(defn get-parent-items [item]
  {:pre [
         (map? item)
         (= (type (:item-name item)) java.lang.String)
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  "2013-12-18 - we want to make sure that this 'item' being given as an argument is a record in the database, so
we test for :item-name which has to be a String."
  (with-collection "tma"
    (find { operators/$or [
                           { :item-name (:answer-item-name item) }
                           { :item-name (:question-item-name item) }
                           { :item-name (:discourse-item-name item) }
                           { :item-name (:user-item-name item) }
                           { :item-name (:recommendation-item-name item) }
                           { :item-name (:in-response-to-item-name item) }
                           { :item-name (:previous-item-name item) }
                           { :item-name (:community-item-name item) }
                           { :item-name (:donation-item-name item) }
                           { :item-name (:recipient-user-item-name item) }]
           })
    (sort (array-map :item-name 1 :updated-at 1))))

(defn get-children-items [item]
  {:pre [
         (map? item)
         (= (type (:item-name item)) java.lang.String)
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  "2013-12-18 - we want to make sure that this 'item' being given as an argument is a record in the database, so
we test for :item-name which has to be a String."
  (with-collection "tma"
    (find { operators/$or [
                           { :answer-item-name (:item-name item) }
                           { :question-item-name (:item-name item) }
                           { :discourse-item-name (:item-name item) }
                           { :user-item-name (:item-name item) }
                           { :recommendation-item-name (:item-name item) }
                           { :in-response-to-item-name (:item-name item) }
                           { :previous-item-name (:item-name item) }
                           { :community-item-name (:item-name item) }
                           { :donation-item-name (:item-name item) }
                           { :recipient-user-item-name (:item-name item) }
                           ]
           })
    (sort (array-map :item-name 1 :updated-at 1))))

(defn paginate-results [item-type which-page]
  {:pre [
         (= (type item-type) java.lang.String)
         (= (type which-page) java.lang.String)
         (or (pos? (Integer/parseInt which-page)) (zero? (Integer/parseInt which-page)))
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  (with-collection "tma"
    (find { :item-type item-type})
;    (fields [:item-name :item-type :user-item-name :created-at])
    (sort (array-map :created-at 1 :user-item-name 1))
    (limit 200)
    (skip (Integer/parseInt which-page))))









