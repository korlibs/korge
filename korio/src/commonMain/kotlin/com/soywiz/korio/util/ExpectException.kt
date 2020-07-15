package com.soywiz.korio.util

import com.soywiz.korio.lang.*
import kotlin.reflect.*

suspend fun <T : Throwable> expectException(clazz: KClass<T>, callback: suspend () -> Unit) {
	var thrown: Throwable? = null
	try {
		callback()
	} catch (e: Throwable) {
		thrown = e
	}
	if (thrown == null || thrown::class != clazz) {
		throw ExpectedException(clazz, thrown)
	}
}

suspend inline fun <reified T : Throwable> expectException(noinline callback: suspend () -> Unit) = expectException(T::class, callback)

class ExpectedException(val expectedClass: KClass<*>, val found: Throwable?)
	: Exception(if (found != null) "Expected ${expectedClass.portableSimpleName} but found $found" else "Expected ${expectedClass.portableSimpleName} no exception was thrown")

// @TODO: Kotlin.JS BUG!
//inline fun <reified T : Throwable> expectException(callback: () -> Unit) {
//	var thrown: Throwable? = null
//	try {
//		callback()
//	} catch (e: Throwable) {
//		thrown = e
//	}
//	if (thrown == null || thrown !is T) {
//		throw ExpectedException(T::class, thrown)
//	}
//}

