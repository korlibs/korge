package com.soywiz.korio.util.i18n

import com.soywiz.korio.*
import com.soywiz.korio.lang.Environment
import kotlinx.browser.*

internal actual val systemLanguageStrings: List<String> by lazy { jsRuntime.langs() }
