package com.soywiz.korge.stat

class Stats {
	interface Named {
		val name: String
	}

	class Counter(override val name: String) : Named {
		var frameCount = 0.0; private set
		var countThisFrame = 0; private set
		var totalCount = 0.0; private set
		val avgCountPerFrame get() = totalCount / frameCount

		fun increment(count: Int = 1) {
			countThisFrame += count
		}

		fun startFrame() {
			totalCount += countThisFrame
			frameCount++
			countThisFrame = 0
		}

		override fun toString(): String =
			"Counter($name): [frames=$frameCount, totalCount=$totalCount, countThisFrame=$countThisFrame, avgCountPerFrame=$avgCountPerFrame]"
	}

	inner class Value(override val name: String) : Named {
		var value: Any? = null

		fun startFrame() {

		}

		fun set(value: Any?) {
			this.value = value
		}

		override fun toString(): String =
			"Value($name): $value"
	}

	fun startFrame() {
		for (c in counters.list) c.startFrame()
		for (c in values.list) c.startFrame()
	}

	class RCollection<T : Named> {
		val list: ArrayList<T> = ArrayList<T>()
		val byName = LinkedHashMap<String, T>()

		fun add(item: T) {
			list += item
			byName[item.name] = item
		}

		inline fun getOrPut(name: String, callback: () -> T): T {
			return byName.getOrPut(name) {
				callback().apply {
					list += this
				}
			}
		}
	}

	val counters = RCollection<Counter>()
	val values = RCollection<Value>()


	fun counter(name: String): Counter = counters.getOrPut(name) { Counter(name) }
	fun value(name: String): Value = values.getOrPut(name) { Value(name) }
	fun dump() {
		println("Counters:")
		for (counter in counters.list) {
			println(" - $counter")
		}
		println("Values:")
		for (value in values.list) {
			println(" - $value")
		}
	}
}
