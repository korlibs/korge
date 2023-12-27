package korlibs.datastructure

import kotlin.test.assertEquals

class PoolTest {
	class Demo {
		var x: Int = 0
		var y: Int = 0
	}

	@kotlin.test.Test
	fun name() {
		var totalResetCount = 0
		var totalAllocCount = 0
		val pool = Pool({
			totalResetCount++
			it.x = 0
			it.y = 0
		}) {
			totalAllocCount++
			Demo()
		}
		val a = pool.alloc()
		val b = pool.alloc()
		val c = pool.alloc()
		assertEquals(0, pool.itemsInPool)
		pool.free(c)
		assertEquals(1, pool.itemsInPool)
		pool.free(b)
		assertEquals(2, pool.itemsInPool)
		pool.free(a)
		assertEquals(3, pool.itemsInPool)
		val d = pool.alloc()
		assertEquals(2, pool.itemsInPool)
		pool.free(d)

		pool.alloc {
			assertEquals(2, pool.itemsInPool)
		}
		assertEquals(3, pool.itemsInPool)

		assertEquals(5, totalResetCount) // Number of resets
		assertEquals(3, totalAllocCount) // Number of allocs
	}
}
