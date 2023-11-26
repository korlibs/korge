package korlibs.time

import korlibs.time.internal.*
import kotlin.time.*

/**
 * Class for measuring relative times with as much precision as possible.
 */
object PerformanceCounter {
    /**
     * Returns a performance counter measure in nanoseconds.
     */
    val nanoseconds: Double get() = KlockInternal.now.nanoseconds

    /**
     * Returns a performance counter measure in microseconds.
     */
    val microseconds: Double get() = KlockInternal.now.microseconds

    /**
     * Returns a performance counter measure in milliseconds.
     */
    val milliseconds: Double get() = KlockInternal.now.milliseconds

    /**
     * Returns a performance counter as a [Duration].
     */
    val reference: Duration get() = KlockInternal.now
}
