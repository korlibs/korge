package com.soywiz.kmem

expect val KmemGC: KmemGCImpl

open class KmemGCImpl {
    open fun collect() {
    }

    open fun collectCyclic() {
    }

    open fun suspend() {
    }

    open fun resume() {
    }

    open fun stop() {
    }

    open fun start() {
    }

    open var threshold: Int = 0
    open var thresholdAllocations: Long = 0L
    open var autotune: Boolean = true
    open var cyclicCollectorEnabled: Boolean = true
}
