package com.soywiz.kmem

public actual val KmemGC: KmemGCImpl = object : KmemGCImpl() {
    override fun collect() = System.gc()
    override fun collectCyclic() = System.gc()
}
