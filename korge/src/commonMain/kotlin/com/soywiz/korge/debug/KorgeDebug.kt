package com.soywiz.korge.debug

interface KorgeDebugNode {
    fun getDebugMethods(): Map<String, () -> Unit> = mapOf()
}
