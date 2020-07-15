package com.soywiz.korio.lang

import com.soywiz.korio.*
import kotlin.reflect.*

actual val <T : Any> KClass<T>.portableSimpleName: String get() = simpleName ?: "unknown"
