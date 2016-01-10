(ns clj-datetime.core
  "The core namespace for date-time operations in the clj-datetime library.

   Create a ZonedDateTime instance with date-time (or a LocalDateTime instance with local-date-time),
   specifying the year, month, day, hour, minute, second, and nanosecond:

     => (date-time 1986 10 14 4 3 27 456)
     #<ZonedDateTime 1986-10-14T04:03:27.456Z>

     => (local-date-time 1986 10 14 4 3 27 456)
     #<LocalDateTime 1986-10-14T04:03:27.456>

   Less-significant fields can be omitted:

     => (date-time 1986 10 14)
     #<ZonedDateTime 1986-10-14T00:00:00.000Z>

     => (local-date-time 1986 10 14)
     #<LocalDateTime 1986-10-14T00:00:00.000>

   Get the current time with (now) and the start of the Unix epoch with (epoch).

   Once you have a date-time, use accessors like hour and second to access the
   corresponding fields:

     => (hour (date-time 1986 10 14 22))
     22

     => (hour (local-date-time 1986 10 14 22))
     22

   The date-time constructor always returns times in the UTC time zone. If you
   want a time with the specified fields in a different time zone, use
   from-time-zone:

     => (from-time-zone (date-time 1986 10 22) (time-zone-for-offset -2))
     #<ZonedDateTime 1986-10-22T00:00:00.000-02:00>

   If on the other hand you want a given absolute instant in time in a
   different time zone, use to-time-zone:

     => (to-time-zone (date-time 1986 10 22) (time-zone-for-offset -2))
     #<ZonedDateTime 1986-10-21T22:00:00.000-02:00>

   In addition to time-zone-for-offset, you can use the time-zone-for-id and
   default-time-zone functions and the utc Var to construct or get DateTimeZone
   instances.

   The functions after? and before? determine the relative position of two
   ZonedDateTime instances:

     => (after? (date-time 1986 10) (date-time 1986 9))
     true

     => (after? (local-date-time 1986 10) (local-date-time 1986 9))
     true

   Often you will want to find a date some amount of time from a given date. For
   example, to find the time 1 month and 3 weeks from a given date-time:

     => (plus (date-time 1986 10 14) (months 1) (weeks 3))
     #<ZonedDateTime 1986-12-05T00:00:00.000Z>

     => (plus (local-date-time 1986 10 14) (months 1) (weeks 3))
     #<LocalDateTime 1986-12-05T00:00:00.000Z>

   An Interval is used to represent the span of time between two DateTime
   instances. Construct one using interval, then query them using within?,
   overlaps?, and abuts?

     => (within? (interval (date-time 1986) (date-time 1990))
                 (date-time 1987))
     true

   To find the amount of time encompassed by an interval, use in-seconds and
   in-minutes:

     => (in-minutes (interval (date-time 1986 10 2) (date-time 1986 10 14)))
     17280

   The overlap function can be used to get an Interval representing the
   overlap between two intervals:

     => (overlap (t/interval (t/date-time 1986) (t/date-time 1990))
                             (t/interval (t/date-time 1987) (t/date-time 1991)))
     #<Interval 1987-01-01T00:00:00.000Z/1990-01-01T00:00:00.000Z>

   Note that all functions in this namespace work with Joda objects or ints. If
   you need to print or parse date-times, see clj-time.format. If you need to
   coerce date-times to or from other types, see clj-time.coerce."
  (:refer-clojure :exclude [extend second])
  (:import [java.time Instant ZoneId LocalDateTime LocalTime LocalDate
                      ZonedDateTime ZoneId ZoneOffset
                      Period Duration YearMonth]
           [java.time.temporal TemporalAmount ChronoUnit ChronoField TemporalAdjusters]
           [java.time.chrono ChronoZonedDateTime ChronoLocalDateTime ChronoLocalDate]
           ))

(defn deprecated [message]
  (println "DEPRECATION WARNING: " message))

(defprotocol DateTimeProtocol
  "Interface for various date time functions"
  (year [this] "Return the year component of the given date/time.")
  (month [this]   "Return the month component of the given date/time.")
  (day [this]   "Return the day of month component of the given date/time.")
  (day-of-week [this]   "Return the day of week component of the given date/time. Monday is 1 and Sunday is 7")
  (hour [this]   "Return the hour of day component of the given date/time. A time of 12:01am will have an hour component of 0.")
  (minute [this]   "Return the minute of hour component of the given date/time.")
  (second [this]   "Return the second of minute component of the given date/time.")
  (milli [this]   "Return the millisecond of second component of the given date/time.")
  (nano [this]   "Return the nanosecond of second component of the given date/time.")
  (equal? [this that] "Returns true if ReadableDateTime 'this' is strictly equal to date/time 'that'.")
  (after? [this that] "Returns true if ReadableDateTime 'this' is strictly after date/time 'that'.")
  (before? [this that] "Returns true if ReadableDateTime 'this' is strictly before date/time 'that'.")
  (plus- [this ^TemporalAmount period]
    "Returns a new date/time corresponding to the given date/time moved forwards by the given Period(s).")
  (minus- [this ^TemporalAmount period]
    "Returns a new date/time corresponding to the given date/time moved backwards by the given Period(s).")
  (first-day-of-the-month- [this] "Returns the first day of the month")
  (last-day-of-the-month- [this] "Returns the last day of the month")
  (week-number-of-year [this] "Returs the number of weeks in the year")) ; Might not be possible in java 8

(defprotocol InTimeUnitProtocol
  "Interface for in-<time unit> functions"
  (in-nanos [this] "Return the time in nanoseconds.")
  (in-millis [this] "Return the time in milliseconds.")
  (in-seconds [this] "Return the time in seconds.")
  (in-minutes [this] "Return the time in minutes.")
  (in-hours [this] "Return the time in hours.")
  (in-days [this] "Return the time in days.")
  (in-weeks [this] "Return the time in weeks")
  (in-months [this] "Return the time in months")
  (in-years [this] "Return the time in years"))

(extend-protocol DateTimeProtocol
  java.time.ZonedDateTime
  (year [this] (.getYear this))
  (month [this] (.getMonthValue this))
  (day [this] (.getDayOfMonth this))
  (day-of-week [this] (.. this (getDayOfWeek) (getValue)))
  (hour [this] (.getHour this))
  (minute [this] (.getMinute this))
  (sec [this]
    {:deprecated "0.6.0"}
    (deprecated "sec is being deprecated in favor of second")
    (.getSecond this))
  (second [this] (.getSecond this))
  (milli [this] (* (.getNano this) 1000000))
  (nano [this] (.getNano this))
  (equal? [this ^ChronoZonedDateTime that] (.isEqual this that))
  (after? [this ^ChronoZonedDateTime that] (.isAfter this that))
  (before? [this ^ChronoZonedDateTime that] (.isBefore this that))
  (plus- [this ^TemporalAmount period] (.plus this period))
  (minus- [this ^TemporalAmount period] (.minus this period))
  (first-day-of-the-month- [this]
    (.with ^ZonedDateTime this (TemporalAdjusters/firstDayOfMonth))) ; always 1 right? or is it a date time
  (last-day-of-the-month- [this]
     (.with ^ZonedDateTime this (TemporalAdjusters/lastDayOfMonth)))
  (week-number-of-year [this]
    (.get this ChronoField/ALIGNED_WEEK_OF_YEAR))

  java.time.LocalDateTime
  (year [this] (.getYear this))
  (month [this] (.getMonthValue this))
  (day [this] (.getDayOfMonth this))
  (day-of-week [this] (.. this (getDayOfWeek) (getValue)))
  (hour [this] (.getHour this))
  (minute [this] (.getMinute this))
  (sec [this]
    {:deprecated "0.6.0"}
    (deprecated "sec is being deprecated in favor of second")
    (.getSecond this))
  (second [this] (.getSecond this))
  (milli [this] (* (.getNano this) 1000000))
  (nano [this] (.getNano this))
  (equal? [this ^ChronoLocalDateTime that] (.isEqual this that))
  (after? [this ^ChronoLocalDateTime that] (.isAfter this that))
  (before? [this ^ChronoLocalDateTime that] (.isBefore this that))
  (plus- [this ^TemporalAmount period] (.plus this period))
  (minus- [this ^TemporalAmount period] (.minus this period))
  (first-day-of-the-month- [this]
    (.with ^LocalDateTime this (TemporalAdjusters/firstDayOfMonth)))
  (last-day-of-the-month- [this]
     (.with ^LocalDateTime this (TemporalAdjusters/lastDayOfMonth)))
  (week-number-of-year [this]
    (.get this ChronoField/ALIGNED_WEEK_OF_YEAR))

  java.time.YearMonth
  (year [this] (.getYear this))
  (month [this] (.getMonthOfYear this))
  (equal? [this ^YearMonth that] (.isEqual this that))
  (after? [this ^YearMonth that] (.isAfter this that))
  (before? [this ^YearMonth that] (.isBefore this that))
  ;(plus- [this ^YearMonth period] (.plus this period))
  ;(minus- [this ^YearMonth period] (.minus this period))

  java.time.LocalDate
  (year [this] (.getYear this))
  (month [this] (.getMonthValue this))
  (day [this] (.getDayOfMonth this))
  (day-of-week [this] (.. this (getDayOfWeek) (getValue)))
  (equal? [this ^ChronoLocalDate that] (.isEqual this that))
  (after? [this ^ChronoLocalDate that] (.isAfter this that))
  (before? [this ^ChronoLocalDate that] (.isBefore this that))
  (plus- [this ^TemporalAmount period] (.plus this period))
  (minus- [this ^TemporalAmount period] (.minus this period))
  (first-day-of-the-month- [this]
    (.with ^LocalDate this (TemporalAdjusters/firstDayOfMonth)))
  (last-day-of-the-month- [this]
     (.with ^LocalDate this (TemporalAdjusters/lastDayOfMonth)))
  (week-number-of-year [this]
    (.get this ChronoField/ALIGNED_WEEK_OF_YEAR))

  java.time.LocalTime
  (hour [this] (.getHour this))
  (minute [this] (.getMinute this))
  (second [this] (.getSecond this))
  (milli [this] (* (.getNano this) 1000000))
  (nano [this] (.getNano this))
  (equal? [this ^LocalTime that] (.isEqual this that))
  (after? [this ^LocalTime that] (.isAfter this that))
  (before? [this ^LocalTime that] (.isBefore this that))
  (plus- [this ^TemporalAmount period] (.plus this period))
  (minus- [this ^TemporalAmount period] (.minus this period))
  )

; TODO Make TZ an atom that can be configured by the user for a default TZ for all operations
(def ^{:doc "ZoneId for UTC."}
      utc
  (ZoneId/of "Z"))

; TODO Potentially add some way to use millis or nanos if someone wants joda/clj-time compatibility

(defn now
  "Returns a DateTime for the current instant in the UTC time zone."
  []
  (ZonedDateTime/now utc))

(defn time-now
  "Returns a LocalTime for the current instant without date or time zone
  using ISOChronology in the current time zone."
  ([]
  (time-now utc))
  ([tz]
    (LocalTime/now tz)))

; Joda deprecated the Midnight stuff
;(defn today-at-midnight
  ;"Returns a ZonedDateTime for today at midnight in the UTC time zone."
  ;[]
  ; (.atStartOfDay (today) utc))

(defn epoch
  "Returns a DateTime for the begining of the Unix epoch in the UTC time zone."
  []
  (ZonedDateTime/ofInstant Instant/EPOCH utc))

; Midnight in Joda was deprecated and not recommended for use.
; Will re-add this feature if asked for
;(defn date-midnight
;  "Constructs and returns a new LocalDate in UTC.
;   Specify the year, month of year, day of month. Note that month and day are
;   1-indexed. Any number of least-significant components can be ommited, in which case
;   they will default to 1."
;  ([year]
;    (date-midnight year 1 1))
;  ([^long year ^long month]
;    (date-midnight year month 1))
;  ([^Long year ^Long month ^Long day]
;   (.atStartOfDay (LocalDate/of year month day) utc)))

(defn min-date
  "Minimum of the provided DateTimes."
  [dt & dts]
  (reduce #(if (before? %1 %2) %1 %2) dt dts))

(defn max-date
  "Maximum of the provided DateTimes."
  [dt & dts]
  (reduce #(if (after? %1 %2) %1 %2) dt dts))

(defn ^ZonedDateTime date-time
  "Constructs and returns a new ZonedDateTime in UTC.
   Specify the year, month of year, day of month, hour of day, minute of hour,
   second of minute, and millisecond of second. Note that month and day are
   1-indexed while hour, second, minute, and millis are 0-indexed.
   Any number of least-significant components can be ommited, in which case
   they will default to 1 or 0 as appropriate."
  ([year]
   (date-time year 1 1 0 0 0 0))
  ([year month]
   (date-time year month 1 0 0 0 0))
  ([year month day]
   (date-time year month day 0 0 0 0))
  ([year month day hour]
   (date-time year month day hour 0 0 0))
  ([year month day hour minute]
   (date-time year month day hour minute 0 0))
  ([year month day hour minute second]
   (date-time year month day hour minute second 0))
  ([^Integer year ^Integer month ^Integer day ^Integer hour
    ^Integer minute ^Integer second ^Integer nano]
   (ZonedDateTime/of year month day hour minute second nano ^ZoneId utc)))

(defn ^java.time.LocalDateTime local-date-time
  "Constructs and returns a new LocalDateTime.
   Specify the year, month of year, day of month, hour of day, minute of hour,
   second of minute, and millisecond of second. Note that month and day are
   1-indexed while hour, second, minute, and millis are 0-indexed.
   Any number of least-significant components can be ommited, in which case
   they will default to 1 or 0 as appropriate."
  ([year]
   (local-date-time year 1 1 0 0 0 0))
  ([year month]
   (local-date-time year month 1 0 0 0 0))
  ([year month day]
   (local-date-time year month day 0 0 0 0))
  ([year month day hour]
   (local-date-time year month day hour 0 0 0))
  ([year month day hour minute]
   (local-date-time year month day hour minute 0 0))
  ([year month day hour minute second]
   (local-date-time year month day hour minute second 0))
  ([^Integer year ^Integer month ^Integer day ^Integer hour
    ^Integer minute ^Integer second ^Integer millis]
   (LocalDateTime/of year month day hour minute second millis)))

(defn ^java.time.YearMonth year-month
  "Constructs and returns a new YearMonth.
   Specify the year and month of year. Month is 1-indexed and defaults
   to January (1)."
  ([year]
     (year-month year 1))
  ([^Integer year ^Integer month]
     (YearMonth/of year month)))

(defn ^java.time.LocalDate local-date
  "Constructs and returns a new LocalDate.
   Specify the year, month, and day. Does not deal with timezones."
  [^Integer year ^Integer month ^Integer day]
  (LocalDate/of year month day))

(defn ^org.joda.time.LocalTime local-time
  "Constructs and returns a new LocalTime.
   Specify the hour of day, minute of hour, second of minute, and millisecond of second.
   Any number of least-significant components can be ommited, in which case
   they will default to 1 or 0 as appropriate."
  ([hour]
   (local-time hour 0 0 0))
  ([hour minute]
   (local-time hour minute 0 0))
  ([hour minute second]
   (local-time hour minute second 0))
  ([^Integer hour ^Integer minute ^Integer second ^Integer nano]
   (LocalTime/of hour minute second nano))
  )

(defn ^java.time.LocalDate today
  "Constructs and returns a new LocalDate representing today's date.
   LocalDate objects do not deal with timezones at all."
  []
  (LocalDate/now utc))

(defn time-zone-for-offset
  "Returns a ZoneOffset for the given offset, specified either in hours or
   hours and minutes."
  ([hours]
   (ZoneOffset/ofHours hours))
  ([hours minutes]
   (ZoneOffset/ofHoursMinutes hours minutes)))

(defn time-zone-for-id
  "Returns a ZoneId for the given ID, which must be in long form, e.g.
   'America/Matamoros'."
  [^String id]
  (ZoneId/of id))

(defn available-ids
  "Returns a set of available IDs for use with time-zone-for-id."
  []
  (ZoneId/getAvailableZoneIds))

(defn default-time-zone
  "Returns the default ZoneId for the current environment."
  []
  (ZoneId/systemDefault))

(defn ^java.time.ZonedDateTime to-time-zone
  "Returns a new ZonedDateTime corresponding to the same absolute instant in time as
   the given ZonedDateTime, but with calendar fields corresponding to the given
   ZoneId."
  [^ZonedDateTime dt ^ZoneId tz]
  (.withZoneSameInstant dt tz))

(defn ^java.time.ZonedDateTime from-time-zone
  "Returns a new ZonedDateTime corresponding to the same point in calendar time as
   the given ZonedDateTime, but for a correspondingly different absolute instant in
   time."
  [^ZonedDateTime dt ^ZoneId tz]
  (.withZoneSameLocal dt tz))

(defn years
  "Given a number, returns a Duration representing that many years.
  Without an argument, returns a ChronoUnit representing only years."
  ([]
   (ChronoUnit/YEARS))
  ([^Integer n]
     (Period/ofYears n)))

(defn months
  "Given a number, returns a Duration representing that many months.
  Without an argument, returns a ChronoUnit representing only months."
  ([]
   (ChronoUnit/MONTHS))
  ([^Integer n]
     (Period/ofMonths n)))

(defn weeks
  "Given a number, returns a Duration representing that many weeks.
  Without an argument, returns a ChronoUnit representing only weeks."
  ([]
   (ChronoUnit/WEEKS))
  ([^Integer n]
     (Duration/ofDays (* n 7))))

(defn days
  "Given a number, returns a Duration representing that many days.
  Without an argument, returns a ChronoUnit representing only days."
  ([]
   (ChronoUnit/DAYS))
  ([^Integer n]
     (Duration/ofDays n)))

(defn hours
  "Given a number, returns a Duration representing that many hours.
  Without an argument, returns a ChronoUnit representing only hours."
  ([]
   (ChronoUnit/HOURS))
  ([^Integer n]
     (Duration/ofHours n)))

(defn minutes
  "Given a number, returns a Duration representing that many minutes.
  Without an argument, returns a ChronoUnit representing only minutes."
  ([]
   (ChronoUnit/MINUTES))
  ([^Integer n]
     (Duration/ofMinutes n)))

(defn seconds
  "Given a number, returns a Duration representing that many seconds.
   Without an argument, returns a ChronoUnit representing only seconds."
  ([]
     (ChronoUnit/SECONDS))
  ([^Integer n]
     (Duration/ofSeconds n)))

(defn millis
  "Given a number, returns a Duration representing that many milliseconds.
   Without an argument, returns a ChronoUnit representing only milliseconds."
  ([]
   (ChronoUnit/MILLIS))
  ([^Long n]
   (Duration/ofMillis n)))

(defn nanos
  "Given a number, returns a Duration representing that many nanoseconds.
   Without an argument, returns a ChronoUnit representing only nanoseconds."
  ([]
   (ChronoUnit/NANOS))
  ([^Long n]
   (Duration/ofNanos n)))



  ; Inverval does not exist nor has an equivalent in Java 8
(extend-protocol InTimeUnitProtocol
  ;java.time.temporal.TemporalAmount
  ;(in-millis [this] (.get this (millis)))
  ;(in-seconds [this] (.get this (seconds)))
  ;(in-minutes [this] (.get this (minutes)))
  ;(in-hours [this] (.get this (hours)))
  ;(in-days [this] (.get this (days)))
  ;(in-weeks [this] (.get this (weeks)))
  ;(in-months [this] (.get this (months)))
  ;(in-years [this] (.get this (years))))

  java.time.Duration
  (in-millis [this] (.toMillis this))
  (in-seconds [this] (.getSeconds this))
  (in-minutes [this] (.toMinutes this))
  (in-hours [this] (.toHours this))
  (in-days [this] (.toDays this))
  (in-weeks [this] (throw (UnsupportedOperationException.)))
  (in-months [this] (throw (UnsupportedOperationException.)))
  (in-years [this] (throw (UnsupportedOperationException.)))

  java.time.Period
  (in-days [this] (throw (UnsupportedOperationException.)))
  (in-weeks [this] (throw (UnsupportedOperationException.)))
  (in-months [this] (.toTotalMonths this))
  (in-years [this] (.. this (normalized) (getYears)))
  (in-seconds [this] (throw (UnsupportedOperationException.))))





(defn plus
  "Returns a new date/time corresponding to the given date/time moved forwards by
   the given Period(s)."
  ([dt ^TemporalAmount p]
     (plus- dt p))
  ([dt p & ps]
     (reduce plus- (plus- dt p) ps)))

(defn minus
  "Returns a new date/time object corresponding to the given date/time moved backwards by
   the given Period(s)."
  ([dt ^TemporalAmount p]
   (minus- dt p))
  ([dt p & ps]
     (reduce minus- (minus- dt p) ps)))

(defn ago
  "Returns a DateTime a supplied period before the present.
  e.g. (-> 5 years ago)"
  [^TemporalAmount period]
  (minus (now) period))

(defn yesterday
  "Returns a ZonedDateTime for yesterday relative to now"
  []
  (-> 1 days ago))

(defn from-now
  "Returns a DateTime a supplied period after the present.
  e.g. (-> 30 minutes from-now)"
  [^TemporalAmount period]
  (plus (now) period))

(defn tomorrow
  "Returns a ZonedDateTime for tomorrow relative to now"
  []
  (-> 1 days from-now))

(defn earliest
  "Returns the earliest of the supplied DateTimes"
  ([^Comparable dt1 ^Comparable dt2]
     (if (pos? (compare dt1 dt2)) dt2 dt1))
  ([dts]
     (reduce (fn [dt1 dt2]
               (if (pos? (compare dt1 dt2)) dt2 dt1)) dts)))

(defn latest
  "Returns the latest of the supplied DateTimes"
  ([^Comparable dt1 ^Comparable dt2]
     (if (neg? (compare dt1 dt2)) dt2 dt1))
  ([dts]
     (reduce (fn [dt1 dt2]
               (if (neg? (compare dt1 dt2)) dt2 dt1)) dts)))

;(defn interval
;  "Returns an interval representing the span between the two given ReadableDateTimes.
;   Note that intervals are closed on the left and open on the right."
;  [^ReadableDateTime dt-a ^ReadableDateTime dt-b]
;  (Interval. dt-a dt-b))

;(defn start
;  "Returns the start DateTime of an Interval."
;  [^Interval in]
;  (.getStart in))
;
;(defn end
;  "Returns the end DateTime of an Interval."
;  [^Interval in]
;  (.getEnd in))

;(defn extend
;  "Returns an Interval with an end ReadableDateTime the specified Period after the end
;   of the given Interval"
;  [^Interval in & by]
;  (.withEnd in (apply plus (end in) by)))


;(defn within?
;  "With 2 arguments: Returns true if the given Interval contains the given
;   ReadableDateTime. Note that if the ReadableDateTime is exactly equal to the
;   end of the interval, this function returns false.
;   With 3 arguments: Returns true if the start ReadablePartial is
;   equal to or before and the end ReadablePartial is equal to or after the test
;   ReadablePartial."
;  ([^Interval i ^ReadableDateTime dt]
;     (.contains i dt))
;  ([^ReadablePartial start ^ReadablePartial end ^ReadablePartial test]
;     (or (equal? start test)
;         (equal? end test)
;         (and (before? start test) (after? end test)))))
;
;(defn overlaps?
;  "With 2 arguments: Returns true of the two given Intervals overlap.
;   Note that intervals that satisfy abuts? do not satisfy overlaps?
;   With 4 arguments: Returns true if the range specified by start-a and end-a
;   overlaps with the range specified by start-b and end-b."
;  ([^Interval i-a ^Interval i-b]
;     (.overlaps i-a i-b))
;  ([^ReadablePartial start-a ^ReadablePartial end-a
;    ^ReadablePartial start-b ^ReadablePartial end-b]
;     (or (and (before? start-b end-a) (after? end-b start-a))
;         (and (after? end-b start-a) (before? start-b end-a))
;         (or (equal? start-a end-b) (equal? start-b end-a)))))
;
;(defn overlap
;  "Returns an Interval representing the overlap of the specified Intervals.
;   Returns nil if the Intervals do not overlap.
;   The first argument must not be nil.
;   If the second argument is nil then the overlap of the first argument
;   and a zero duration interval with both start and end times equal to the
;   current time is returned."
;  [^Interval i-a ^Interval i-b]
;     ;; joda-time AbstractInterval.overlaps:
;     ;;    null argument means a zero length interval 'now'.
;     (cond (nil? i-b) (let [n (now)] (overlap i-a (interval n n)))
;           (.overlaps i-a i-b) (interval (latest (start i-a) (start i-b))
;                                         (earliest (end i-a) (end i-b)))
;           :else nil))
;
;(defn abuts?
;  "Returns true if Interval i-a abuts i-b, i.e. then end of i-a is exactly the
;   beginning of i-b."
;  [^Interval i-a ^Interval i-b]
;  (.abuts i-a i-b))


(defn first-day-of-the-month
  ([^long year ^long month]
     (first-day-of-the-month- (date-time year month)))
  ([dt]
     (first-day-of-the-month- dt)))

(defn last-day-of-the-month
  ([^long year ^long month]
     (last-day-of-the-month- (date-time year month)))
  ([dt]
     (last-day-of-the-month- dt)))

(defn number-of-days-in-the-month
  (^long [^ZonedDateTime dt]
         (day (last-day-of-the-month- dt)))
  (^long [^long year ^long month]
         (day (last-day-of-the-month- (date-time year month)))))


(defn do-at*
  "Creates a Clock that replaces the utc definition to call overloaded
   methods in the library that will take a Clock instead of a ZoneId."
  [^ZonedDateTime base-date-time body-fn]
    (with-redefs [utc (proxy [java.time.Clock] []
                        (getZone [] (ZoneId/of "Z"))
                        (instant [] (.toInstant base-date-time))
                        (withZone [zone-id] utc))]
      (body-fn)))

(defmacro do-at
  "Like clojure.core/do except evalautes the expression at the given date-time"
  [^ZonedDateTime base-date-time & body]
  `(do-at* ~base-date-time
    (fn [] ~@body)))


(defn ^ZonedDateTime floor
  "Floors the given date-time dt to the given time unit dt-fn,
  e.g. (floor (now) hour) returns (now) for all units
  up to and including the hour"
  ([^ZonedDateTime dt dt-fn]
	 (let [dt-fns [year month day hour minute second nano]]
	 	(apply date-time
	 		(map apply
				(concat (take-while (partial not= dt-fn) dt-fns) [dt-fn])
				(repeat [dt]))))))


(defn ^ZonedDateTime today-at
  ([^long hours ^long minutes ^long seconds ^long nanos]
   (let [dt (floor (now) day)]
     (-> dt
         (.plusHours hours)
         (.plusMinutes minutes)
         (.plusSeconds seconds)
         (.plusNanos nanos))))
  ([^long hours ^long minutes ^long seconds]
   (today-at hours minutes seconds 0))
  ([^long hours ^long minutes]
   (today-at hours minutes 0))
  ([^long hours]
   (today-at hours 0)))
