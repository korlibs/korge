package com.soywiz.korio.net.http

import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.toString
import com.soywiz.korio.stream.readAll

suspend fun HttpBodyContent.toDebugString(): String {
    return "$contentType\n${this.createAsyncStream().readAll().toString(UTF8)}"
}
