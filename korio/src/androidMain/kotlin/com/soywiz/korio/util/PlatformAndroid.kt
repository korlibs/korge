package com.soywiz.korio.util

import com.soywiz.korio.BuildConfig

internal actual val rawPlatformName: String = "jvm-android"
internal actual val rawOsName: String by lazy { System.getProperty("os.name") }
internal actual val rawIsDebug: Boolean = BuildConfig.DEBUG
