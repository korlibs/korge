---
permalink: /time/measure/
layout: default
group: time
title: "Time Measure"
title_short: Measure
description: "measureTime, measureTimeWithResult & PerformanceCounter"
fa-icon: fa-ruler-horizontal
priority: 40
---

Klock has utilities for mesuring time.

## Measuring Time

As for Klock 1.0, there are two relevant functionality: the `measureTime`, `measureTimeWithResult` functions and the `PerformanceCounter` class.

### measureTime

This function is inline and allocation-free, and can be used for expensive computations as well as for asynchronous blocks:

```kotlin
val time: TimeSpan = measureTime {
    // expensive or asynchronous computation
}
```

### measureTimeWithResult

This function is inline but it allocates a TimedResult instance, so it is not suitable for critical places, but allows to return a result along the time:

```kotlin
val timedResult: TimedResult<String> = measureTimeWithResult {
    // expensive or asynchronous computation
    "result"
}
val time: TimeSpan = timedResult.time
val result: String = timedResult.result
```

### PerformanceCounter

This class offers a performance counter that will increase over time but that cannot be used as reference in time. Only can be used as relative time to compute deltas:

```kotlin
val start: Double = PerformanceCounter.microseconds
// ...
val end: Double = PerformanceCounter.microseconds
val elapsed: TimeSpan = (end - start).microseconds 
```
