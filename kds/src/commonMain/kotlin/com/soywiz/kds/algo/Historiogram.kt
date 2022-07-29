package com.soywiz.kds.algo

import com.soywiz.kds.IntIntMap

object Historiogram {
    fun values(array: IntArray, out: IntIntMap = IntIntMap()): IntIntMap {
        for (v in array) {
            out.getOrPut(v) { 0 }
            out[v]++
        }
        return out
    }
}
