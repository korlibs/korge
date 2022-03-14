package com.github.quillraven.fleks.collection

import kotlin.test.*

internal class BitArrayTest {
    @Test
    fun createEmptyBitArray() {
        val bits = BitArray(0)

        assertEquals(0, bits.length())
        assertEquals(0, bits.capacity)
    }

    @Test
    fun setBitAtIndex3WithSufficientCapacity() {
        val bits = BitArray(3)

        bits.set(2)

        assertEquals(3, bits.length())
        assertEquals(64, bits.capacity)
        assertTrue { bits[2] }
    }

    @Test
    fun setBitAtIndex3WithInsufficientCapacity() {
        val bits = BitArray(0)

        bits.set(2)

        assertEquals(3, bits.length())
        assertEquals(64, bits.capacity)
        assertTrue { bits[2] }
    }

    @Test
    fun getBitOfOutOfBoundsIndex() {
        val bits = BitArray(0)

        assertFalse(bits[64])
    }

    @Test
    fun clearAllSetBits() {
        val bits = BitArray()
        bits.set(2)
        bits.set(4)

        bits.clearAll()

        assertEquals(0, bits.length())
    }

    @Test
    fun clearSpecificBit() {
        val bits = BitArray()
        bits.set(2)

        bits.clear(2)

        assertEquals(0, bits.length())
    }

    @Test
    fun twoBitArraysIntersectWhenTheyHaveAtLeastOneBitSetAtTheSameIndex() {
        val bitsA = BitArray(256)
        val bitsB = BitArray(1)
        bitsA.set(2)
        bitsA.set(4)
        bitsA.set(6)
        bitsB.set(4)

        val actualA = bitsA.intersects(bitsB)
        val actualB = bitsB.intersects(bitsA)

        assertTrue(actualA)
        assertTrue(actualB)
    }

    @Test
    fun twoBitArraysDoNotIntersectWhenTheyDoNotHaveAtLeastOneBitSetAtTheSameIndex() {
        val bitsA = BitArray(256)
        val bitsB = BitArray(1)
        bitsA.set(2)
        bitsA.set(4)
        bitsA.set(6)
        bitsB.set(3)

        val actualA = bitsA.intersects(bitsB)
        val actualB = bitsB.intersects(bitsA)

        assertFalse(actualA)
        assertFalse(actualB)
    }

    @Test
    fun bitArrayContainsBitArrayIfTheSameBitsAreSet() {
        val bitsA = BitArray(256)
        val bitsB = BitArray(1)
        bitsA.set(2)
        bitsA.set(4)
        bitsB.set(2)
        bitsB.set(4)

        val actualA = bitsA.contains(bitsB)
        val actualB = bitsB.contains(bitsA)

        assertTrue(actualA)
        assertTrue(actualB)
    }

    @Test
    fun bitArrayDoesNotContainBitArrayIfDifferentBitsAreSet() {
        val bitsA = BitArray(256)
        val bitsB = BitArray(1)
        bitsA.set(2)
        bitsA.set(4)
        bitsB.set(2)
        bitsB.set(3)

        val actualA = bitsA.contains(bitsB)
        val actualB = bitsB.contains(bitsA)

        assertFalse(actualA)
        assertFalse(actualB)
    }

    @Test
    fun runActionForEachSetBit() {
        val bits = BitArray(128)
        bits.set(3)
        bits.set(5)
        bits.set(117)
        var numCalls = 0
        val bitsCalled = mutableListOf<Int>()

        bits.forEachSetBit {
            ++numCalls
            bitsCalled.add(it)
        }

        assertEquals(3, numCalls)
        assertEquals(listOf(117, 5, 3), bitsCalled)
    }

    @Test
    fun bitArrayEqualsToIncompatibleTypeObject() {
        val bits = BitArray(42)
        bits.set(3)
        val otherBits = 42

        assertEquals(false, bits.equals(otherBits))
        assertEquals(false, bits.equals(null))
    }

    @Test
    fun bitArrayEqualsToSameObject() {
        val bits = BitArray(42)
        bits.set(3)

        assertEquals(true, bits == bits)
    }

    @Test
    fun bitArrayEqualsToOtherBitArray() {
        val bits = BitArray(42)
        bits.set(4)
        val otherBits = BitArray(44)
        otherBits.set(4)
        val bits42 = BitArray(42)
        bits42.set(4)

        assertEquals(true, bits == otherBits, "bitArray equals to another bitArray with different size")
        assertEquals(true, bits == bits42, "bitArray equals to another bitArray with equal size")

        otherBits.set(7)
        bits42.set(7)
        assertEquals(false, bits == otherBits, "bitArray equals not to another bitArray with different size")
        assertEquals(false, bits == bits42, "bitArray equals not to another bitArray with equal size")
    }
}
