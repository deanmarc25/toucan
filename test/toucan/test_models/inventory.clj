(ns toucan.test-models.inventory
  "A model with `:primary-key` set to `sku`"
  (:require [toucan.models :as models]))

(models/defmodel Inventory :inventories)

(extend (class Inventory)
  models/IModel
  (merge models/IModelDefaults {:primary-key    (constantly :sku)}))
