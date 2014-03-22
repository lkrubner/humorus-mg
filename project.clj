(defproject tma-admin "1"
  :description "This is the admin dashboard that provides, via HTTP/HTML, a GUI through which TMA staff can interact with the database that powers TMA."
  :url "http://www.tailormadeanswers.com/"
  :license {:name "Copyright Lawrence Krubner 2013"
            :url "http://www.tailormadeanswers.com/"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [dire "0.5.1"]
                 [com.cemerick/friend "0.2.0"]
                 [ring "1.2.1"]
                 [clj-time "0.6.0"]
                 [org.clojure/data.json "0.2.3"]
                 [enlive "1.1.5"]
                 [compojure "1.1.6"]
                 [cheshire "5.2.0"]
                 [com.novemberain/monger "1.7.0-beta1"]
                 [com.taoensso/timbre "2.7.1"]
                 [org.clojure/tools.namespace "0.2.4"]
                 [formative "0.8.7"]
                 [lamina "0.5.0"]
                 [me.raynes/fs "1.4.4"]
                 [org.clojure/core.incubator "0.1.3"]
                 [hiccup "1.0.4"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/java.classpath "0.2.0"]]}}
  :repositories [["central-proxy" "https://repository.sonatype.org/content/repositories/centralm1/"]]
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"]
  :disable-implicit-clean true
  :warn-on-reflection true
  :main tma-admin.core
  :jvm-opts ["-Xms512m" "-Xmx2000m" "-XX:-UseCompressedOops"])



