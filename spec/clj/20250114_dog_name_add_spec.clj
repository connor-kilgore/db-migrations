(ns db.migrations.20250114-dog-name-add-spec
  (:require [c3kit.bucket.api :as db]
            [speclj.core :refer :all]
            [schema :as schema]
            [db.migrations.20250114-dog-name-add :as sut]))

(defn create-spec-db [schemas]
  (let [backend-schemas  (remove #(contains? schemas (-> % :kind :value)) schema/full-schema)
        schemas (concat backend-schemas schemas)
        spec-db (db/create-db (assoc config/jdbc :impl :memory) [schemas])
        _ (db/set-safety! false)]
    spec-db))

(def spec-db (create-spec-db sut/schemas))

(describe "Dog name add"
  (redefs-around [sut/migration-db spec-db])
  (context "up"
    (it "fido exists"
      (sut/up)
      (should= "fido" (:name (db/ffind-by- spec-db :dog :name "fido")))))

  (context "down"
    (it "fido is deleted"
      (sut/down)
      (should-be-nil (db/ffind-by- spec-db :dog :name "fido"))))
  )
