package korlibs.datastructure.lock

import korlibs.time.*

actual class Lock actual constructor() : BaseLock {
	actual inline operator fun <T> invoke(callback: () -> T): T = callback()
    actual override fun notify(unit: Unit) {
    }
    actual override fun wait(time: TimeSpan): Boolean {
        return false
    }
}

actual class NonRecursiveLock actual constructor() : BaseLock {
    actual inline operator fun <T> invoke(callback: () -> T): T = callback()
    actual override fun notify(unit: Unit) {
    }
    actual override fun wait(time: TimeSpan): Boolean {
        return false
    }
}
