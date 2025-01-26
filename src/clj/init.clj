(ns init
  (:require [c3kit.apron.legend :as legend]
            [c3kit.bucket.memory]
            [schema :as schema]))

(defn install-legend! []
  (legend/init! {
                 :dog schema/dog
                 }))
