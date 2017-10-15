package com.soywiz.korge.resources

import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.Language
import kotlin.reflect.KClass

annotation class Path(
	@Language("File Reference")
	val path: String
)

data class VPath(val path: String)

suspend fun <T : Any> AsyncInjector.getPath(clazz: KClass<T>, path: String): T = getWith(clazz, VPath(path))
suspend inline fun <reified T : Any> AsyncInjector.getPath(path: String): T = getPath(T::class, path)
