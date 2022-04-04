package com.soywiz.korio.net.http

import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

suspend fun HttpBodyContent.toDebugString(): String {
    return "$contentType\n${this.createAsyncStream().readAll().toString(UTF8)}"
}
