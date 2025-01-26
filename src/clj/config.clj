(ns config)

(def jdbc
  {:impl             :jdbc
   :connection-pool? true
   :dbtype           "postgresql"
   :dialect          :postgres
   :max-pool-size    10
   :min-pool-size    3
   :password         ""
   :port             5432
   :user             "developer"
   :dbname           "demo_local"
   :host             "127.0.0.1"})