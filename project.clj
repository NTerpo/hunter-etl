(defproject hunter-etl "0.3.0-SNAPSHOT"
  :description "ETL for the Hunter API"
  :url "https://github.com/NTerpo/hunter-ETL"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.0.1"]
                 [cheshire "5.3.1"]]
  :plugins [[codox "0.8.10"]]
  :main hunter-etl.core)
