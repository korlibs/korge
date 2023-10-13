package korlibs.io.lang

import kotlin.reflect.*

expect val <T : Any> KClass<T>.portableSimpleName: String
expect val <T : Any> KClass<T>.portableQualifiedName: String
