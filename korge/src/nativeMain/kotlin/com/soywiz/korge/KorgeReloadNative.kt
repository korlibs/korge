package com.soywiz.korge

import kotlin.reflect.KClass

internal actual fun <T : Any> KorgeReload_getReloadedClass(clazz: KClass<T>, context: ReloadClassContext): KClass<T> {
    return clazz
}
