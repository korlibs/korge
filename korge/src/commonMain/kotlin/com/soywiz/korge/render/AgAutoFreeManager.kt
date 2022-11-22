package com.soywiz.korge.render

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korio.lang.*

// @TODO: This is pretty generic, we could expose it elsewhere
class AgAutoFreeManager(
) : Closeable {
    class Entry(
        var closeable: Closeable? = null,
    ) {
        fun closeAndReset() {
            closeable?.close()
            closeable = null
        }
        fun reset() {
            closeable = null
        }
    }

    private val entryPool = Pool(reset = { it.reset() }) { Entry() }
    private val cachedCloseables = FastIdentityMap<Closeable?, Entry>()
    private val availableInLastGC = fastArrayListOf<Entry>()

    fun reference(closeable: Closeable) {
        cachedCloseables.getOrPut(closeable) {
            entryPool.alloc().also { it.closeable = closeable }
        }
    }

    private var fcount = 0
    var framesBetweenGC: Int = 60

    internal fun afterRender() {
        fcount++
        if (fcount >= framesBetweenGC) {
            fcount = 0
            gc()
        }
    }

    internal fun gc() {
        // Delete elements that didn't survive the last GC
        for (entry in availableInLastGC) {
            if (!cachedCloseables.contains(entry.closeable)) {
                entry.closeable?.close()
                entryPool.free(entry)
            }
        }

        // Reconstruct
        availableInLastGC.clear()
        cachedCloseables.fastValueForEach {
            availableInLastGC.add(it)
        }

        // Clear cached entries
        cachedCloseables.clear()
    }

    override fun close() {
        availableInLastGC.fastForEach { it.closeAndReset() }
        cachedCloseables.fastValueForEach { it.closeAndReset() }
        availableInLastGC.clear()
        entryPool.clear()
        cachedCloseables.clear()
    }
}
