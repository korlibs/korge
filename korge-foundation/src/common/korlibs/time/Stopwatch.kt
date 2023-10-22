package korlibs.time

class Stopwatch(val nanosecondProvider: () -> Double = { PerformanceCounter.nanoseconds }) {
    constructor(timeProvider: TimeProvider) : this({ timeProvider.now().unixMillis.milliseconds.nanoseconds })
    private var running = false
    private var startNano = 0.0
    private val currentNano get() = nanosecondProvider()
    private fun setStart() { startNano = currentNano }
    init {
        setStart()
    }
    fun start() = this.apply {
        setStart()
        running = true
    }
    fun restart() = start()
    fun stop() = this.apply {
        startNano = elapsedNanoseconds
        running = false
    }
    val elapsedNanoseconds get() = if (running) currentNano - startNano else startNano
    val elapsedMicroseconds get() = elapsedNanoseconds * 1000
    val elapsed: TimeSpan get() = elapsedNanoseconds.nanoseconds
    fun getElapsedAndRestart(): TimeSpan = elapsed.also { restart() }
}
