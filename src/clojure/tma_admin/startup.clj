(ns tma-admin.startup
  (:require [tma-admin.secretary :as secretary]
            [tma-admin.monger :as monger]
            [clojure.pprint :as pp])
  (:use [taoensso.timbre :as timbre :only (trace debug info warn error fatal spy)]))


(defn connect-to-database []
  (try
    (monger/establish-database-connection)
    (catch Exception e (println e))))

(defn set-the-current-debugging-level [& args]
  "2013-03-01 - called when the app starts. level-of-debugging should be passed in as a command line argument. If set to 'production', then all debugging and output to the terminal is surpressed. Otherwise, we are in debugging mode, and a lot gets printed to the terminal, mostly via timbre."
  (let [level-of-debugging (or (first args) "debug")]
    (if (= level-of-debugging "production")
      (timbre/set-level! :error)
      (timbre/set-level! :debug))))

(defn- copy-each-default-field-to-the-forms-fields-vector [forms-fields default-fields]
  (reduce
   (fn [vector-of-all-fields next-field-map]
     (conj vector-of-all-fields next-field-map))
   forms-fields
   default-fields))

(defn- fill-forms [forms vector-of-fields-for-all-records]
  {:pre [
         (= (type vector-of-fields-for-all-records) clojure.lang.PersistentVector)
         (= (type forms) clojure.lang.APersistentMap$ValSeq)
         ]
   :post [(= (type %) clojure.lang.PersistentHashMap)]}
  (loop [one-form (first forms)
         remaining-forms (rest forms)
         forms-with-all-fields {}]
    (if (first remaining-forms)
      (recur
       (first remaining-forms)
       (rest remaining-forms)
       (assoc forms-with-all-fields (get-in one-form [:values :item-type]) (assoc one-form :fields (copy-each-default-field-to-the-forms-fields-vector (:fields one-form) vector-of-fields-for-all-records))))
      forms-with-all-fields)))

(defn initiate-forms []
  "2013-09-29 - we have some common fields that we want all forms to have (for instance 'created-at' and 'updated-at'). For the sake of brevity, we define these fields only once in the schema config. At startup, we need to mix this fields in with all of the forms."
  (let [map-with-keys-and-forms (read-string (slurp (clojure.java.io/resource "config/schema.edn")))
        vector-of-fields-for-all-records (:fields-for-all-records map-with-keys-and-forms)
        map-with-keys-and-forms (dissoc map-with-keys-and-forms :fields-for-all-records)
        all-forms (fill-forms (vals map-with-keys-and-forms) vector-of-fields-for-all-records)]
    (secretary/add-to-interactions [:schema] all-forms)))

(defn start-channels []
  (secretary/start-channels))

