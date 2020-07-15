package org.jbox2d.internal

import kotlin.browser.*
import kotlin.js.*

//actual fun System_nanoTime(): Long = window.performance.now().toLong() // @TODO: node.js: https://nodejs.org/api/process.html#process_process_hrtime_time
actual fun System_nanoTime(): Long = (Date.now().toLong() * 1000000L)
