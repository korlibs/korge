package com.soywiz.korge.animate

import com.soywiz.korio.util.clamp
import com.soywiz.korma.ds.IntArrayList
import com.soywiz.korma.ds.binarySearch

open class Timed<T>(initialCapacity: Int = 7) {
	val times = IntArrayList(initialCapacity)
	val objects = java.util.ArrayList<T>(initialCapacity)
	val size: Int get() = times.size

	val entries: List<Pair<Int, T>> get() = times.zip(objects)

	fun add(time: Int, obj: T) {
		times.add(time)
		objects.add(obj)

		// Insert in order
		var m = times.size - 2
		while (m >= 0 && time < times[m]) {
			swap(m, m + 1)
			m--
		}
	}

	private fun swap(a: Int, b: Int) {
		val tempTime = this.times[b]
		val tempObject = this.objects[b]
		this.times[b] = this.times[a]
		this.objects[b] = this.objects[a]
		this.times[a] = tempTime
		this.objects[a] = tempObject
	}

	fun findNearIndex(time: Int): Int {
		val res = times.binarySearch(time)
		return if (res < 0) (-res - 1).clamp(0, times.size - 1) else res
	}

	data class RangeResult(var startIndex: Int = 0, var endIndex: Int = 0)

	fun getRangeIndices(startTime: Int, endTime: Int, out: RangeResult = RangeResult()): RangeResult {
		val startIndex = (findNearIndex(startTime) - 1).clamp(0, size - 1)
		val endIndex = (findNearIndex(endTime) + 1).clamp(0, size - 1)
		var min = Int.MAX_VALUE
		var max = Int.MIN_VALUE
		for (n in startIndex..endIndex) {
			val time = times[n]
			//if (time in (startTime + 1)..endTime) {
			if (time in startTime..endTime) {
				min = Math.min(min, n)
				max = Math.max(max, n)
			}
		}
		out.startIndex = min
		out.endIndex = max
		return out
	}

	inline fun forEachInRange(startTime: Int, endTime: Int, maxCalls: Int = Int.MAX_VALUE, callback: (index: Int, time: Int, left: T) -> Unit) {
		val startIndex = (findNearIndex(startTime) - 1).clamp(0, size - 1)
		val endIndex = (findNearIndex(endTime) + 1).clamp(0, size - 1)
		var totalCalls = 0
		for (n in startIndex..endIndex) {
			val time = times[n]
			val obj = objects[n]
			if (time in (startTime + 1)..endTime) {
				callback(n, time, obj)
				totalCalls++
				if (totalCalls >= maxCalls) break
			}
		}
	}

	data class Result<T>(
		var index: Int = 0,
		var left: T? = null,
		var right: T? = null,
		var ratio: Double = 0.0
	)

	fun find(time: Int, out: Result<T> = Result()): Result<T> {
		return findAndHandle(time) { index, left, right, ratio ->
			out.index = index
			out.left = left
			out.right = right
			out.ratio = ratio
			out
		}
	}

	inline fun <TR> findAndHandle(time: Int, callback: (index: Int, left: T?, right: T?, ratio: Double) -> TR): TR {
		if (objects.isEmpty()) return callback(0, null, null, 0.0)
		val index = findNearIndex(time)
		val timeAtIndex = times[index]

		if (time < timeAtIndex && index <= 0) {
			return callback(0, null, objects[0], 0.0)
		} else {
			val idx = if (time < timeAtIndex) index - 1 else index
			val curTimeAtIndex = times[idx + 0]
			if (curTimeAtIndex == time) {
				return callback(idx, objects[idx], null, 0.0)
			} else {
				if (idx >= times.size - 1) {
					return callback(objects.size, objects[objects.size - 1], null, 1.0)
				} else {
					val nextTimeAtIndex = times[idx + 1]
					val elapsedTime = (time - curTimeAtIndex).toDouble()
					val totalTime = (nextTimeAtIndex - curTimeAtIndex).toDouble()
					return callback(idx, objects[idx], objects[idx + 1], elapsedTime / totalTime)
				}
			}
		}
	}

	fun findWithoutInterpolation(time: Int, out: Result<T> = Result()): Result<T> {
		return findAndHandleWithoutInterpolation(time) { index, left ->
			out.index = index
			out.left = left
			out.right = null
			out.ratio = 0.0
			out
		}
	}

	inline fun <TR> findAndHandleWithoutInterpolation(time: Int, callback: (index: Int, left: T?) -> TR): TR {
		if (objects.isEmpty()) return callback(0, null)
		val index = findNearIndex(time)
		val timeAtIndex = times[index]
		if (time < timeAtIndex && index <= 0) {
			return callback(0, null)
		} else {
			val idx = if (time < timeAtIndex) index - 1 else index
			val curTimeAtIndex = times[idx + 0]
			if (curTimeAtIndex == time) {
				return callback(idx, objects[idx])
			} else {
				if (idx >= times.size - 1) {
					return callback(objects.size, objects[objects.size - 1])
				} else {
					return callback(idx, objects[idx])
				}
			}
		}
	}

	override fun toString(): String = "Timed($entries)"
}
