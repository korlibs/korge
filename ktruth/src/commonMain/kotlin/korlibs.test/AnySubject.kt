package korlibs.test

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.assertEquals
import kotlin.test.assertIs

open class AnySubject<T : Any>(val subject: T) {
    fun isEqualTo(expected: T) {
        assertEquals(expected, subject)
    }

    inline fun <reified E : Any> isInstanceOf(): E {
        return isInstanceOf<E>(subject)
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <reified E : Any> isInstanceOf(value: Any?): E {
        contract { returns() implies (value is E) }
        assertIs<E>(subject)
        return subject as E
    }
}
