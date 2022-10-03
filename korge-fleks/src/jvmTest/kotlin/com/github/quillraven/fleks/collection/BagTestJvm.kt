package com.github.quillraven.fleks.collection

import kotlin.test.Test
import kotlin.test.assertFailsWith

class GenericBagTestJvm {
    @Test
    fun cannotGetStringValueOfInvalidOutOfBoundsIndex() {
        val bag = bag<String>(2)

        assertFailsWith<IndexOutOfBoundsException> { bag[2] }
    }
}

class IntBagTestJvm {
    @Test
    fun addValueUnsafeWithInsufficientCapacity() {
        val bag = IntBag(0)

        assertFailsWith<IndexOutOfBoundsException> { bag.unsafeAdd(42) }
    }

    @Test
    fun cannotGetValueOfOutOfBoundsIndex() {
        val bag = IntBag(2)

        assertFailsWith<IndexOutOfBoundsException> { bag[2] }
    }
}
