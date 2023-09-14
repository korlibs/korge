package korlibs.io.lang

import kotlin.reflect.*

actual val <T : Any> KClass<T>.portableSimpleName: String get() = java.simpleName
