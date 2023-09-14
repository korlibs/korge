package korlibs.memory

import kotlin.native.internal.GC

public actual val KmemGC: KmemGCImpl = object : KmemGCImpl() {
    override fun collect() = GC.collect()
    override fun collectCyclic() = GC.collect() // Available since 1.4?
    override fun suspend() = GC.suspend()
    override fun resume() = GC.resume()
    override fun stop() = GC.stop()
    override fun start() = GC.start()

    override var threshold: Int
        get() = GC.threshold
        set(value) { GC.threshold = value }
    override var thresholdAllocations: Long
        get() = GC.thresholdAllocations
        set(value) { GC.thresholdAllocations = value }
    override var autotune: Boolean
        get() = GC.autotune
        set(value) { GC.autotune = value }
    // Available since 1.4?
    override var cyclicCollectorEnabled: Boolean
        get() = true
        set(value) { }
}
