(ns tma-admin.server
  (:require [tma-admin.supervisor :as supervisor]
            [dire.core :refer [with-handler supervise]]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [me.raynes.fs :as fs]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds]))
  (:use [ring.util.response]
        [ring.middleware.params]
        [ring.middleware.reload]
        [ring.middleware.keyword-params]
        [ring.middleware.multipart-params]
        [ring.middleware.nested-params]
        [ring.middleware.file]
        [ring.middleware.resource]
        [ring.middleware.cookies]
        [ring.middleware.file-info]
        [ring.middleware.session]
        [ring.middleware.session.cookie]
        [ring.middleware.session.store]
        [ring.middleware.content-type]
        [ring.middleware.not-modified]
        [ring.adapter.jetty :only [run-jetty]]))


(def users {"admin" {:username "admin"
                     :password (creds/hash-bcrypt "password")
                     :roles #{::admin}}})
(derive ::admin ::user)


(defn change-current-logging-level [request]
  {:pre [(map? request)] }
  "2013-09-08 - can be called from (action), whereas set-the-current-debugging-level is only called at startup."
  (supervisor/change-current-logging-level (get-in request [:params :current-debugging-level]))
  (ring.util.response/response "Level of debugging/logging changed."))

(defn home-page []
  (assoc
      (ring.util.response/response "Hi. This is TMA. But you must be looking for a different URL?")
    :headers {"Content-Type" "text/html"}))

(defn admin-home-page [request]
  {:pre [(map? request)] }
  (assoc
      (ring.util.response/response (supervisor/home-page request))
    :headers {"Content-Type" "text/html"}))

(defn admin-process-input [request]
  {:pre [(map? request)] }
  (assoc
      (ring.util.response/response (supervisor/process-input request))
    :headers {"Content-Type" "text/html"}))

(defn admin-list-items [request]
  {:pre [(map? request)] }
  (assoc
      (ring.util.response/response (supervisor/render-page request "list_items.html"))
    :headers {"Content-Type" "text/html"}))

(defn admin-edit-items [request]
  {:pre [(map? request)] }
  (assoc
      (ring.util.response/response (supervisor/render-page request "edit_items.html"))
    :headers {"Content-Type" "text/html"}))
  
(defn admin-show-items [request]
  {:pre [(map? request)] }
  (assoc
      (ring.util.response/response (supervisor/render-page request "show_items.html"))
    :headers {"Content-Type" "text/html"}))

(defn admin-user-search [request]
  {:pre [(map? request)] }
  (assoc
      (ring.util.response/response (supervisor/user-search request))
    :headers {"Content-Type" "text/html"}))

(defn login [request]
  {:pre [(map? request)]}
  (assoc
      (ring.util.response/response (supervisor/login request))
        :headers {"Content-Type" "text/html"}))

(defroutes secure-routes
  (GET "/" request (admin-home-page request))
  (GET "/user-search" request (admin-user-search request))
  (GET "/list/:item-type" request (admin-list-items request))
  (GET "/list/:item-type/:which-page" request (admin-list-items request))
  (GET "/show/:item-name" request (admin-show-items request))
  (POST "/process-input/" request (admin-process-input request))
  (GET "/edit/:item-type" request (admin-edit-items request))
  (GET "/edit/:item-type/:item-name" request (admin-edit-items request)))

(defroutes app-routes
  (context "/admin" request
           (friend/wrap-authorize secure-routes #{::user}))
  (GET "/change-current-debugging-level" request (change-current-logging-level request))
  (ANY "/" request (home-page))
  (ANY "/login" request (login request))
  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  (route/resources "/")
  (route/not-found "Page not found"))

(defn- path-to-files-directory []
  (if (fs/exists? "/home/tma/public_html/files/")
    "/home/tma/public_html/files/" 
    "/Users/lkrubner/tma_files/")) 

(def app
  (-> app-routes
      (friend/authenticate {:workflows [(workflows/interactive-form)]
                            :credential-fn (partial creds/bcrypt-credential-fn users)})
      (wrap-session {:cookie-name "tma-admin-session" :cookie-attrs {:max-age 90000000}})
      (wrap-file (path-to-files-directory))
      (wrap-cookies)
      (wrap-keyword-params)
      (wrap-multipart-params)
      (wrap-nested-params)
      (wrap-params)
      (wrap-content-type)
      (wrap-not-modified)))





(defn start [& args]
  (println "App 'TMA admin' is starting.")
  (println "If no port is specified then we will default to port 34000.")
  (println "You can set this app to production mode, where debugging output to the terminal is surpressed, by starting it like this:")
  (println "java -jar target/admin-1-standalone.jar  30001 production")
  (let [port (if (nil? (first (first args)))
                       34000
                       (Integer/parseInt (first (first args))))]
    (try
      (supervisor/start)
      (run-jetty #'app {:port port :join? false :max-threads 5000})
      (catch Exception e (println (str e))))))
