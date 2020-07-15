package com.soywiz.korio.lang

import kotlin.reflect.*

expect val <T : Any> KClass<T>.portableSimpleName: String
