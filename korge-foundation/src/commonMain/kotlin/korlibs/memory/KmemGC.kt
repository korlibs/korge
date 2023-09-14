package korlibs.memory

public expect val KmemGC: KmemGCImpl

public open class KmemGCImpl {
    public open fun collect() {
    }

    public open fun collectCyclic() {
    }

    public open fun suspend() {
    }

    public open fun resume() {
    }

    public open fun stop() {
    }

    public open fun start() {
    }

    public open var threshold: Int = 0
    public open var thresholdAllocations: Long = 0L
    public open var autotune: Boolean = true
    public open var cyclicCollectorEnabled: Boolean = true
}
