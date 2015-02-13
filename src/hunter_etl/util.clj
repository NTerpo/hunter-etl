(ns hunter-etl.util
  (:use [clojure.data :refer :all])
  (:require [clj-http.client :as client]
            [cheshire.core :refer :all]
            [clojure.string :as st]))

;;;; Extract

(defn get-result
  "gets metadata from an API and provides a first basic filter"
  [url]
  (-> url
      (client/get)
      :body
      (parse-string true)))

;;;; Transform

(def hunter-keys
  "keys needed in the Hunter API"
  [:title :description :publisher :uri :created :updated :spatial :temporal :tags :resources :huntscore])

(defn not-hunter-keys
  "keys on a collection that are not on the hunter-keys"
  [vect]
  (vec
   (second
    (diff (set hunter-keys) (set vect)))))

(defn geo-tagify
  "extend the spatial coverage tagging 'us'->'america'->'countries'->'world'"
  [geo]
  (let [geo (clojure.string/lower-case geo)]
    (if-not (nil? (some #{geo} ["france" "us" "europe" "world" "uk"]))
      ({"france" ["france" "fr" "europe" "schengen" "eu" "ue" "countries" "world" "all"]
        "us" ["us" "usa" "america" "united states" "united-states" "united states of america" "united-states-of-america" "world" "countries" "all"]
        "europe" ["europe" "schengen" "eu" "ue" "countries" "world" "all"]
        "world" ["world" "all" "countries"]
        "uk" ["uk" "england" "scotland" "wales" "ireland" "great-britain" "gb"]} geo)
      (vector geo))))

(defn extend-tags
  "create new tags with the given tags vector by spliting words and cleaning"
  [tags]
  (->> (vec (disj (set (->> (map st/lower-case tags)
                            (map st/trim)
                            (mapcat #(st/split % #"-"))
                            (concat tags))) "report" "data" "-" "service" "government"))
       (map #(st/replace % "," ""))
       (map #(st/replace % "(" ""))
       (map #(st/replace % ")" ""))))

(defn tagify-title
  "create new tags from the title"
  [title]
  (vec (disj (set (-> (st/lower-case title)
                      (st/split #" "))) "database" "-" "db" "data" "dataset" "to" "and")))

(defn extend-temporal
  "extend the temporal coverage with dates between limits"
  [temporal]
  (let [limits (re-seq #"[0-9]{4}" temporal)]
    (if (nil? limits)
      "all"
      (if (= 1 (count limits))
                        (vec limits)
                        (vec
                         (map str
                              (range (Integer. (first limits))
                                     (+ 1 (Integer. (last limits))))))))))

(defn calculate-huntscore
  "returns the sum of reuses, views/1000 recent-views/200 and followers/10"
  [reuses total-views recent-views followers]
  (reduce + [(* 1 reuses)
             (* 0.001 total-views)
             (* 0.005 recent-views)
             (* 0.1 followers)]))

;;;; Load

(def api-url "http://localhost:3000/api/datasets")

(defn load-to-hunter-api
  "Send each dataset to the Hunter API via method post"
  [coll]
  (map #(client/post api-url
                     {:body (generate-string %)
                      :content-type "application/json"}) coll))

;;;; By Hand Datasets

(defn create-ds
  "create a hunter dataset by hand
  eg. (create-ds \"Global Bilateral Migration Database\" \"Global matrices of bilateral migrant stocks spanning the period 1960-2000, disaggregated by gender and based primarily on the foreign-born concept are presented. Over one thousand census and population register records are combined to construct decennial matrices corresponding to the last five completed census rounds.\\n\\nFor the first time, a comprehensive picture of bilateral global migration over the last half of the twentieth century emerges. The data reveal that the global migrant stock increased from 92 to 165 million between 1960 and 2000. South-North migration is the fastest growing component of international migration in both absolute and relative terms. The United States remains the most important migrant destination in the world, home to one fifth of the world’s migrants and the top destination for migrants from no less than sixty sending countries. Migration to Western Europe remains largely from elsewhere in Europe. The oil-rich Persian Gulf countries emerge as important destinations for migrants from the Middle East, North Africa and South and South-East Asia. Finally, although the global migrant stock is still predominantly male, the proportion of women increased noticeably between 1960 and 2000.\" \"World Bank\" \"http://databank.worldbank.org/data/views/variableselection/selectvariables.aspx?source=global-bilateral-migration\" [\"World\" \"East Asia & Pacific\" \"Europe\" \" Asia\" \"Latin America\" \"Caribbean\" \"Middle East\" \"North Africa\" \"South Asia\" \"Sub Saharan\" \"Africa\"] \"1960 - 2000\" [\"migrants\" \"immigration\"] \"2011-07-01T00:00:00.000000\" \"2011-07-01T00:00:00.000000\" [] 5 0)"
  [title description publisher uri spatial temporal tags created updated resources reuses views]
  (list {:title title
         :description description
         :publisher publisher
         :uri uri
         :spatial spatial
         :temporal (extend-temporal temporal)
         :tags (vec (concat (tagify-title title)
                            tags))
         :created created
         :updated updated
         :resources resources
         :huntscore (calculate-huntscore reuses views 0 0)}))