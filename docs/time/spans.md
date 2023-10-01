---
permalink: /time/spans/
layout: default
group: time
title: "Time Spans"
title_short: Spans
description: "TimeSpan, MonthSpan, DateTimeSpan, DateTimeRange..."
fa-icon: fa-hourglass-half
priority: 30
---

Klock has utilities for representing spans of time, dates, and months.

## TimeSpan

Klock offers a `TimeSpan` inline class using a Double to be allocation-free on all targets, and it serves to represent durations without start references.
It has millisecond precision up to `2 ** 52`, which means that it can represent up to **142808 years** with millisecond precision.
It has a special `TimeSpan.NULL` value (internally represented as NaN) to represent an absence of time without having to use nullable types that are not allocation-free.

### Constructing instances

There are extension properties for `Number` to generate `TimeSpan` instances. The extensions use `Number`, but are inline, so no allocation is done.

```kotlin
val time = 1_000_000_000.nanoseconds
val time = 1_000_000.microseconds
val time = 1_000.milliseconds
val time = 1.seconds
val time = 0.5.seconds
val time = 60.minutes
val time = 24.hours
val time = 1.days
val time = 1.weeks
```

You can represent from nanoseconds to weeks. Months and years are not included here but included as part of `MonthSpan` since months and years work different because leap years.

### Arithmetic

```kotlin
val time = 4.seconds
val doubleTheTime = time * 2
val negatingTime = -time
val twoHundredMillisecondsMore = time + 200.milliseconds
```

### External Arithmetic

Adding or subtracting time to a date

```kotlin
val now = DateTime.now()
val inTenSeconds = now + 10.seconds
```

### Comparison

`TimeSpan` implements `Comparable<TimeSpan>` so you can compare times independently to the unit used to instantiate them:

```kotlin
val isTrue = 4001.milliseconds > 4.seconds
```

### Converting between units

`TimeSpan` has several properties to get the instance time interpreted in different units of measure:

```kotlin
val value: Double = 1.seconds.nanoseconds // 1.seconds as nanoseconds (1_000_000_000)
val value: Double = 1.seconds.microseconds // 1.seconds as microseconds (1_000_000)
val value: Double = 1.seconds.milliseconds // 1.seconds as milliseconds (1000)
val value: Double = 1.seconds.seconds // 1.seconds as seconds (1)
val value: Double = 1.seconds.minutes // 1.seconds as minutes (1.0/60)
val value: Double = 1.seconds.hours // 1.seconds as hours (1.0/3_600)
val value: Double = 1.seconds.days // 1.seconds as days (1.0/86_4000)
```

For milliseconds there are a couple of additional properties to get it as Long and Int:

```kotlin
val value: Long = 1.seconds.millisecondsLong // 1.seconds as milliseconds (1000L)
val value: Int  = 1.seconds.millisecondsInt  // 1.seconds as milliseconds (1000)
```

## MonthSpan

MonthSpan allows to represent `month` and `year` durations (with month precission) where `TimeSpan` simply can't work because month distance depends on specific moments to have into account leap years.

### Constructing instances

```kotlin
val time = 1.months
val time = 5.years
```

### Arithmetic

Adding or subtracting month-based spans

```kotlin
val time: MonthSpan = 5.years + 2.months
val time: MonthSpan = 5.years * 2
val time: DateTimeSpan = 5.years + 5.days
```

### External Arithmetic

Adding or subtracting months to a date

```kotlin
val now = DateTime.now()
val inTwoMonths = now + 2.months
```

### Components

```kotlin
val time = 5.years + 2.months + 4.months

val years : Int = time.years  // 5
val months: Int = time.months // 6

val totalYears : Double = time.totalYears  // 5.0
val totalMonths: Int    = time.totalMonths // 5 * 12 + 6 = 66
```

## DateTimeSpan

DateTimeSpan is a combination of `MonthSpan` and `TimeSpan`.

This class is not inline, so whenever it is possible use `MonthSpan` or `TimeSpan` to alter `DateTime` directly.

## DateTimeRange

DateTimeRange is a range between two DateTime.

### Constructing Instances

```kotlin
val today = DateTime.now()
val tomorrow = DateTime.now() + 1.days

val rangeOpen = today until tomorrow
val rangeClosed = today .. tomorrow
```

### Contains

```kotlin
val inTenMinutes = now + 10.minutes
val contains: Boolean = inTenMinutes in rangeOpen
```

### Span and Duration

```kotlin
val duration: TimeSpan     = rangeOpen.duration
val span    : DateTimeSpan = rangeOpen.span
```

### Days Between two DateTime

```kotlin
val inFourMonths = today + 4.month
val days = (today until inFourMonths).span.days
``` 

## DateTimeRangeSet

The DateTimeRangeSet represents a non-overlapping sets of DateTimeRange.
It can be used to measure availability ranges among other things.

### Constructing Instances

```kotlin
val set: DateTimeRangeSet = DateTimeRangeSet(dateTimeRange1, dateTimeRange2, dateTimeRange3)
```

### Combining two instances (addition/union)

It will combine all the ranges and generate a non-overlapping instance

```kotlin
val set = dateTimeRangeSet1 + dateTimeRangeSet2
```

### Substracting two time ranges

```kotlin
val set = dateTimeRangeSet1 - dateTimeRangeSet2
```

### Intersection between two ranges

```kotlin
val set = dateTimeRangeSet1.intersection(dateTimeRangeSet2)
```
