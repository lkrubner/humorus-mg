(ns tma-admin.controller
  (:import
   (java.util UUID)
   (javax.imageio ImageIO))
  (:require
   [tma-admin.item-name :as in]
   [tma-admin.make-images :as mi]
   [tma-admin.secretary :as secretary]
   [tma-admin.dates :as tyme]
   [me.raynes.fs :as fs]
   [formative.core :as f]
   [formative.parse :as fp]
   [formative.render :refer [render-form render-field render-problems render-default-input]]
   [hiccup.core :as hiccup]
   [dire.core :refer [with-postcondition with-handler supervise]]
   [clojure.string :as st]
   [clojure.pprint :as pp]
   [clojure.java.io :as io])
  (:use
   [tma-admin.ad-hoc-form-fields]))



;; 2013-12-13 - this namespace should be the only namespace where secretary and formative meet.
;; tma-admin.views calls this namespace to get data and forms, but tma-admin.views never sees secretary
;; or formative or monger -- this namespace masks all of that from tma-admin.views. 


(defn- get-map-of-properties-for-this-one-field [item-type-as-string this-one-field-as-keyword]
  (first 
   (reduce
    (fn [vector-with-map-of-properties next-field]
      (if (= (:name next-field) this-one-field-as-keyword)
        (conj vector-with-map-of-properties next-field)
        vector-with-map-of-properties))
    []
    (secretary/get-from-interactions [:schema (keyword item-type-as-string) :fields]))))

(defn is-this-field-important [item-type-as-string this-one-field-as-keyword]
  (:important-to-show-in-lists (get-map-of-properties-for-this-one-field item-type-as-string this-one-field-as-keyword)))

(defn transform-keyword-into-string [some-keyword]
  {:pre [(= (type some-keyword) clojure.lang.Keyword)]
   :post [(= (type %) java.lang.String)]}
  (let [some-keyword (st/replace (str some-keyword) #":" "")
        some-keyword (st/replace (str some-keyword) #"-" " ")]
      some-keyword))

(defn transform-keyword-into-string-with-hyphens [some-keyword]
  {:pre [(= (type some-keyword) clojure.lang.Keyword)]
   :post [(= (type %) java.lang.String)]}
  (let [some-keyword (st/replace (str some-keyword) #":" "")]
      some-keyword))

(defn get-schema-types []
  {:post [(vector? %)
          (= (type (first %)) java.lang.String)]}
  "2014-02-05 - this is called in tma-bottom-links-list and tma-bottom-links-edit in tma-programmer-designer-contract, to list all the different types of data in the system."
  (reduce
   (fn [vector-of-types-as-strings next-type]
     (conj vector-of-types-as-strings (transform-keyword-into-string-with-hyphens next-type)))
   []
   (keys (secretary/get-from-interactions [:schema]))))

(defn- user-info []
  (secretary/get-from-interactions [:current :user-cache]))

(defn get-user-info-for-search []
  (supervise #'user-info))

(defn- got [name-of-function-as-a-string & args]
  "2014-02-05 - if there are args, that is usually the result map."
  (let [request (first args)
        cached-data (secretary/get-from-interactions [:current (str (:uri request)) name-of-function-as-a-string])]
    (if (nil? cached-data)
      (do
        (supervise #'secretary/fetch name-of-function-as-a-string (first args))
        (secretary/get-from-interactions [:current (str (:uri request)) name-of-function-as-a-string]))
      cached-data)))

(defn fetch [name-of-function-as-a-string & args]
  {:pre [
         (= (type name-of-function-as-a-string) java.lang.String)
         (map? (first args))
         ]
   :post [(vector? %)]}
  (supervise #'got name-of-function-as-a-string (first args)))



(defn get-old-versions [item-name]
  (secretary/get-old-versions item-name))




(defn- build-vector-of-values-for-form [default-values item-values]
  (reduce
   (fn [vector-of-all-values [k v]]
     (conj vector-of-all-values { k (cond
                                     (= v "t") true
                                     (= v "1") true
                                     (= v "f") false
                                     (= v "0") false
                                     :else v) }))
   default-values
   item-values))

(defn- add-values-to-form [request]
  {:pre [
         (map? request)
         (= (type (get-in request [:params :item-type])) java.lang.String)
         (if (get-in request [:params :item-name])
           (= (type (get-in request [:params :item-name])) java.lang.String)
           true)
         ]
   :post [
          (map? %)
          (vector? (:fields %))
          (vector? (:validations %))
          (= (type (:action %)) java.lang.String)
          ]}
  "2013-12-26 - item-name is optional, it might be nil, or it might be an actual item-name"
  (let [item-name (get-in request [:params :item-name])
        item-type-as-keyword (keyword (get-in request [:params :item-type]))
        the-form (secretary/get-from-interactions [:schema item-type-as-keyword])]
    (if item-name
      (assoc the-form :values (build-vector-of-values-for-form (:values the-form) (first (fetch "get-current-item" request))))
      the-form)))

(defn get-form [request]
  {:pre [
         (map? request)
         (= (type (get-in request [:params :item-type])) java.lang.String)
         ]
   :post [(= (type %) java.lang.String)]}
  "2014-02-05 - we must invalidate the cache, so the user will see that the values have been updated when they save their changes."
  (hiccup/html (f/render-form (add-values-to-form request))))

(defn process-input [request]
  {:pre [
         (map? request)
         (= (type (get-in request [:params :community-item-name])) java.lang.String)
         (= (type (get-in request [:params :item-type])) java.lang.String)
         ]
   :post [(= (type %) java.lang.String)]}
  (let [item-type-as-keyword (keyword (get-in request [:params :item-type]))
        the-form (secretary/get-from-interactions [:schema item-type-as-keyword])
        item (:params request)
        item (if (:item-name item)
               item
               (assoc item :item-name (in/make-item-name item)))
        created-at (tyme/make-created-at item)
        updated-at (tyme/make-updated-at item)
        start-at (tyme/make-start-at item)
        end-at (tyme/make-end-at item)
        item (fp/parse-params the-form item)
        item (if created-at
               (assoc item :created-at created-at)
               item)
        item (if updated-at
               (assoc item :updated-at updated-at)
               item)
        item (if start-at
               (assoc item :start-at start-at)
               item)
        item (if end-at
               (assoc item :end-at end-at)
               item)
        ]
    (if (:to-be-deleted item)
      (do 
        (secretary/remove-item item)
        "Your item has been deleted.")
      (do 
        (mi/make-file-document-if-a-file-has-been-uploaded item)
        (let [
              item (dissoc item :file-1)
              item (dissoc item :file-2)
              item (dissoc item :file-3)
              ]
          (secretary/persist-item item)
          "Your item has been saved.")))))










(with-handler #'secretary/fetch
  "What if something throws an exception while this chain of functions tries to get data?"
  java.lang.NullPointerException
  (fn [e & args] (do
                   (println "secretary/fetch has no return value, but it can raise a java.lang.NullPointerException? If a function was called that does not exist?")
                   (println (str e))
                   (println "This is the function that was being called: \"" (str (first args)) "\"")
                   (println "These were the arguments: ")
                   (doseq [x (rest args)]
                     (do  (println " one of the arguments: ")
                          (println (str x)))))))

(with-postcondition #'user-info
  :user-info-is-not-empty
  (fn [return-value]
    (> (count return-value) 0)))

(with-handler #'user-info
  {:postcondition :user-info-is-not-empty}
  "2014-02-04 - if nothing comes back from get-user-info-for-search, then we want to see what is in the [:current] cache, to help diagnose why we were unable to get the data we wanted."
  (fn [e result]
    (println "Postcondition failed for get-user-info-for-search.  Result was: ")
    (pp/pprint result)
    (println "The current contents of the :current cache in secretary/interactions is: ")
    (pp/pprint (secretary/get-from-interactions [:current]))))

(with-postcondition #'secretary/fetch
  :secretary-fetch-must-return-a-vector
  (fn [return-value]
    (vector? return-value)))

(with-handler #'secretary/fetch
  {:postcondition :secretary-fetch-must-return-a-vector}
  (fn [e result]
    (vector result)))

;; (with-postcondition #'got
;;   :got-returns-a-vector
;;   (fn [return-value]
;;     (vector? return-value)))

;; (with-handler #'got
;;   {:postcondition :got-returns-a-vector}
;;   "Sometimes we get nothing back, but we should always be returning a vector. If there is no vector, then why?"
;;   (fn [e result]
;;     (println "This exception was raised by controller/got:")
;;     (pp/pprint e)
;;     (println "This was the return value from controller/got: ")
;;     (pp/pprint result)
;;     (vector result)))


