package com.soywiz.korge.native

import kotlin.reflect.KClass

expect object KorgeNative {
	fun getClassSimpleName(clazz: KClass<*>): String
}
