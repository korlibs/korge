package com.github.quillraven.fleks.collection

import kotlin.test.*

class GenericBagTest {
    @Test
    fun createEmptyBagOfStringOfSize32() {
        val bag = bag<String>(32)

        assertEquals(32, bag.capacity)
        assertEquals(0, bag.size)
    }

    @Test
    fun addStringToBag() {
        val bag = bag<String>()

        bag.add("42")

        assertEquals(1, bag.size)
        assertTrue("42" in bag)
    }

    @Test
    fun removeExistingStringFromBag() {
        val bag = bag<String>()
        bag.add("42")

        val expected = bag.removeValue("42")

        assertEquals(0, bag.size)
        assertFalse { "42" in bag }
        assertTrue(expected)
    }

    @Test
    fun removeNonExistingStringFromBag() {
        val bag = bag<String>()

        val expected = bag.removeValue("42")

        assertFalse(expected)
    }

    @Test
    fun setStringValueAtIndexWithSufficientCapacity() {
        val bag = bag<String>()

        bag[3] = "42"

        assertEquals(4, bag.size)
        assertEquals("42", bag[3])
    }

    @Test
    fun setStringValueAtIndexWithInsufficientCapacity() {
        val bag = bag<String>(2)

        bag[2] = "42"

        assertEquals(3, bag.size)
        assertEquals("42", bag[2])
        assertEquals(3, bag.capacity)
    }

    @Test
    fun addStringToBagWithInsufficientCapacity() {
        val bag = bag<String>(0)

        bag.add("42")

        assertEquals(1, bag.size)
        assertEquals("42", bag[0])
        assertEquals(1, bag.capacity)
    }

    @Test
    fun cannotGetStringValueOfInvalidInBoundsIndex() {
        val bag = bag<String>()

        assertFailsWith<NoSuchElementException> { bag[0] }
    }

    @Test
    fun executeActionForEachValueOfStringBag() {
        val bag = bag<String>(4)
        bag[1] = "42"
        bag[2] = "43"
        var numCalls = 0
        val valuesCalled = mutableListOf<String>()

        bag.forEach {
            ++numCalls
            valuesCalled.add(it)
        }

        assertEquals(2, numCalls)
        assertEquals(listOf("42", "43"), valuesCalled)
    }
}

class IntBagTest {
    @Test
    fun createEmptyBagOfStringOfSize32() {
        val bag = IntBag(32)

        assertEquals(32, bag.capacity)
        assertEquals(0, bag.size)
    }

    @Test
    fun addValueToBag() {
        val bag = IntBag()

        bag.add(42)

        assertTrue(bag.isNotEmpty)
        assertEquals(1, bag.size)
        assertTrue(42 in bag)
    }

    @Test
    fun clearAllValuesFromBag() {
        val bag = IntBag()
        bag.add(42)
        bag.add(43)

        bag.clear()

        assertEquals(0, bag.size)
        assertFalse { 42 in bag }
        assertFalse { 43 in bag }
    }

    @Test
    fun addValueUnsafeWithSufficientCapacity() {
        val bag = IntBag(1)

        bag.unsafeAdd(42)

        assertTrue(42 in bag)
    }

    @Test
    fun addValueToBagWithInsufficientCapacity() {
        val bag = IntBag(0)

        bag.add(42)

        assertEquals(1, bag.size)
        assertEquals(42, bag[0])
        assertEquals(1, bag.capacity)
    }

    @Test
    fun doNotResizeWhenBagHasSufficientCapacity() {
        val bag = IntBag(8)

        bag.ensureCapacity(7)

        assertEquals(8, bag.capacity)
    }

    @Test
    fun resizeWhenBagHasInsufficientCapacity() {
        val bag = IntBag(8)

        bag.ensureCapacity(9)

        assertEquals(10, bag.capacity)
    }

    @Test
    fun executeActionForEachValueOfBag() {
        val bag = IntBag(4)
        bag.add(42)
        bag.add(43)
        var numCalls = 0
        val valuesCalled = mutableListOf<Int>()

        bag.forEach {
            ++numCalls
            valuesCalled.add(it)
        }


        assertEquals(2, numCalls)
        assertEquals(listOf(42, 43), valuesCalled)
    }

    @Test
    fun sortValuesByNormalIntComparisonWithSizeLessThan7() {
        val bag = IntBag()
        repeat(6) { bag.add(6 - it) }

        bag.sort(compareEntity { e1, e2 -> e1.id.compareTo(e2.id) })

        repeat(6) {
            assertEquals(it + 1, bag[it])
        }
    }

    @Test
    fun sortValuesByNormalIntComparisonWithSizeLessThan50ButGreater7() {
        val bag = IntBag()
        repeat(8) { bag.add(8 - it) }

        bag.sort(compareEntity { e1, e2 -> e1.id.compareTo(e2.id) })

        repeat(8) {
            assertEquals(it + 1, bag[it])
        }
    }

    @Test
    fun sortValuesByNormalIntComparisonWithSizeGreater50() {
        val bag = IntBag()
        repeat(51) { bag.add(51 - it) }

        bag.sort(compareEntity { e1, e2 -> e1.id.compareTo(e2.id) })

        repeat(51) {
            assertEquals(it + 1, bag[it])
        }
    }
}
