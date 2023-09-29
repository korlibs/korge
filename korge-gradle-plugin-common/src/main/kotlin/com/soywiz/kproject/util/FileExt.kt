package com.soywiz.kproject.util

import java.io.*

fun File.writeTextIfNew(text: String) {
    parentFile.mkdirs()
    if (takeIf { it.exists() }?.readText() != text) {
        writeText(text)
    }
}
