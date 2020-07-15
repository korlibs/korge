package com.soywiz.korte.internal

internal val invalidOp: Nothing get() = throw RuntimeException()
internal fun invalidOp(msg: String): Nothing = throw RuntimeException(msg)
