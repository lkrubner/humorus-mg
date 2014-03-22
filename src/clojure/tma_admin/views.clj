(ns tma-admin.views
  (:import
   [java.io FileNotFoundException])
  (:require
   [tma-admin.controller :as controller]
   [clojure.pprint :as pp]
   [clojure.string :as st]
   [net.cgrand.enlive-html :as enlive]
   [dire.core :refer [with-handler supervise with-post-hook]])
  (:use
   [tma-admin.tma-programmer-designer-contract]))


(defn login [request]
  {:pre [(map? request)]
   :post [(= (type %) java.lang.String)]}
  "2013-03-01 - the default name of the function expected by the Friend authorization library."
  (apply str (enlive/emit* (enlive/html-resource "templates/login.html"))))

(defn call-programmer-designer-contract-function [function-name-as-string template request]
  {:pre [
         (= (type function-name-as-string) java.lang.String)
         (vector? template)
         (map? request)
         ]
   :post [(= (type %) clojure.lang.PersistentVector)]}
  (let [function-name-as-symbol (symbol function-name-as-string)
        event (ns-resolve 'tma-admin.tma-programmer-designer-contract function-name-as-symbol)]
    (event template request)))

(defn process-programmer-designer-contract [seq-of-css-ids template request]
  {:pre [
         (= (type seq-of-css-ids) clojure.lang.LazySeq)
         (vector? template)
         (map? request)
         ]
   :post [(= (type %) clojure.lang.PersistentVector)]}
  "2013-09-05 - we are given a sequence of CSS ids which should match the names of functions in this namespace, so we call these ids as functions."  
  (loop [seq-of-function-names seq-of-css-ids
         updated-template template]
    (if (first seq-of-function-names) 
      (recur (rest seq-of-function-names)
             (supervise #'call-programmer-designer-contract-function (first seq-of-function-names) updated-template request))
      updated-template)))

(defn get-ids-out-of-enlive-nodes [nodes]
  {:pre [(= (type nodes) clojure.lang.LazySeq)]
   :post [(= (type %) clojure.lang.LazySeq)]}
  (flatten (map (fn [node] (merge [] (get-in node [:attrs :id]))) nodes)))

(defn get-html-file-as-seq-of-nodes [which-template]
  {:pre [(= (type which-template) java.lang.String)]
   :post [(= (type %) clojure.lang.PersistentVector)]}
  "2013-12-22 - I had the problem that html-resource returned clojure.lang.LazySeq but transform returns clojure.lang.PersistentVector. That caused failure after the first function in tma-programmer-designer-contract was called, because the type changed, and thus the functions could not dependably count on one type or another. So I am running this through a pointless transform in the hope of getting a type that will remain stable through all of the functions in tma-programmer-designer-contract. Also, I asked about this online and got interesting feedback here: https://groups.google.com/forum/#!topic/enlive-clj/7rT-W7V3kO8"
  (enlive/transform (enlive/html-resource (str "templates/" (str which-template))) [:.logo-text] (enlive/content (str "The TailorMadeAnswer Admin"))))

(defn render-page [request which-template & args]
  {:pre [
         (map? request)
         (= (type which-template) java.lang.String)
         (if (first args)
           (= (type (first args)) java.lang.String)
           true)
         ]
   :post [(= (type %) java.lang.String)]}
  "2013-09-05 - if an HTML element has the CSS class of  tma-programmer-designer-contract then we want to get its id and call its id as a function, which should be in this namespace.  2013-12-14 - if args is set, then it is a message that should be sent to the user"
  (let [template (get-html-file-as-seq-of-nodes which-template)
        template (if (first args)
                   (enlive/transform template [:#messages] (enlive/content (first args)))
                   template)
        nodes (enlive/select template [:.tma-programmer-designer-contract])
        seq-of-css-ids (get-ids-out-of-enlive-nodes nodes)
        template (process-programmer-designer-contract seq-of-css-ids template request)]
    (apply str (enlive/emit* template))))

(defn home [request & args]
  {:pre [(map? request)]
   :post [(= (type %) java.lang.String)]}
  "2014-02-04 - we call (first) on args just so we can pass it to render-page without the message getting wrapped in multiple seqs."
  (if (get-in request [:params :item-type])
    (render-page request "edit_items.html" (first args))
    (render-page (assoc-in request [:params :item-type] "users") "list_items.html")))

(defn process-input [request]
  {:pre [(map? request)]
   :post [(= (type %) java.lang.String)]}
  (home request (supervise #'controller/process-input request)))

(defn user-search [request]
  {:pre [(map? request)]
   :post [(= (type %) java.lang.String)]}
    (render-page request "show_users.html"))


(defn- transform-error-messages-to-single-string [map-of-problems]
  {:pre [(map? map-of-problems)
         (map? (first (:problems map-of-problems)))]}
  (loop [this-message (first (:problems map-of-problems))
         remaining-problems (rest (:problems map-of-problems))
         single-string-with-all-messages ""]
    (if (first remaining-problems)
      (recur
       (first remaining-problems)
       (rest remaining-problems)
       (str single-string-with-all-messages " For the field " (str (first (:keys (first remaining-problems)))) " there was this error: "(:msg (first remaining-problems))))
      single-string-with-all-messages)))

(with-handler #'controller/process-input
  "2014-02-25 - When we try to validate input via Formative, and there is a problem, Formative throws an exception full of errors. This: 

    (pp/pprint (ex-data e))

gives us:

{:problems
 ({:keys (:subscribe-to-questions-via-email), :msg \"not an accepted value\"}
  {:keys (:clients-can-contact), :msg \"not an accepted value\"}
  {:keys (:subscribe-to-all-discourse), :msg \"not an accepted value\"})}
"
  clojure.lang.ExceptionInfo
  (fn [e & args]
    (transform-error-messages-to-single-string (ex-data e))))

(with-handler #'call-programmer-designer-contract-function
  "What if the CSS ID in the HTML fails to match a function that's been defined? "
  java.lang.NullPointerException
  (fn [e & args] (do  (println "You may have forgotten to define a function for the CSS ID being called, or perhaps the item-type or item-name is not in the request map and you are calling a function that expects that..")
                      (println (str e))
                      (println "This is the function that was being called, be sure it exists in tma-programmer-designer-contract: \"" (str (first args)) "\"")
                      (println "These were the arguments: ")
                      (doseq [x (rest args)]
                        (do  (println "One of the arguments is: ")
                             (println (str x))))
                      (str  "An error has occurred. The programmer may have forgotten to define a function for the CSS ID being called."))))













