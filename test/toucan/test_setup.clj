(ns toucan.test-setup
  "Test setup logic and helper functions. All test namespaces should require this one to make sure the env is set up properly."
  (:require [clojure.java.jdbc :as jdbc]
            [expectations :refer :all]
            (toucan [db :as db]
                    [models :as models])
            (toucan.test-models [category :refer [Category]]
                                [inventory :refer [Inventory]]
                                [user :refer [User]]
                                [venue :refer [Venue]])
            [toucan.util.test :as u])
  (:import java.sql.Timestamp))

;; Don't run unit tests whenever JVM shuts down
(expectations/disable-run-on-shutdown)

;;; Basic Setup

(defn- configure-db! []
  (db/set-default-db-connection!
    (merge {:classname   "org.postgresql.Driver"
            :subprotocol "postgresql"
            :subname     "//localhost:5432/toucan_test"}
           (when (System/getenv "CIRCLECI")
             {:user    "ubuntu"
              :subname "//localhost:5432/circle_test"}))))

(defn- execute! {:style/indent 0} [& statements]
  (doseq [sql statements]
    (jdbc/execute! (db/connection) [sql])))

(defn- create-models! []
  (execute!
    ;; User
    "CREATE TABLE IF NOT EXISTS users (
       id SERIAL PRIMARY KEY,
       \"first-name\" VARCHAR(256) NOT NULL,
       \"last-name\" VARCHAR(256) NOT NULL
     );"
    "TRUNCATE TABLE users RESTART IDENTITY CASCADE;"
    ;; Venue
    "CREATE TABLE IF NOT EXISTS venues (
       id SERIAL PRIMARY KEY,
       name VARCHAR(256) UNIQUE NOT NULL,
       category VARCHAR(256) NOT NULL,
       \"created-at\" TIMESTAMP NOT NULL,
       \"updated-at\" TIMESTAMP NOT NULL
      );"
    "TRUNCATE TABLE venues RESTART IDENTITY CASCADE;"
    ;; Category
    "CREATE TABLE IF NOT EXISTS categories (
       id SERIAL PRIMARY KEY,
       name VARCHAR(256) UNIQUE NOT NULL,
       \"parent-category-id\" INTEGER
     );"
    "TRUNCATE TABLE categories RESTART IDENTITY CASCADE;"
    ;; Inventory
    "CREATE TABLE IF NOT EXISTS inventories (
      id SERIAL PRIMARY KEY,
      sku VARCHAR(256) UNIQUE NOT NULL,
      name VARCHAR(256) NOT NULL);"
    "TRUNCATE TABLE inventories RESTART IDENTITY CASCADE;"))

(def ^java.sql.Timestamp jan-first-2017 (Timestamp/valueOf "2017-01-01 00:00:00"))

(defn- insert-test-data! []
  ;; User
  (db/simple-insert-many! User
    [{:first-name "Cam",   :last-name "Saul"}
     {:first-name "Rasta", :last-name "Toucan"}
     {:first-name "Lucky", :last-name "Bird"}])
  ;; Venue
  (db/simple-insert-many! Venue
      [{:name "Tempest",     :category "bar",   :created-at jan-first-2017, :updated-at jan-first-2017}
       {:name "Ho's Tavern", :category "bar",   :created-at jan-first-2017, :updated-at jan-first-2017}
       {:name "BevMo",       :category "store", :created-at jan-first-2017, :updated-at jan-first-2017}])
  ;; Category
  (db/simple-insert-many! Category
    [{:name "bar"}
     {:name "dive-bar", :parent-category-id 1}
     {:name "resturaunt"}
     {:name "mexican-resturaunt", :parent-category-id 3}])
  ;; Inventory
  (db/simple-insert-many! Inventory
    [{:name "Computer" :sku "cp-1"}
     {:name "Text Book" :sku "tb-1"}
     {:name "Pencils" :sku "pn-1"}]))

(defn reset-db!
  "Reset the DB to its initial state, creating tables if needed and inserting the initial test data."
  []
  (create-models!)
  (insert-test-data!))

(defn- set-models-root-namespace! []
  (models/set-root-namespace! 'toucan.test-models))

(defn- test-setup! {:expectations-options :before-run} []
  (configure-db!)
  (reset-db!)
  (set-models-root-namespace!))

(defmacro with-clean-db
  "Run test BODY and reset the database to its initial state afterwards."
  {:style/indent 0}
  [& body]
  `(try ~@body
        (finally (reset-db!))))
