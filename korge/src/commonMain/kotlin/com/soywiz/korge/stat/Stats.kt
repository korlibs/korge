package com.soywiz.korge.stat

import com.soywiz.kds.iterators.*

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
		counters.list.fastForEach { it.startFrame() }
		values.list.fastForEach { it.startFrame() }
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
		counters.list.fastForEach { counter ->
			println(" - $counter")
		}
		println("Values:")
		values.list.fastForEach { value ->
			println(" - $value")
		}
	}
}
