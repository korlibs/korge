package com.soywiz.korfl

import com.soywiz.korio.ds.IntArrayList
import com.soywiz.korio.ds.binarySearch
import com.soywiz.korio.util.clamp


// @TODO: Move to Korio?
open class Timed<T> {
	val times = IntArrayList()
	val objects = arrayListOf<T>()
	val size: Int get() = times.size

	fun add(time: Int, obj: T) {
		times.add(time)
		objects.add(obj)
	}

	fun findNearIndex(time: Int): Int {
		val res = times.binarySearch(time)
		return if (res < 0) -res - 1 else res
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
}
