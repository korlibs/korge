package com.soywiz.korio.util

internal actual val rawPlatformName: String = "jvm"
internal actual val rawOsName: String by lazy { System.getProperty("os.name") }
