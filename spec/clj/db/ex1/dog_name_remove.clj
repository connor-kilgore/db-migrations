(ns db.ex1.dog-name-remove
  (:require [c3kit.bucket.migrator :as m]))

(defn up []
  (m/remove-attribute! :dog :name))