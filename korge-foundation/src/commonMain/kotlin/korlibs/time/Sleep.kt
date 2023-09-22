package korlibs.time

import korlibs.time.internal.*

/** Sleeps the thread during the specified time. Spinlocks on JS */
@ExperimentalStdlibApi
fun blockingSleep(time: TimeSpan) = KlockInternal.sleep(time)
