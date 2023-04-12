package korlibs.graphics.gl

import korlibs.datastructure.*
import korlibs.datastructure.lock.*
import korlibs.graphics.*
import korlibs.io.concurrent.atomic.*
import korlibs.kgl.*
import korlibs.memory.unit.*

class GLGlobalState(val gl: KmlGl, val ag: AG) {
    var texturesCreated = korAtomic(0)
    var texturesDeleted = korAtomic(0)
    var texturesSize = korAtomic(0L)

    var buffersCreated = korAtomic(0)
    var buffersDeleted = korAtomic(0)
    var buffersSize = korAtomic(0L)

    internal val objectsToDeleteLock = Lock()
    internal val objectsToDelete = fastArrayListOf<GLBaseObject>()

    fun readStats(out: AGStats) {
        out.buffersCount = buffersCreated.value - buffersDeleted.value
        out.buffersMemory = ByteUnits.fromBytes(buffersSize.value)
        out.texturesCount = texturesCreated.value - texturesDeleted.value
        out.texturesCreated = texturesCreated.value
        out.texturesDeleted = texturesDeleted.value
        out.texturesMemory = ByteUnits.fromBytes(texturesSize.value)
    }
}

internal class GLBaseProgram(globalState: GLGlobalState, val programInfo: GLProgramInfo) : GLBaseObject(globalState) {
    override fun delete() {
        programInfo.delete(gl)
    }
    fun use() {
        programInfo.use(gl)
    }
}

internal open class GLBaseObject(val globalState: GLGlobalState) : AGNativeObject {
    val gl: KmlGl = globalState.gl

    open fun delete() {
    }

    final override fun markToDelete() {
        globalState.objectsToDeleteLock { globalState.objectsToDelete += this }
    }
}

internal fun AGBuffer.gl(state: GLGlobalState): GLBuffer = this.createOnce(state) { GLBuffer(state) }
internal class GLBuffer(state: GLGlobalState) : GLBaseObject(state) {
    var id: Int = gl.genBuffer()

    internal var lastUploadedSize = -1

    var estimatedBytes: Long = 0L
        set(value) {
            globalState.buffersSize.addAndGet(+value -field)
            field = value
        }
    init {
        globalState.buffersCreated.incrementAndGet()
    }
    override fun delete() {
        globalState.buffersDeleted.incrementAndGet()
        gl.deleteBuffer(id)
        id = -1
        lastUploadedSize = -1
    }

    override fun toString(): String = "GLBuffer($id)"
}

internal fun AGFrameBufferBase.gl(state: GLGlobalState): GLFrameBuffer = this.createOnce(state) { GLFrameBuffer(state, this) }
internal class GLFrameBuffer(state: GLGlobalState, val ag: AGFrameBufferBase) : GLBaseObject(state) {
    var renderBufferId = gl.genRenderbuffer()
    var frameBufferId = gl.genFramebuffer()
    var info: AGFrameBufferInfo = AGFrameBufferInfo.INVALID

    override fun delete() {
        gl.deleteRenderbuffer(renderBufferId)
        gl.deleteFramebuffer(frameBufferId)
        renderBufferId = -1
        frameBufferId = -1
    }
}

internal fun AGTexture.gl(state: GLGlobalState): GLTexture = this.createOnce(state) { GLTexture(state) }
internal class GLTexture(state: GLGlobalState) : GLBaseObject(state) {
    var id = gl.genTexture()
    var cachedContentVersion: Int = -2
    var cachedAGContextVersion: Int = -2
    var estimatedBytes = 0L
        set(value) {
            globalState.texturesSize.addAndGet(+value -field)
            field = value
        }

    init {
        globalState.texturesCreated.incrementAndGet()
        //println("CREATED texture=$id")
    }

    override fun delete() {
        //println("DELETE texture=$id")
        globalState.texturesDeleted.incrementAndGet()
        gl.deleteTexture(id)
        id = -1
    }
}

////////////////

internal fun <T : AGObject, R: AGNativeObject> T.createOnce(state: GLGlobalState, block: (T) -> R): R {
    if (this._native == null || this._cachedContextVersion != state.ag.contextVersion) {
        this._cachedContextVersion = state.ag.contextVersion
        this._resetVersion()
        this._native = block(this)
    }
    return this._native as R
}
