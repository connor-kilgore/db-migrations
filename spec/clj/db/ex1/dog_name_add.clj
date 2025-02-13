(ns db.ex1.dog-name-add
  (:require [c3kit.bucket.migrator :as m]))

(defn up []
  (m/add-attribute! :dog :name {:type :string}))
