package com.soywiz.korio.lang

import kotlin.reflect.KClass

expect val <T : Any> KClass<T>.portableSimpleName: String
