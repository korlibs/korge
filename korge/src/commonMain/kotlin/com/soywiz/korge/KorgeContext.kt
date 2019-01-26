package com.soywiz.korge

expect suspend fun <T> withKorgeContext(context: Any?, callback: suspend () -> T): T

