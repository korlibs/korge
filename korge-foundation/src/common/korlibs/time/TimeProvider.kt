package korlibs.time

/** Class to provide time that can be overridden to mock or change its behaviour. */
interface TimeProvider {
    /** Returns a [DateTime] for this provider. */
    fun now(): DateTime

    companion object : TimeProvider {
        override fun now(): DateTime = DateTime.now()

        /** Constructs a [TimeProvider] from a [callback] producing a [DateTime]. */
        operator fun invoke(callback: () -> DateTime) = object : TimeProvider {
            override fun now(): DateTime = callback()
        }
    }
}

inline fun TimeProvider.measure(block: () -> Unit): TimeSpan {
    val start = now()
    block()
    val end = now()
    return end - start
}
