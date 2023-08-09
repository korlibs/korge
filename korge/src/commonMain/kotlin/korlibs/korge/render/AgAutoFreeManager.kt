@file:OptIn(ExperimentalStdlibApi::class)

package korlibs.korge.render

import korlibs.datastructure.*

// @TODO: This is pretty generic, we could expose it elsewhere
class AgAutoFreeManager(
) : AutoCloseable {
    class Entry(
            var autoCloseable: AutoCloseable? = null,
    ) {
        fun closeAndReset() {
            autoCloseable?.close()
            autoCloseable = null
        }
        fun reset() {
            autoCloseable = null
        }
    }

    private val entryPool = Pool(reset = { it.reset() }) { Entry() }
    private val cachedCloseables = FastIdentityMap<AutoCloseable?, Entry>()
    private val availableInLastGC = fastArrayListOf<Entry>()

    fun reference(autoCloseable: AutoCloseable) {
        cachedCloseables.getOrPut(autoCloseable) {
            entryPool.alloc().also { it.autoCloseable = autoCloseable }
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
            if (!cachedCloseables.contains(entry.autoCloseable)) {
                entry.autoCloseable?.close()
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
