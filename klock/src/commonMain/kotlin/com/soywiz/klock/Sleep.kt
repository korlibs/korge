package com.soywiz.klock

import com.soywiz.klock.hr.HRTimeSpan
import com.soywiz.klock.hr.hr
import com.soywiz.klock.internal.KlockInternal

/** Sleeps the thread during the specified time. Spinlocks on JS */
fun blockingSleep(time: HRTimeSpan) = KlockInternal.sleep(time)

/** Sleeps the thread during the specified time. Spinlocks on JS */
fun blockingSleep(time: TimeSpan) = KlockInternal.sleep(time.hr)
