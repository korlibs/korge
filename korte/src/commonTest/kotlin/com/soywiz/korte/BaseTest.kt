package com.soywiz.korte

import kotlin.test.*

open class BaseTest {

}

inline fun <reified T> expectException(message: String, callback: () -> Unit) {
    var exception: Throwable? = null
    try {
        callback()
    } catch (e: Throwable) {
        exception = e
    }
    val e = exception
    if (e != null) {
        if (e is T) {
            assertEquals(message, e.message)
        } else {
            throw e
        }
    } else {
        assertTrue(false, "Expected ${T::class} with message '$message'")
    }
}
