package korlibs.time

import korlibs.time.hr.HRTimeSpan
import korlibs.time.hr.hr
import korlibs.time.internal.KlockInternal

/** Sleeps the thread during the specified time. Spinlocks on JS */
fun blockingSleep(time: HRTimeSpan) = KlockInternal.sleep(time)

/** Sleeps the thread during the specified time. Spinlocks on JS */
fun blockingSleep(time: TimeSpan) = KlockInternal.sleep(time.hr)
