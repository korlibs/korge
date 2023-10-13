package korlibs.io.lang

import kotlin.reflect.*

actual val <T : Any> KClass<T>.portableSimpleName: String get() = simpleName ?: "unknown"
actual val <T : Any> KClass<T>.portableQualifiedName: String get() = qualifiedName ?: portableSimpleName
