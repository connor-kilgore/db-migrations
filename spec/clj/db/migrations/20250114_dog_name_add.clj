(ns db.migrations.20250114-dog-name-add
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.apron.schema :as s]
            [c3kit.bucket.api :as db]
            [config :as config]
            ))

(def migration-dog
  {:kind (s/kind :dog)
   :id   {:type :uuid :db {:type "uuid PRIMARY KEY"} :strategy :pre-populated}
   :name {:type :string :db {:type "text"}}})

(def schemas [migration-dog])
(def migration-db (db/create-db config/jdbc schemas))

(defn up []
  (db/tx- migration-db {:kind :dog :name "fido" :id (ccc/new-uuid)}))

(defn down []
  (let [fido (db/ffind-by- migration-db :dog :name "fido")]
    (when fido (db/delete- migration-db fido))))