(ns tma-admin.perpetual
  (:require
   [tma-admin.make-images :as mi]
   [tma-admin.monitoring :as monitor]
   [tma-admin.dates :as dates]
   [tma-admin.secretary :as secretary]
   [clojure.set :as sets]
   [clojure.pprint :as pp]))



(defn resource-usage []
  ;; 2013-05-28 - THIS FUNCTION IS CALLED AT STARTUP AND RUNS IN ITS OWN THREAD, REPEATING ENDLESSLY.
  ;;
  (println "The time is: " (dates/current-time-as-string))
  (doseq [x (monitor/thread-top)]
    (pp/pprint x))
  (println (str (monitor/show-stats-regarding-resources-used-by-this-app)))
  (. java.lang.Thread sleep 300000)
  (resource-usage))

(defn get-user-info-for-search []
  ;; 2014-02-04 - THIS FUNCTION IS CALLED AT STARTUP AND RUNS IN ITS OWN THREAD, REPEATING ENDLESSLY.
  ;;
  (secretary/add-to-interactions [:current :user-cache] (secretary/get-user-info-for-search))
  (. java.lang.Thread sleep 36000000)
  (get-user-info-for-search))


;; (defn make-image-thumbnails []
;;   ;; 2014-02-06 - THIS FUNCTION IS CALLED AT STARTUP AND RUNS IN ITS OWN THREAD, REPEATING ENDLESSLY.
;;   ;;
;;   (mi/make-image-thumbnails)
;;   (. java.lang.Thread sleep 300000)
;;   (make-image-thumbnails))
