(ns db.migrations.20250113-base
  (:require [c3kit.bucket.migrator :as m]))

(defn up []
  (m/add-attribute! "dog" "name" {:type :string})
  )

(defn down [])