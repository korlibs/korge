package com.soywiz.korio.lang

inline fun assert(cond: Boolean): Unit {
	if (!cond) throw AssertionError()
}

inline fun assert(cond: Boolean, message: () -> String): Unit {
    if (!cond) throw AssertionError(message())
}

object Assert {
    inline fun <T> eq(expect: T, actual: T): Unit {
        assert(expect == actual) { "Expected $expect to be $actual" }
    }
}

