@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.pauseable

import korlibs.datastructure.lock.Lock
import korlibs.time.seconds

interface Pauseable {
    var paused: Boolean
}
fun Pauseable.pause() {
    paused = true
}
fun Pauseable.resume() {
    paused = false
}

class SyncPauseable : Pauseable {
    val pausedLock = Lock()
    override var paused: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                pausedLock { pausedLock.notify() }
            }
        }
    fun checkPaused() {
        while (paused) { pausedLock { pausedLock.wait(60.seconds) } }
    }
}
