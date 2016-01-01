(ns clj-datetime.jdbc
  "clojure.java.jdbc protocol extensions supporting DateTime coercion.

  To use in your project, just require the namespace:

    => (require 'clj-datetime.jdbc)
    nil

  Doing so will extend the protocols defined by clojure.java.jdbc, which will
  cause java.sql.Timestamp objects in JDBC result sets to be coerced to
  java.time.ZonedDateTime objects, and vice versa where java.sql.Timestamp
  objects would be required by JDBC."
  (:require [clj-datetime.coerce :as tc]
            [clojure.java.jdbc :as jdbc]))

; http://clojure.github.io/java.jdbc/#clojure.java.jdbc/IResultSetReadColumn
(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Timestamp
  (result-set-read-column [v _2 _3]
    (tc/from-sql-time v))
  java.sql.Date
  (result-set-read-column [v _2 _3]
    (tc/from-sql-date v))
  java.sql.Time
  (result-set-read-column [v _2 _3]
    (tc/from-date v)))

; http://clojure.github.io/java.jdbc/#clojure.java.jdbc/ISQLValue
(extend-protocol jdbc/ISQLValue
  java.time.ZonedDateTimeDateTime
  (sql-value [v]
    (tc/to-sql-time v)))
