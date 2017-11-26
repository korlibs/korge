package com.soywiz.korge.native

import kotlin.reflect.KClass

actual object KorgeNative {
	actual fun getClassSimpleName(clazz: KClass<*>): String = clazz.simpleName ?: "unknown"
}
