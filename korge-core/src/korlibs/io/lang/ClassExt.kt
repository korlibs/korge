package korlibs.io.lang

import kotlin.reflect.KClass

expect val <T : Any> KClass<T>.portableSimpleName: String
