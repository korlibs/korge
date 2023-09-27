package korlibs.korge.resources

import korlibs.inject.Injector
import kotlin.reflect.KClass

data class VPath(val path: String)

suspend fun <T : Any> Injector.getPath(clazz: KClass<T>, path: String): T = getWith(clazz, VPath(path))
suspend inline fun <reified T : Any> Injector.getPath(path: String): T = getPath(T::class, path)
