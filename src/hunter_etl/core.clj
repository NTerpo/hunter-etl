(ns hunter-etl.core
  (:require [hunter-etl.transform :refer [load-to-hunter-api]]
            [hunter-etl.portals.data-gov :refer [dg-extract dg-transform]]
            [hunter-etl.portals.data-gouv-fr :refer [dgf-extract dgf-transform]]
            [hunter-etl.portals.data-gov-uk :refer [dguk-extract dguk-transform]]))

(defn dg-etl
  "data.gov ETL
  takes between 0 and 3 arguments :
  ([integer][integer][string])
  
  0 => loads in the Hunter DB the most popular dgu dataset cleaned
  1 => loads in the Hunter DB the given number of dgu datasets cleaned
  2 => same with an offset
  3 => same but only with datasets corresponding to a query

  Works perfectly well with number = 1000"
  [& args]
  (-> (apply dg-extract args)
      dg-transform
      load-to-hunter-api))

(defn dgf-etl
  "data.gouv.fr ETL
  takes between 0 and 3 arguments :
  ([integer][integer][string])
  
  0 => loads in the Hunter DB the most popular dgu dataset cleaned
  1 => loads in the Hunter DB the given number of dgu datasets cleaned
  2 => same with an page number ~ offset= page*number
  3 => same but only with datasets corresponding to a query

  With a number argument too big - eg 1000 - gets a HTTP 500 from the API"
  [& args]
  (-> (apply dgf-extract args)
      dgf-transform
      load-to-hunter-api))

(defn dguk-etl
  "data.gov.uk ETL
  takes between 0 and 3 arguments :
  ([integer][integer][string])
  
  0 => loads in the Hunter DB the most popular dgu dataset cleaned
  1 => loads in the Hunter DB the given number of dgu datasets cleaned
  2 => same with an offset
  3 => same but only with datasets corresponding to a query"
  [& args]
  (-> (apply dguk-extract args)
      dguk-transform
      load-to-hunter-api))
