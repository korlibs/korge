package com.soywiz.korte.util

interface AsyncTextWriterContainer {
    suspend fun write(writer: suspend (String) -> Unit)
}
