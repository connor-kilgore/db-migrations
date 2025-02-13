(ns schema
  (:require [c3kit.apron.schema :as s]))

;(def story
;  {:kind             (s/kind :story)
;   :id               s/id
;   :jira-id          {:type :long}
;   :project          {:type :ref :validate s/present? :message "must be present"}
;   :milestone        {:type :ref}
;   :iteration        {:type :ref}
;   :estimate         {:type :float}
;   :description      {:type :string}
;   :members          {:type [:ref] :db [:no-history]}
;   :number           {:type :long}
;   :poker-id         {:type :long}
;   :tags             {:type [:ref] :db [:no-history]}
;   :todos            {:type :string :db [:no-history]}
;   :attachments      {:type [:ref] :db [:no-history]}
;   :title            {:type :string :validate s/present? :message "must be present"}
;   :state            {:type :keyword :db [:no-history] :validate #(contains? story-states %) :message "must be valid story state"}
;   :last-modified-by {:type :ref}
;   })

(def dog
  {:kind (s/kind :dog)
   :id   {:type :uuid :db {:type "uuid PRIMARY KEY"} :strategy :pre-populated}
   ;:name {:type :string :db {:type "text"}}
   ;:age  {:type :long :db {:type "long"}}
   })

(def full-schema [dog])