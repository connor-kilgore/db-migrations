(ns db.migrations.20250116-add-many-dogs
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.bucket.api :as db]))

(defn up []
  (doseq [n (range 1000)]
    (db/tx*
      (for [m (range 1000)]
        {:kind :dog :age (-> n (* 1000) (+ m)) :id (ccc/new-uuid)}))))

(defn down []
  (db/delete-all :dog))