package com.soywiz.kds.algo

import com.soywiz.kds.IntIntMap
import com.soywiz.kds.lock.NonRecursiveLock

class Historiogram(private val out: IntIntMap = IntIntMap()) {
    private val lock = NonRecursiveLock()
    fun add(value: Int) {
        lock {
            out.getOrPut(value) { 0 }
            out[value]++
        }
    }

    fun getMapCopy(): IntIntMap {
        val map = IntIntMap()
        lock {
            out.fastForEach { key, value -> map[key] = value }
        }
        return out
    }

    override fun toString(): String {
        return lock {
            "Historiogram(${getMapCopy().toMap()})"
        }
    }

    companion object {
        fun values(array: IntArray, out: Historiogram = Historiogram()): IntIntMap {
            for (v in array) out.add(v)
            return out.out
        }
    }
}
