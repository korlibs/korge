package com.soywiz.korge.gradle.util

import java.io.File

fun File.ensureParents() = this.apply { parentFile.mkdirs() }
fun <T> File.conditionally(ifNotExists: Boolean = true, block: File.() -> T): T? = if (!ifNotExists || !this.exists()) block() else null
operator fun File.get(name: String) = File(this, name)
