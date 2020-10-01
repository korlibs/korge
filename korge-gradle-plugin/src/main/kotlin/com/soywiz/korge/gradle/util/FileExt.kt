package com.soywiz.korge.gradle.util

import java.io.File
import java.nio.charset.*

fun File.ensureParents() = this.apply { parentFile.mkdirs() }
fun <T> File.conditionally(ifNotExists: Boolean = true, block: File.() -> T): T? = if (!ifNotExists || !this.exists()) block() else null
fun <T> File.always(block: File.() -> T): T = block()
operator fun File.get(name: String) = File(this, name)
fun File.writeTextIfChanged(text: String, charset: Charset = Charsets.UTF_8) {
    val originalText = this.takeIf { it.exists() }?.readText(charset)
    if (originalText != text) {
        writeText(text, charset)
    }
}

fun File.writeBytesIfChanged(bytes: ByteArray) {
    val originalBytes = this.takeIf { it.exists() }?.readBytes()
    if (originalBytes == null || !bytes.contentEquals(originalBytes)) {
        writeBytes(bytes)
    }
}
