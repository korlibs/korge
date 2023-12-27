package korlibs.datastructure

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PriorityQueueTest {
    @Test
    fun test() {
        val pq = PriorityQueue<Int>()
        pq.add(10)
        pq.add(15)
        pq.add(5)
        assertEquals(3, pq.size); assertEquals(5, pq.removeHead())
        assertEquals(2, pq.size); assertEquals(10, pq.removeHead())
        assertEquals(1, pq.size); assertEquals(15, pq.removeHead())
        assertEquals(0, pq.size)
    }

    @Test
    fun testInt() {
        val pq = IntPriorityQueue()
        pq.add(10)
        pq.add(15)
        pq.add(5)
        assertEquals(3, pq.size); assertEquals(5, pq.removeHead())
        assertEquals(2, pq.size); assertEquals(10, pq.removeHead())
        assertEquals(1, pq.size); assertEquals(15, pq.removeHead())
        assertEquals(0, pq.size)
    }

    @Test
    fun test2() {
        data class Demo(val value: Long)

        val pq = PriorityQueue<Demo>(Comparator { a, b -> a.value.compareTo(b.value) })

        val a = Demo(10)
        val b = Demo(15)
        val c = Demo(5)

        pq.add(a)
        pq.add(b)
        pq.add(c)
        assertEquals(3, pq.size); assertEquals(c, pq.removeHead())
        assertEquals(2, pq.size); assertEquals(a, pq.removeHead())
        assertEquals(1, pq.size); assertEquals(b, pq.removeHead())
        assertEquals(0, pq.size)
    }

    @Test
    fun test3() {
        val pq = IntPriorityQueue { a, b -> (-a).compareTo(-b) }
        pq.addAll(listOf(1, 2, 3, 4))
        assertEquals(listOf(4, 3, 2, 1), pq.toList())
    }

    @Test
    fun test4() {
        class WI(val v: Int)
        val pq = PriorityQueue<WI> { a, b -> (-a.v).compareTo(-b.v) }
        pq.addAll(listOf(1, 2, 3, 4).map { WI(it) })
        assertEquals(listOf(4, 3, 2, 1), pq.map { it.v })
    }

    @Test
    fun test5() {
        data class WI(var v: Int) {
            override fun toString(): String = "$v"
        }
        val pq = PriorityQueue<WI> { a, b -> (a.v).compareTo(b.v) }
        val a = WI(10)
        val b = WI(20)
        val c = WI(30)
        val d = WI(40)
        pq.addAll(listOf(a, b, c, d))
        b.v = 35
        pq.updateObject(b)
        assertEquals("[10, 30, 35, 40]", pq.toArraySorted().toList().toString())
        b.v = 15
        pq.updateObject(b)
        assertEquals("[10, 15, 30, 40]", pq.toArraySorted().toList().toString())
        b.v = 0
        pq.updateObject(b)
        assertEquals("[0, 10, 30, 40]", pq.toArraySorted().toList().toString())
        b.v = 41
        pq.updateObject(b)
        assertEquals("[10, 30, 40, 41]", pq.toArraySorted().toList().toString())
    }

    @Test
    fun test6() {
        data class WI(var v: Int) {
            override fun toString(): String = "$v"
        }

        fun List<WI>.checkInOrder(): Int {
            for (n in 1 until this.size) {
                if (this[n].v < this[n - 1].v) return n
            }
            return -1
        }

        val pq = PriorityQueue<WI> { a, b -> (a.v).compareTo(b.v) }
        val rand = Random(0L)
        val instances = (0 until 8).map { WI(rand.nextInt()) }
        pq.addAll(instances)
        for (n in 0 until 1024) {
            val instance = instances[rand.nextInt(0, instances.size)]
            instance.v = rand.nextInt()
            pq.updateObject(instance)

            val sorted = pq.toArraySorted().toList()
            assertEquals(-1, sorted.checkInOrder(), message = "$sorted")
        }
    }


    // Items inserted on setUp.
    private val o1 = QueueItem("o1", 1)
    private val o2 = QueueItem("o2", -5)
    private val o3 = QueueItem("o3", 7)
    private val o4 = QueueItem("o4", 4)
    private val o5 = QueueItem("o5", 9)
    private val o6 = QueueItem("o6", 0)

    private val pq = PriorityQueue<QueueItem> { l, r -> l.priority.compareTo(r.priority) }.apply {
        addAll(arrayOf(o1, o2, o3, o4, o5, o6))
    }

    // Items not inserted on setUp.
    private val n1 = QueueItem("n1", 3)

    @Test
    fun testInitialOrder() {
        assertEquals("o2", pq.head.toString())
        assertEquals("[o2, o6, o1, o4, o3, o5]", pq.toArraySorted().toList().toString())
    }

    @Test
    fun testUpdateOrder() {
        o3.priority = -6
        pq.updateObject(o3)
        assertEquals("o3", pq.head.toString())
        assertEquals("[o3, o2, o6, o1, o4, o5]", pq.toArraySorted().toList().toString())
    }

    @Test
    fun testPostUpdateInsert() {
        o3.priority = -6
        pq.updateObject(o3)
        pq.add(n1)
        assertEquals("[o3, o2, o6, o1, n1, o4, o5]", pq.toArraySorted().toList().toString())
    }

    @Test
    fun testContains() {
        assertTrue(pq.contains(o4))
        assertFalse(pq.contains(n1))
    }

    internal class QueueItem(var name: String, var priority: Int) {
        override fun toString() = name
    }
}
