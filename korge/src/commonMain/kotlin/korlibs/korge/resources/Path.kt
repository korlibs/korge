package korlibs.korge.resources

import korlibs.inject.AsyncInjector
import kotlin.reflect.KClass

data class VPath(val path: String)

suspend fun <T : Any> AsyncInjector.getPath(clazz: KClass<T>, path: String): T = getWith(clazz, VPath(path))
suspend inline fun <reified T : Any> AsyncInjector.getPath(path: String): T = getPath(T::class, path)
