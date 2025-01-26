(ns main
  (:require [c3kit.apron.app :as app]
            [c3kit.bucket.api :as db]))

(defn start-db [] (app/start! [db/service]))
