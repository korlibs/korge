package com.soywiz.korio.util

import com.soywiz.korio.*

internal actual val rawOsName: String by lazy { jsRuntime.rawOsName }
internal actual val rawPlatformName: String by lazy { jsRuntime.rawPlatformName }
