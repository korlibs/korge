package com.soywiz.korge.render

import com.soywiz.kds.FastIdentityMap
import com.soywiz.kds.getAndRemove
import com.soywiz.kds.getOrPut
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korag.AG
import com.soywiz.korag.AGList

/**
 * Class to handle cached buffers, that are freed after a few frames of not being used
 */
class AgBufferManager(
    val ag: AG
) {
    private val buffers = FastIdentityMap<AgCachedBuffer, AG.Buffer>()
    private val referencedBuffersSinceGC = AgFastSet<AgCachedBuffer>()

    fun getBuffer(cached: AgCachedBuffer): AG.Buffer {
        referencedBuffersSinceGC.add(cached)
        return buffers.getOrPut(cached) {
            ag.createBuffer().also {
                it.upload(cached.data, cached.dataOffset, cached.dataLen)
            }
        }
    }

    var fcount = 0
    var framesBetweenGC = 60

    internal fun afterRender() {
        // Prevent leaks when not referenced anymore
        removeCache()

        fcount++
        if (fcount >= framesBetweenGC) {
            fcount = 0
            gc()
        }
    }

    fun gc() {
        delete(referencedBuffersSinceGC.items)
        referencedBuffersSinceGC.clear()
    }

    private fun removeCache() {
    }

    fun delete(buffer: List<AgCachedBuffer>) {
        ag.commandsNoWait { list ->
            buffer.fastForEach { delete(it, list) }
        }
    }
    fun delete(buffer: AgCachedBuffer) = ag.commandsNoWait { delete(buffer, it) }

    fun delete(buffer: AgCachedBuffer, list: AGList) {
        val buf = buffers.getAndRemove(buffer)
        buf?.close(list)
    }
}

//class AgCachedBuffer(val kind: AG.Buffer.Kind, val data: Any, val dataOffset: Int = 0, val dataLen: Int = -1)
class AgCachedBuffer(val data: Any, val dataOffset: Int = 0, val dataLen: Int = -1)
