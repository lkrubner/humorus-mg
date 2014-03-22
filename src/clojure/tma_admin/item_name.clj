(ns tma-admin.item-name
  (:import
   (java.util UUID))
  (:require
   [clojure.string :as st]))

(defn make-item-name [item]
  {:pre [
         (map? item) 
         (= (type (:item-type item)) java.lang.String)
         (= (type (:community-item-name item)) java.lang.String)
         ]
   :post [(= (type %) java.lang.String)]}
  "2013-12-17 - for items that belong to all communities, such as users, then community name should be set to 'all'."
  (let [item-name (str (:community-item-name item) "-" (:item-type item)  "-" (:user-item-name item) "-")
        item-name (str item-name (str (java.util.UUID/randomUUID)))
        item-name (st/replace item-name  #"[^A-Za-z0-9]" "-")
        item-name (st/replace item-name #"--" "-")
        item-name (st/replace item-name #"--" "-")
        item-name (st/lower-case item-name)]
    item-name))
