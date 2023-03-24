package korlibs.test

import kotlin.reflect.*
import kotlin.test.*

class MapSubject<K : Any, V : Any>(
    val keyType: KClass<out K>,
    val valueType: KClass<out V>,
    val actual: Map<K, V>
) {
    fun containsExactly(vararg elements: Any) {
        assertTrue(elements.size % 2 == 0, "Received an odd number of elements!")

        val expectedKeys = mutableSetOf<K>()

        val expectedPairs = elements.toList().windowed(2, 2) {

            assertTrue(keyType.isInstance(it[0]),
                "Provided expected key is not the same instance of the key type: ${it[0]}")
            assertTrue(valueType.isInstance(it[1]),
                "Provided expected value is not the same instance of the value type: ${it[1]}")

            val key = it[0] as K
            val value = it[1] as V

            assertFalse(expectedKeys.contains(key), "You provided the same key twice! key: $key")

            expectedKeys.add(key)

            key to value
        }

        expectedPairs.forEach { (expectedKey, expectedValue) ->
            assertTrue(actual.contains(expectedKey), "actual does not contain key: $expectedKey")
            assertEquals(
                expectedValue, actual[expectedKey], "key: `$expectedKey`"
            )
        }

        val unexpectedKeysInActual = actual.keys - expectedKeys
        assertTrue(unexpectedKeysInActual.isEmpty(),
            "actual contains more keys than expected. keys: $unexpectedKeysInActual")
    }

    companion object {
        inline operator fun <reified K : Any, reified V : Any> invoke(actual: Map<K, V>) =
            MapSubject(
                K::class,
                V::class,
                actual
            )
    }

}