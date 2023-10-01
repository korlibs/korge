---
permalink: /time/dates/
layout: default
group: time
title: "Dates"
title_short: Dates
description: "DateTime, Date, DayOfWeek, Month, Year, YearMonth, TimeZone..."
fa-icon: fa-calendar-alt
priority: 20
---

To represent instants with date and time information, there are two classes: `DateTime` and `DateTimeTz`.

* `DateTime` class is used to represent instants in UTC time. This class is `inline` and it is represented internally as a `Double` in a way that it is allocation-free on all targets including JS.
* `DateTimeTz` class is used to represent instants with an offset in a TimeZone. It includes a `DateTime` and a offset. And it is different from `DateTime` itself.

Since 1.7.0:

* `Date` class represent the Year+Month+Day part of an instant
* `Time` class represent the Hour+Minute+Second+Millisecond part of an instant

## Current Time

```kotlin
val utc = DateTime.now()
val local = DateTimeTz.nowLocal()
```

## Unix Timestamp

To get the current UTC Unix TimeStamp:

```kotlin
val unix = DateTime.now().unixMillis
val unix = DateTime.nowUnix()
```

To construct a UTC date from an Unix TimeStamp:

```kotlin
val date = DateTime.fromUnix(unix)
```

## Components

```kotlin
val time = DateTime.now()

val year: Year = time.year
val year: Int = time.yearInt

val month: Month = time.month
val month0: Int = time.month0
val month1: Int = time.month1

val yearMonth: YearMonth = time.yearMonth
val dayOfMonth: Int = time.dayOfMonth

val dayOfWeek: DayOfWeek = time.dayOfWeek
val dayOfWeek: Int = time.dayOfWeekInt

val dayOfYear: Int = time.dayOfYear

val hours: Int = time.hours
val minutes: Int = time.minutes
val seconds: Int = time.seconds
val milliseconds: Int = time.milliseconds
```

## From DateTime to Date & Time

Since 1.7.0:

```kotlin
val date: Date = dateTime.date
val time: Time = dateTime.time
```

## From DateTime to DateTimeTz

```kotlin
time.localUnadjusted
time.toOffsetUnadjusted(offset: TimezoneOffset)

time.local
time.toOffset(offset: TimezoneOffset)
```

## Formating and Parsing Dates

The `DateFormat` interface allows to parse and format dates from/to Strings.

```kotlin
val dateFormat: DateFormat = DateFormat("EEE, dd MMM yyyy HH:mm:ss z") // Construct a new DateFormat from a String
val date: DateTimeTz = dateFormat.parse("Sat, 08 Sep 2018 04:08:09 UTC") // Parse a Date from a String
val dateStr: String = DateTime.now().format(dateFormat) // Format a Date using a specific DateFormat.
```

## TimeZones

As for 1.0 Klock doesn't have direct TimeZone support. But support offseted DateTime using `DateTimeTz`.

What Klock allows to do here is to get the UTC offset of the operating system TimeZone in a specific moment (having into account daylight changes when supported by the OS).

## Date Information

Klock allows to get Date information: from how many days has a month, to whether a year is leap, to which month will be in three years and a six months.
This UTC offset is represented by the class `TimezoneOffset` that just wraps the `TimeSpan` class.

### DayOfWeek enum

DayOfWeek is an enum with all seven days of the week:
`Sunday(0)`, `Monday(1)`, `Tuesday(2)`, `Wednesday(3)`, `Thursday(4)`, `Friday(5)`, `Saturday(6)`

* Constructing a DayOfWeek from an integer where Sunday=0: `DayOfWeek[index0]`
* Getting index0 (sunday=0) and index1 (sunday=1) representations: `dayOfWeek.index0`, `dayOfWeek.index1`

### Month enum

Month is an enum with all twelve months on it:
`January`, `February`, `March`, `April`, `May`, `June`, `July`, `August`, `September`, `October`, `November`, `December`.

Months are a set of 28-31 days. The number of days of each month is always the same, except for `February` that has 28 days in normal years, and 29 in leap years.

* Getting next and previous month (cyclic): `month.next`, `month.prev`
* Getting the number of days in a common and leap year: `month.daysCommon`, `month.daysLeap`
* Getting the number of days for a specific year or a leap year: `month.days(year)`, `month.days(leap = true)`
* Getting a Month from a one-based representation (where January is 1 and December is 12), while wrapping outside numbers: `Month(month1)`
* Getting the month zero-based (January=0) and one-based (January=1) representation: `month.index0`, `month.index1`
* Add or subtract months: `month + 11`

### Year class

The `Year` class represents a normal Year and it is an inline class. It supports from year 1 to 9999 where the leap year formulas are valid.

* Construct a Year: `Year(2018)`
* Get the number of days of a specific year: `Year(2018).days`
* Determine if a year is leap: `Year(2018).isLeap`
* How many days have passed since year 1 to the beginning of a specific year: `Year(2018).daysSinceOne`
* Construct a year using the number of days since year 1: `Year.fromDays(730_000)`

### YearMonth class

The `YearMonth` class is an inline class representing a pair of `Year` and `Month`. This pair has information about the number of days in a month
and can be useful to represent calendars.

* The current YearMonth: `DateTime.now().yearMonth`
* Number of days in a YearMonth: `yearMonth.days`
* Number of days since the start of the year to reach the beginning of the month: `yearMonth.daysToStart`
* Number of days since the start of the year to reach the end of the month: `yearMonth.daysToEnd`
* Get the components year and the month: `yearMonth.year` and `yearMonth.month`
* Add or subtract months of years: `yearMonth - 1.years + 1.months`
