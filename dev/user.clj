(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :as pp]
            [clojure.repl :as repl]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]))









(defn is-the-user-namespace-loaded? []
  (println "yes, the user namespace has been loaded"))


