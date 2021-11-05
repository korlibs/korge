package com.soywiz.kmem

public actual val KmemGC: KmemGCImpl = object : KmemGCImpl() {
    public override fun collect() = System.gc()
    public override fun collectCyclic() = System.gc()
}
