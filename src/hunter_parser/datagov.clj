(ns hunter-parser.datagov
  (:require [clj-http.client :as client]
            [cheshire.core :refer :all]
            [clojure.string :as st]
            [hunter-parser.util :refer :all]))

(def base-url "https://catalog.data.gov/api/3/")

(defn- get-tags
  [vect]
  (vec (map #(% :name) (map #(select-keys % [:name]) vect))))

(defn- get-temporal
  "try to find the value of 'temporal' from a data.gov metadata dataset"
  [vect]
  (first
   (filter #(not (nil? %))
           (map #(get % :temporal)
                (map #(hash-map (keyword (% :key)) (% :value))
                     (map #(select-keys % [:key :value]) vect))))))

(defn get-most-pop-datagov-ds
  "gets a number of the most popular datasets' metadata from the ckan API of data.gov and transforms them to match the Hunter API scheme"
  [number offset]
  (let [response (((get-result (str "https://catalog.data.gov/api/3/action/package_search?q=&rows=" number
                                    "&start=" offset)) :result) :results)]
    (->> (map #(select-keys % [:title :notes :organization :resources :tags :extras :revision_timestamp]) response)
         (map #(assoc % :publisher (get-in % [:organization :title])
                      :uri (if-not (nil? (get-in % [:resources 0 :url]))
                             (get-in % [:resources 0 :url])
                             "URI Not Available")
                      :created (if-not (nil? (get-in % [:resources 0 :created]))
                                 (get-in % [:resources 0 :created])
                                 (% :revision_timestamp))
                      :tags (vec (concat (tagify-title (% :title)) (extend-tags (get-tags (% :tags)))))
                      :spatial (geo-tagify "us")
                      :temporal (if (not (nil? (get-temporal (% :extras))))
                                  (extend-temporal (get-temporal (% :extras)))
                                  "all")
                      :updated (% :revision_timestamp)
                      :description (% :notes)))
         (map #(dissoc % :organization :resources :extras :revision_timestamp :notes)))))
