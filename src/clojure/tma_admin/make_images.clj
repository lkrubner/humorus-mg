(ns tma-admin.make-images
  (:import
   (java.util UUID)
   (javax.imageio ImageIO)
   (java.awt.image BufferedImage))
  (:require
   [tma-admin.secretary :as secretary]
   [tma-admin.item-name :as in]
   [me.raynes.fs :as fs]
   [dire.core :refer [with-postcondition with-handler supervise]]
   [clojure.string :as st]
   [clojure.pprint :as pp]
   [clojure.java.io :as io]))




(defn- path-to-image-directory-uploads []
  (if (fs/exists?  "/home/tma/public_html/files/uploads/")
    "/home/tma/public_html/files/uploads/" 
    "/Users/lkrubner/tma_files/uploads/")) 

(defn- make-file-name [v item]
  {:pre [(map? v)]
   :post [(= (type %) java.lang.String)]}
  (let [username (:user-item-name item)
        filename (get v "filename")
        filename (st/replace filename #"[^A-Za-z0-9\.]" "-")
        filename (str username "-" (java.util.UUID/randomUUID) "-" filename)]
    filename))

(defn- make-destination-path-for-file [file-name]
  {:pre [(= (type file-name) java.lang.String)]}
  (str (path-to-image-directory-uploads) file-name))

(defn make-file-document-if-a-file-has-been-uploaded [item]
  {:pre [(map? item)]}
  "2014-02-06 - this is called when a user first uploads an image. It saves the image without making changes to it. A function running in a separte thread eventually makes thumbnails out of this image, and then deletes this images from the 'uploads' folder where it is here uploaded to."
  (doseq [[k v] item]
    (when (map? v)
      (when (number? (get v "size"))
        (when (> (get v "size") 0)
          (when (not (= (get v "filename") ""))
            (let [file-name (make-file-name v item)
                  image-to-save {
                                 :item-type "files"
                                 :item-name (in/make-item-name 
                                             (merge v {
                                                       :item-type "files"
                                                       :user-item-name (or (:user-item-name item)  (:item-name item))
                                                       :community-item-name (:community-item-name item)
                                                       }))
                                 :community-item-name (:community-item-name item)
                                 :user-item-name (or (:user-item-name item)  (:item-name item))
                                 :in-response-to-item-name (:item-name item)
                                 :file-name file-name
                                 :content-type (get v "content-type")
                                 }]
              (secretary/persist-item image-to-save)
              (with-open [in (io/input-stream (get v "tempfile"))
                          out (io/output-stream (make-destination-path-for-file file-name))]
                (io/copy in out)))))))))






