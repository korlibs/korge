package korlibs.io.lang

import kotlin.reflect.*

actual val <T : Any> KClass<T>.portableSimpleName: String get() = java.simpleName
actual val <T : Any> KClass<T>.portableQualifiedName: String get() = qualifiedName ?: java.canonicalName.replace("/", ".")
