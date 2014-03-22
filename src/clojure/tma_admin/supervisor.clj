(ns tma-admin.supervisor
  (:require [tma-admin.perpetual :as perpetual]
            [tma-admin.startup :as startup]
            [tma-admin.views :as views]
            [clojure.string :as st]
            [clojure.stacktrace :as stack]
            [dire.core :refer [with-handler supervise]])
  (:use
   [taoensso.timbre :as timbre :only (trace debug info warn error fatal spy)]))





(defmacro start-perpetual-events []
  `(do ~@(for [i (map first (ns-publics 'tma-admin.perpetual))]
           `(.start (Thread. ~(symbol (str "perpetual/" i)))))))

(defmacro start-startup-events []
  `(do ~@(for [i (map first (ns-publics 'tma-admin.startup))]
           `(~(symbol (str "startup/" i))))))

(defn change-current-logging-level [request]
  "2013-09-08 - can be called from (action), whereas set-the-current-debugging-level is only called at startup."
  (startup/set-the-current-debugging-level (get-in request [:params :current-debugging-level])))

(defn home-page [request]
  (views/home request))

(defn process-input [request]
  (views/process-input request))

(defn render-page [request which-template]
  (views/render-page request which-template))

(defn user-search [request]
  (views/user-search request))

(defn login [request]
  (supervise #'views/login request))

(with-handler #'views/login
  java.lang.AssertionError
  (fn [e & args] (do
                   (println "exception: " (str e) " args: " (str args))
                   "Error rendering the form for login.")))

(defn print-error-info [e]
  "2012-12-10 - I originally put this in core.clj, but it is really just a utility function, so I will put it here."
  (prn "Exception in the main function: " e)
  (prn (apply str "The print-cause-trace with chained exceptions is:" (stack/print-cause-trace e)))
  (prn (apply str "The print-stack-trace standard clojure is:" (stack/print-stack-trace e))))

(defn start []
  (try 
    (start-startup-events)
    (start-perpetual-events)
    (catch Exception e (print-error-info e))))





