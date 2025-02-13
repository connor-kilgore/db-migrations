(ns db.migrations.20250115-name-to-age
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.apron.schema :as s]
            [c3kit.bucket.api :as db]
            [c3kit.bucket.migrator :as m]
            [config :as config]
            ))

(def migration-dog
  {:kind (s/kind :dog)
   :id   {:type :uuid :db {:type "uuid PRIMARY KEY"} :strategy :pre-populated}
   :name {:type :string :db {:type "text"}}
   :name-delete {:type :string :db {:type "text" :name "name_deleted"}}
   :age  {:type :long :db {:type :long}}
   })

(def schemas [migration-dog])
(def migration-db (db/create-db config/jdbc schemas))

(defn up []
;  (m/add-attribute! "dog" "age" {:type :long})
;  (let [dogs (db/find-by- migration-db :dog)
;        updated-dogs (map #(assoc % :age (count (:name %))) dogs)]
;    (db/tx* migration-db updated-dogs))
;  (m/rename-attribute! "dog" "name" "dog" "name_deleted")
  )
;
(defn down []
;  (m/rename-attribute! "dog" "name_deleted" "dog" "name")
;  (m/remove-attribute! "dog" "age")
  )