package korlibs.template

import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
