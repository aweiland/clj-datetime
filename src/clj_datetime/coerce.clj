(ns clj-datetime.coerce
  "Utilites to coerce ZonedDateTime instances to and from various other types.
   For example, to convert a ZonedDateTime to and from a Java long:

     => (to-long (date-time 1998 4 25))
     893462400000

     => (from-long 893462400000)
     #<ZonedDateTime 1998-04-25T00:00:00.000Z>"
  (:refer-clojure :exclude [extend second])
  (:require [clj-datetime.core :refer :all]
            [clj-datetime.format :as time-fmt])
  (:import [java.sql Timestamp]
           [java.util Date]
           [java.time Instant ZoneId LocalDateTime LocalDate ZonedDateTime ZoneId]
           [java.time.temporal ChronoField]))

(defprotocol ICoerce
  (^java.time.ZonedDateTime
    to-date-time [obj] "Convert `obj` to a  ZonedDateTime instance."))

(defprotocol IMillis
  (get-millis [this] "Get milliseconds from some date time type"))

(extend-protocol IMillis
  nil
  (get-millis [_] nil)

  ZonedDateTime
  (get-millis [this] (.getLong this ChronoField/MILLI_OF_SECOND))

  LocalDateTime
  (get-millis [this] (.getLong this ChronoField/MILLI_OF_SECOND)))


(defn from-long
  "Returns a ZonedDateTime instance in the UTC time zone corresponding to the given
   number of milliseconds after the Unix epoch."
  [^Long millis]
  (.atZone (Instant/ofEpochMilli millis) ^ZoneId utc))

(defn from-string
  "return DateTime instance from string using
   formatters in clj-datetime.format, returning first
   which parses"
  [^String s]
  (time-fmt/parse s))

(defn from-date
  "Returns a ZonedDateTime instance in the UTC time zone corresponding to the given
   Java Date object."
  [^Date date]
  (when date
    (from-long (.getTime date))))

(defn from-sql-date
  "Returns a ZonedDateTime instance in the UTC time zone corresponding to the given
   java.sql.Date object."
  [^java.sql.Date sql-date]
  (when sql-date
    (from-long (.getTime sql-date))))

(defn from-sql-time
  "Returns a ZonedDateTime instance in the UTC time zone corresponding to the given
   java.sql.Timestamp object."
  [^java.sql.Timestamp sql-time]
  (when sql-time
    (from-long (.getTime sql-time))))

(defn to-long
  "Convert `obj` to the number of milliseconds after the Unix epoch."
  [obj]
  (if-let [dt (to-date-time obj)]
    (get-millis dt)))

(defn to-epoch
  "Convert `obj` to Unix epoch."
  [obj]
  (let [millis (to-long obj)]
    (and millis (quot millis 1000))))

(defn to-date
  "Convert `obj` to a Java Date instance."
  [obj]
  (if-let [dt (to-date-time obj)]
    (Date. (get-millis dt))))

(defn to-sql-date
  "Convert `obj` to a java.sql.Date instance."
  [obj]
  (if-let [dt (to-date-time obj)]
    (java.sql.Date. (get-millis dt))))

(defn to-sql-time
  "Convert `obj` to a java.sql.Timestamp instance."
  [obj]
  (if-let [dt (to-date-time obj)]
    (java.sql.Timestamp. (get-millis dt))))

(defn to-string
  "Returns a string representation of obj in UTC time-zone
  using (ISODateTimeFormat/dateTime) date-time representation."
  [obj]
  (if-let [^ZonedDateTime dt (to-date-time obj)]
    (time-fmt/unparse (:date-time time-fmt/formatters) dt)))

(defn to-timestamp
  "Convert `obj` to a Java SQL Timestamp instance."
  [obj]
  (if-let [dt (to-date-time obj)]
    (Timestamp. (get-millis dt))))

(defn to-local-date
  "Convert `obj` to a java.time.LocalDate instance"
  [obj]
  (if-let [dt (to-date-time obj)]
    (LocalDate. (get-millis (from-time-zone dt (default-time-zone))))))

(defn to-local-date-time
  "Convert `obj` to a java.time.LocalDateTime instance"
  [obj]
  (if-let [dt (to-date-time obj)]
    (LocalDateTime. (get-millis (from-time-zone dt (default-time-zone))))))

(defn in-time-zone
  "Convert `obj` into `tz`, return java.time.LocalDate instance."
  [obj tz]
  (if-let [dt (to-date-time obj)]
    (-> dt
        (to-time-zone tz)
        .toLocalDate)))

(extend-protocol ICoerce
  nil
  (to-date-time [_]
    nil)

  Date
  (to-date-time [date]
    (from-date date))

  java.sql.Date
  (to-date-time [sql-date]
    (from-sql-date sql-date))

  java.sql.Timestamp
  (to-date-time [sql-time]
    (from-sql-time sql-time))

  DateTime
  (to-date-time [date-time]
    date-time)

  YearMonth
  (to-date-time [year-month]
    (date-time (year year-month) (month year-month)))

  LocalDate
  (to-date-time [local-date]
    (date-time (year local-date) (month local-date) (day local-date)))

  LocalDateTime
  (to-date-time [local-date-time]
    (date-time (year local-date-time) (month local-date-time) (day local-date-time)
               (hour local-date-time) (minute local-date-time) (second local-date-time)
               (milli local-date-time)))

  Integer
  (to-date-time [integer]
    (from-long (long integer)))

  Long
  (to-date-time [long]
    (from-long long))

  String
  (to-date-time [string]
    (from-string string))

  Timestamp
  (to-date-time [timestamp]
    (from-date timestamp)))
