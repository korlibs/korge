package com.soywiz.kproject.model

sealed interface KPPlugin {
    companion object
}

data class GradlePlugin(val name: String) : KPPlugin

fun KPPlugin.Companion.parseString(str: String): KPPlugin {
    return GradlePlugin(str)
}

fun KPPlugin.Companion.parseObject(any: Any?): KPPlugin {
    return when (any) {
        is String -> parseString(any)
        else -> TODO("Unsupported plugin $any")
    }
}
