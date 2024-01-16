@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.lock

import korlibs.time.*
import korlibs.concurrent.lock.waitPrecise as waitPreciseConcurrent
import korlibs.concurrent.lock.wait as waitConcurrent

@Deprecated("Use korlibs.concurrent.lock package")
typealias BaseLock = korlibs.concurrent.lock.BaseLock
@Deprecated("Use korlibs.concurrent.lock package")
typealias Lock = korlibs.concurrent.lock.Lock
@Deprecated("Use korlibs.concurrent.lock package")
typealias NonRecursiveLock = korlibs.concurrent.lock.NonRecursiveLock

@Deprecated("Use korlibs.concurrent.lock package")
fun BaseLock.waitPrecise(time: TimeSpan): Boolean = this.waitPreciseConcurrent(time)
@Deprecated("Use korlibs.concurrent.lock package")
fun BaseLock.wait(time: TimeSpan, precise: Boolean): Boolean = this.waitConcurrent(time, precise)
