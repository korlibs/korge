package korlibs.datastructure.lock

import korlibs.time.*

actual class Lock actual constructor() : BaseLock {
    var locked = false
    actual inline operator fun <T> invoke(callback: () -> T): T {
        locked = true
        try {
            return callback()
        } finally {
            locked = false
        }
    }

    actual override fun notify(unit: Unit) {
        if (!locked) error("Must lock before notifying")
    }

    actual override fun wait(time: TimeSpan): Boolean {
        if (!locked) error("Must lock before waiting")
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
