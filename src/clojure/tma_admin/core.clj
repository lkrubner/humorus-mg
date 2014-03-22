(ns tma-admin.core
  (:gen-class)
  (:require [tma-admin.server :as server]))

(defn -main [& args]
  (server/start args))

