package korlibs.io.lang

import korlibs.io.*
import kotlin.reflect.*

actual val <T : Any> KClass<T>.portableSimpleName: String get() = simpleName ?: "unknown"
