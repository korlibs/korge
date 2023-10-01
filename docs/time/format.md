---
permalink: /time/format/
layout: default
group: time
title: "Time Format"
title_short: Format
description: "Formatting, parsing and localization"
fa-icon: fa-globe
priority: 60
---

Klock has utilities for formatting, parsing and localizing dates and times.

## Localization

Starting with Klock 1.3.0, there is support for multiple languages.
English support works out of the box, but to prevent adding growing the size of the library without DCE/Tree shaking,
there is a separate artifact called `klock-locale` that add support for additional languages.
Each language is added via an extension method to the `KlockLocale` companion object.

### Month localized names

```kotlin

val name = Month.February.localName(KlockLocale.japanese) // "2月"
val name = Month.February.localName(KlockLocale.spanish) // "febrero"
val name = Month.February.localShortName(KlockLocale.spanish) // "feb"
```

### Day of week localized names

```kotlin
val name = DayOfWeek.Monday.localName(KlockLocale.japanese) // "月曜日"
val name = DayOfWeek.Monday.localShortName(KlockLocale.japanese) // "月"
```

### Formating dates in a specific language

```kotlin
val date = DateTime(year = 2019, month = Month.March, day = 13, hour = 21, minute = 36, second = 45, milliseconds = 512)
val locale = KlockLocale.spanish

// A generic format with a specific locale
date.toString(DateFormat.DEFAULT_FORMAT.withLocale(locale)) // Mié, 13 Mar 2019 21:36:45 UTC

// Locale-specific formats
locale.formatDateTimeMedium.format(date) // 13/03/2019 21:36:45
locale.formatDateTimeShort.format(date) // 13/03/19 21:36
locale.formatDateFull.format(date) // Miércoles, 13 de Marzo de 2019
locale.formatDateLong.format(date) // 13 de Marzo de 2019
locale.formatDateMedium.format(date) // 13/03/2019
locale.formatDateShort.format(date) // 13/03/19
locale.formatTimeMedium.format(date) // 21:36:45
locale.formatTimeShort.format(date) // 21:36
```

## ISO8601 (serialization and parsing)

ISO-86601 is a date, time and interval serialization specification defined here: <https://en.wikipedia.org/wiki/ISO_8601>.

### Serializing dates

```kotlin
val date = DateTime(2019, Month.April, 14)
assertEquals("2019-04-14", date.format(ISO8601.DATE_CALENDAR_COMPLETE))
assertEquals("2019-04-14", date.format(ISO8601.DATE_CALENDAR_COMPLETE.extended))
assertEquals("20190414", date.format(ISO8601.DATE_CALENDAR_COMPLETE.basic))
```

### Serializing times

```kotlin
val time = 15.hours + 30.minutes + 12.seconds
assertEquals("15:30:12", ISO8601.TIME_LOCAL_COMPLETE.format(time))
assertEquals("153012", ISO8601.TIME_LOCAL_COMPLETE.basic.format(time))
```

### Serializing intervals

```kotlin
val time = 1.years + 0.months + 27.days + 11.hours + 9.minutes + 11.seconds
assertEquals("P1Y0M27DT11H9M11S", ISO8601.INTERVAL_COMPLETE0.format(time))
```
