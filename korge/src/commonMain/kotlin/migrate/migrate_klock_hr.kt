@file:Suppress(
    "PackageDirectoryMismatch", "EXPERIMENTAL_API_USAGE", "unused", "UNUSED_PARAMETER",
    "RedundantVisibilityModifier", "SpellCheckingInspection"
)

package com.soywiz.klock.hr

import com.soywiz.klock.TimeSpan

/**
 * Migrate from version 1.15.1 to 2.0.0
 */
@Deprecated(
    message = "Need migrate to milliseconds",
    replaceWith = ReplaceWith("this.milliseconds", "com.soywiz.klock.milliseconds"),
    level = DeprecationLevel.ERROR
)
public inline val Int.hrMilliseconds: TimeSpan
    get() {
        throw Error("migrate to milliseconds")
    }

/**
 * Migrate from version 1.15.1 to 2.0.0
 */
@Deprecated(
    message = "Need migrate to milliseconds",
    replaceWith = ReplaceWith("this.milliseconds", "com.soywiz.klock.milliseconds"),
    level = DeprecationLevel.ERROR
)
inline val Long.hrMilliseconds: TimeSpan
    get() {
        throw Error("migrate to milliseconds")
    }

/**
 * Migrate from version 1.15.1 to 2.0.0
 */
@Deprecated(
    message = "Need migrate to milliseconds",
    replaceWith = ReplaceWith("this.milliseconds", "com.soywiz.klock.milliseconds"),
    level = DeprecationLevel.ERROR
)
public inline val Float.hrMilliseconds: TimeSpan
    get() {
        throw Error("migrate to milliseconds")
    }

/**
 * Migrate from version 1.15.1 to 2.0.0
 */
@Deprecated(
    message = "Need migrate to milliseconds",
    replaceWith = ReplaceWith("this.milliseconds", "com.soywiz.klock.milliseconds"),
    level = DeprecationLevel.ERROR
)
public inline val Double.hrMilliseconds: TimeSpan
    get() {
        throw Error("migrate to milliseconds")
    }
