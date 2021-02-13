package com.soywiz.korio.time

import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korio.lang.*

@PublishedApi
internal val traceTimes by lazy { Environment["TRACE_TIMES"] == "true" }

inline fun <T : Any> traceTime(name: String, block: () -> T): T {
    lateinit var result: T
    val time = measureTime {
        result = block()
    }
    if (traceTimes) {
        Console.info("$name loaded in $time")
    }
    return result
}
