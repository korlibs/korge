package com.soywiz.korio.lang

actual val platformCharsetProvider: CharsetProvider = CharsetProvider { normalizedName, _ -> null }
