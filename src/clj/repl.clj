(ns repl
  (:require
    [main :as main]
    [init :as init]))

(println "Welcome to the Migrations REPL!")
(println "Initializing")
(require '[c3kit.bucket.api :as db])
(require '[c3kit.bucket.migrator :as m])
(init/install-legend!)
(main/start-db)
