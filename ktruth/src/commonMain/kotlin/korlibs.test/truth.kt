package korlibs.test

import kotlin.test.*

fun assertThat(actual: Double) = DoubleSubject(actual)
fun assertThat(actual: Boolean) = BooleanSubject(actual)
fun <T: Any> assertThat(actual: T) = AnySubject(actual)
fun <T: Any> assertThat(actual: Collection<T>) = CollectionSubject(actual)
inline fun <reified E: Any, reified V: Any> assertThat(actual: Map<E, V>) = MapSubject(actual)

fun testAssertFailsWithMessage(expectedErrorMessage: String, block: () -> Unit) {
    try {
        block()
    } catch (assertionError: AssertionError) {
        assertEquals(expectedErrorMessage, assertionError.message)
    }
}