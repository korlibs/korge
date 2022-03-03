package com.soywiz.korio.util

import com.soywiz.korio.*

internal actual val rawPlatformName: String = "native"
internal actual val rawOsName: String = "$nativeOsfamilyName$nativeArchName"
