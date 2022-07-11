<p align="center">
    <img alt="Klock" src="https://raw.githubusercontent.com/korlibs/korlibs-logos/master/256/klock.png" />
</p>

<p align="center">
    Klock is a Date & Time library for Multiplatform Kotlin.
</p>

<p align="center">
    It is designed to be as allocation-free as possible using Kotlin inline classes,
    to be consistent and portable across targets since all the code is written in Common Kotlin,
    and to provide an API that is powerful, fun and easy to use.
</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/com.soywiz.korlibs.klock/klock"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/com.soywiz.korlibs.klock/klock"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord" /></a>
</p>
<!-- /BADGES -->

### Full Documentation: <https://docs.korge.org/klock/>

### Some samples:

```kotlin
val now = DateTime.now()
val duration = 1.seconds
val later = now + 1.months + duration
val is2018Leap = Year(2018).isLeap
val daysInCurrentMonth = now.yearMonth.days
val daysInNextMonth = (now.yearMonth + 1.months).days
```

### Usage with gradle:

```groovy
def klockVersion = "..." // Find latest version in https://search.maven.org/artifact/com.soywiz.korlibs.klock/klock

repositories {
    mavenCentral()
}

// For multiplatform Kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation "com.soywiz.korlibs.klock:klock:$klockVersion" // Common 
            }
        }
    }
}

// For JVM
dependencies {
    implementation "com.soywiz.korlibs.klock:klock-jvm:$klockVersion"
}
```

### Testing & Kotest

Kotest is a flexible and comprehensive testing tool for Kotlin with multiplatform support.
It supports Klock adding additional matchers. For a full list of Klock Kotest matchers, check this link:
<https://kotest.io/docs/assertions/matchers.html>

And you can find a sample here: <https://github.com/kotest/kotest/tree/master/kotest-assertions/kotest-assertions-klock>
