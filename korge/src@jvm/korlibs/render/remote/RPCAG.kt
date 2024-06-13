package korlibs.render.remote

import korlibs.datastructure.*
import korlibs.graphics.*
import korlibs.image.color.*
import korlibs.memory.*


class RPCAGExecutor(val client: RPCClient, val ag: AG) : AutoCloseable {
    val frameBuffer = LinkedHashMap<Int, AGFrameBufferBase>()
    val textures = LinkedHashMap<Int, AGTexture>()
    val buffers = LinkedHashMap<Int, AGBuffer>()

    val handler = client.registerPacketHandler(1000 until 2000, ::handle)

    override fun close() {
        handler.close()
    }

    fun handle(packet: Packet) {
        val data = packet.data
        when (packet.id) {
            RPCAG.CLIENT_AG_OBJECT_CREATE -> {
                val poolId = data.getInt()
                val id = data.getInt()
                val info = data.getInt()
                println("CLIENT_AG_OBJECT_CREATE: poolId=$poolId, id=$id, info=$info")
                when (poolId) {
                    RPCAG.POOL_ID_FRAME -> frameBuffer.getOrPut(id) { AGFrameBufferBase(isMain = info != 0) }
                    RPCAG.POOL_ID_TEXTURE -> textures.getOrPut(id) { AGTexture() }
                    RPCAG.POOL_ID_BUFFER -> buffers.getOrPut(id) { AGBuffer() }
                }
            }
            RPCAG.CLIENT_AG_OBJECT_DELETE -> {
                val poolId = data.getInt()
                val id = data.getInt()
                println("CLIENT_AG_OBJECT_DELETE: poolId=$poolId, id=$id")
                when (poolId) {
                    RPCAG.POOL_ID_FRAME -> frameBuffer.remove(id)?.close()
                    RPCAG.POOL_ID_TEXTURE -> textures.remove(id)?.close()
                    RPCAG.POOL_ID_BUFFER -> buffers.remove(id)?.close()
                }
            }
            RPCAG.CLIENT_AG_CLEAR -> {
                val frameBufferId = data.getInt()
                val frameBufferInfo = AGFrameBufferInfo(data.getLong())
                val color = RGBA(data.getInt())
                val depth = data.getFloat()
                val stencil = data.getInt()
                val clearBits = data.getInt()
                val scissor = AGScissor(data.getLong())
                val clearColor = clearBits.extract(0)
                val clearDepth = clearBits.extract(1)
                val clearStencil = clearBits.extract(2)
                println("CLEAR!")
                ag.clear(
                    frameBuffer[frameBufferId]!!,
                    frameBufferInfo, color, depth, stencil, clearColor, clearDepth, clearStencil,
                    scissor
                )
            }
            RPCAG.CLIENT_AG_DRAW -> {
                //ag.draw(AGMultiBatch())
                TODO()
            }
        }
    }
}

class RPCAG(val client: RPCClient) : AG() {
    val frameBufferIds = Pool { it }
    val textureIds = Pool { it }
    val bufferIds = Pool { it }

    companion object {
        const val POOL_ID_FRAME = 1
        const val POOL_ID_TEXTURE = 2
        const val POOL_ID_BUFFER = 3

        // CLIENT -> SERVER
        const val CLIENT_AG_OBJECT_CREATE = 1001
        const val CLIENT_AG_OBJECT_DELETE = 1002
        const val CLIENT_AG_CLEAR = 1011
        const val CLIENT_AG_DRAW = 1012
        const val CLIENT_AG_UPLOAD_TEX = 1021
        const val CLIENT_AG_UPLOAD_BUFFER = 1022
    }

    override fun clear(
        frameBuffer: AGFrameBufferBase,
        frameBufferInfo: AGFrameBufferInfo,
        color: RGBA,
        depth: Float,
        stencil: Int,
        clearColor: Boolean,
        clearDepth: Boolean,
        clearStencil: Boolean,
        scissor: AGScissor
    ) {
        client.sendPacket(CLIENT_AG_CLEAR, 64) {
            putInt(frameBuffer.obj().id)
            putLong(frameBufferInfo.dataLong)
            putInt(color.value)
            putFloat(depth)
            putInt(stencil)
            putInt(0.insert(clearColor, 0).insert(clearDepth, 1).insert(clearStencil, 2))
            putLong(scissor.raw)
        }
    }

    fun AGFrameBufferBase.obj() = mobj(POOL_ID_FRAME, frameBufferIds) { if (this.isMain) 1 else 0 }
    fun AGTexture.obj() = mobj(POOL_ID_TEXTURE, textureIds)
    fun AGBuffer.obj() = mobj(POOL_ID_BUFFER, bufferIds)

    internal inline fun <T : AGObject> T.mobj(poolId: Int, pool: Pool<Int>, genExtra: () -> Int = { 0 }) = this.createOnce(this@RPCAG) { RPCNativeObject(this, poolId, pool, genExtra()) }

    inner class RPCNativeObject<T : AGObject>(val obj: T, val poolId: Int, val pool: Pool<Int>, val info: Int = 0) : AGNativeObject {
        val id = pool.alloc()
        init {
            client.sendPacket(CLIENT_AG_OBJECT_CREATE, 12) {
                putInt(poolId)
                putInt(id)
                putInt(info)
            }
        }

        override fun markToDelete() {
            pool.free(id)
            client.sendPacket(CLIENT_AG_OBJECT_DELETE, 8) {
                putInt(poolId)
                putInt(id)
            }
        }
    }
}
