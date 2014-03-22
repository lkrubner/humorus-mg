(ns tma-admin.tma-programmer-designer-contract
  (:require [tma-admin.controller :as controller]
            [clojure.pprint :as pp]
            [clojure.string :as st]
            [dire.core :refer [with-handler supervise]]
            [net.cgrand.enlive-html :as enlive]))




;; 2013-12-13 - in admin.views we establish a system whereby any HTML node that has a CSS class
;; of "tma-programmer-designer-contract" will also have an ID that is a function that should be
;; called. All of those functions live here in this namespace.



(defn- create-string-with-this-items-important-fields [item]
  (loop [all-fields-to-loop-over (keys item)
         this-one-field-as-keyword (first all-fields-to-loop-over)
         string-to-show-the-user ""]
    (if (first all-fields-to-loop-over)
      (recur
       (rest all-fields-to-loop-over)
       (first all-fields-to-loop-over)
       (if (controller/is-this-field-important (:item-type item) this-one-field-as-keyword)
         (str string-to-show-the-user "<span class='in-a-list-" (controller/transform-keyword-into-string this-one-field-as-keyword)  "'>" (controller/transform-keyword-into-string this-one-field-as-keyword) " : " (this-one-field-as-keyword item) "</span>")
         string-to-show-the-user))
      string-to-show-the-user)))
  
(defn tma-all-user-info [template request]
  {:pre [
         (vector? template)
         (map? request)
         ]
   :post [(vector? %)]}
  (enlive/transform template [:p.tma-paginated-list]
                    (enlive/clone-for [item (controller/get-user-info-for-search)]
                                      [:p.tma-paginated-list :a]
                                      (enlive/content (:username item))
                                      [:p.tma-paginated-list :a]
                                      (enlive/set-attr :href (str "/admin/edit/users/" (:item-name item)))
                                      [:p.tma-paginated-list :span.user-email]
                                      (enlive/content (:email item))
                                      [:p.tma-paginated-list :span.user-public-name]
                                      (enlive/content (str (:first-name item) " " (:last-name item))))))

(defn tma-bottom-links-list [template request]
  {:pre [
         (vector? template)
         (map? request)
         ]
   :post [(vector? %)]}
  (enlive/transform template [:#list-items :li]
                    (enlive/clone-for [one-type-as-string (controller/get-schema-types)]
                                      [:li :a]
                                      (enlive/content one-type-as-string)
                                      [:li :a]
                                      (enlive/set-attr :href (str "/admin/list/" one-type-as-string)))))

(defn tma-bottom-links-edit [template request]
  {:pre [
         (vector? template)
         (map? request)
         ]
   :post [(vector? %)]}  
  (enlive/transform template [:#add-new :li]  
                    (enlive/clone-for [one-type-as-string (controller/get-schema-types)]
                                      [:li :a]
                                      (enlive/content (str "New " one-type-as-string))
                                      [:li :a]
                                      (enlive/set-attr :href (str "/admin/edit/" one-type-as-string)))))

(defn tma-link-to-show-page [template request]
  {:pre [
         (vector? template)
         (map? request)
         ]
   :post [(vector? %)]}  
  (enlive/transform template [:#tma-link-to-show-page :a]  
                    (enlive/set-attr :href (str "/admin/show/" (get-in request [:params :item-name])))))

(defn tma-link-to-edit-page [template request]
  {:pre [
         (vector? template)
         (map? request)
         ]
   :post [(vector? %)]}
  (let [item (first (controller/fetch "get-current-item" request))]
    (enlive/transform template [:#tma-link-to-edit-page :a]  
                      (enlive/set-attr :href (str "/admin/edit/" (:item-type item) "/" (:item-name item))))))

(defn tma-show-form-for-editing [template request]
  {:pre [
         (vector? template)
         (map? request)
         ]
   :post [(vector? %)]}  
  (enlive/transform template [:#tma-show-form-for-editing] (enlive/html-content (controller/get-form request))))

(defn tma-children-item-links [template request]
  {:pre [
         (vector? template)
         (map? request)
         ]
   :post [(vector? %)]}  
  (enlive/transform template [:#tma-children-item-links :.tma-paginated-list]  
                    (enlive/clone-for [item (controller/fetch "get-children-items" request)]
                                      [:.tma-paginated-list :a]
                                      (enlive/html-content (create-string-with-this-items-important-fields item))
                                      [:.tma-paginated-list :a]
                                      (enlive/set-attr :href (str "/admin/show/" (:item-name item))))))

(defn tma-parent-item-links [template request]
  {:pre [
         (vector? template)
         (map? request)
         ]
   :post [(vector? %)]}  
  (enlive/transform template [:#tma-parent-item-links :.tma-paginated-list]
                    (enlive/clone-for [item (controller/fetch "get-parent-items" request)]
                                      [:.tma-paginated-list :a]
                                      (enlive/html-content (create-string-with-this-items-important-fields item))
                                      [:.tma-paginated-list :a]
                                      (enlive/set-attr :href (str "/admin/show/" (:item-name item))))))

(defn tma-show-all-possible-fields-for-this-item  [template request]
  {:pre [
         (vector? template)
         (map? request)
         ]
   :post [(vector? %)]}
  (let [item (first (controller/fetch "get-current-item" request))]
    (enlive/transform template [:#tma-show-all-possible-fields-for-this-item :.all-possible-fields]  
                      (enlive/clone-for [field-as-keyword (keys item)]
                                        [:h3]
                                        (enlive/content (controller/transform-keyword-into-string field-as-keyword))
                                        [:p]
                                        (enlive/content (str (field-as-keyword item)))))))

(defn tma-paginate-items [template request]
  {:pre [
         (vector? template)
         (map? request)
         ]
   :post [(vector? %)]}
  (let [results-to-paginate (controller/fetch "paginate-results" request)]
    (if results-to-paginate
      (do 
        (enlive/transform template [:#tma-paginate-items :.tma-paginated-list]  
                          (enlive/clone-for [item results-to-paginate]
                                            [:.tma-paginated-list :a]
                                            (enlive/html-content (create-string-with-this-items-important-fields item))
                                            [:.tma-paginated-list :a]
                                            (enlive/set-attr :href (str "/admin/show/" (:item-name item))))))
      template)))

(defn tma-show-count-of-pages-to-paginate [template request]
  {:pre [
         (vector? template)
         (map? request)
         ]
   :post [(vector? %)]}
  (let [vector-holding-one-number (controller/fetch "get-count" request)
        seq-of-seqs-of-numbers (partition 100 (range (first vector-holding-one-number)))]
    (enlive/transform template [:#tma-show-count-of-pages-to-paginate :a]
                      (enlive/clone-for [n seq-of-seqs-of-numbers]
                                        [:a.link-for-pagination]
                                        (enlive/content (str (first n)))
                                        [:a.link-for-pagination]
                                        (enlive/set-attr :href (str "/admin/list/" (get-in request [:params :item-type]) "/" (str (first n))))))))

(defn tma-span-to-show-item-type  [template request]
  {:pre [
         (vector? template)
         (map? request)
         ]
   :post [(vector? %)]}
  (enlive/transform template [:#tma-span-to-show-item-type] (enlive/content (get-in request [:params :item-type]))))

(defn tma-link-to-this-item-if-it-is-a-file [template request]
  {:pre [
         (vector? template)
         (map? request)
         ]
   :post [(vector? %)]}
    (let [item (first (controller/fetch "get-current-item" request))
          template (enlive/transform template [:#tma-link-to-this-item-if-it-is-a-file :a] (enlive/content (str "Download: " (:file-name item))))
        template (enlive/transform template [:#tma-link-to-this-item-if-it-is-a-file :a] (enlive/set-attr :href (str "/processed/" (:file-name item))))]
    template))

(defn tma-links-to-old-versions [template request]
  {:pre [
         (vector? template)
         (map? request)
         ]
   :post [(vector? %)]}
  (enlive/transform template [:#tma-links-to-old-versions :.tma-paginated-list]
                    (enlive/clone-for [item (controller/get-old-versions (get-in request [:params :item-name]))]
                                      [:.tma-paginated-list :a]
                                      (enlive/html-content (create-string-with-this-items-important-fields item))
                                      [:.tma-paginated-list :a]
                                      (enlive/set-attr :href (str "/admin/show/" (:item-name item))))))



