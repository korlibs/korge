package com.soywiz.korio.util

internal actual val rawPlatformName: String = "jvm-android"
internal actual val rawOsName: String by lazy { System.getProperty("os.name") }
