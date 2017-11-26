package com.soywiz.korge.lang

import com.soywiz.korge.native.KorgeNative
import kotlin.reflect.KClass

val <T : Any> KClass<T>.portableSimpleName: String get() = KorgeNative.getClassSimpleName(this)
