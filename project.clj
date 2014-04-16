(defproject knakk/svamp "0.1.0-SNAPSHOT"
  :description "Svamp"
  :url "http://github.com/knakk/svamp"
  :license {:name "GPLv3"
            :url "http://www.gnu.org/licenses/gpl-3.0.html"}
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[javax.servlet/servlet-api "2.5"]
                                  [org.clojure/tools.namespace "0.2.4"]
                                  [expectations "2.0.6"]]}}
  :source-paths ["src/clj"]
  :dependencies [
                 ;; Temporary as not to fail tests:
                 [org.immutant/immutant-web "1.1.1"]

                 ;; Java
                 ;[org.elasticsearch/elasticsearch "1.1.0"]

                 ;; Clojure
                 [org.clojure/clojure "1.6.0"]
                 [compojure "1.1.6"]
                 [prismatic/schema "0.2.1"]
                 [com.taoensso/timbre "3.1.6"]
                 [clojurewerkz/elastisch "2.0.0-beta3"]
                 [com.draines/postal "1.9.2"]
                 [clj-http "0.9.1"]
                 [fogus/ring-edn "0.2.0"]

                 ;; Clojurecsript
                 [org.clojure/clojurescript "0.0-2173"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [om "0.5.3"]]
  :plugins [[lein-cljsbuild "1.0.2"]]
  :cljsbuild {
  :builds [{:id "dev"
            :source-paths ["src/cljs"]
            :compiler {
              :output-to "resources/public/main.js"
              :output-dir "resources/public/out"
              :optimizations :none
              :source-map true}}
           {:id "release"
            :source-paths ["src/cljs"]
            :compiler {
              :output-to "main.js"
              :optimizations :advanced
              :pretty-print false
              :preamble ["react/react.min.js"]
              :externs ["react/externs/react.js"]}}]})
