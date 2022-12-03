package com.soywiz.korag.gl

import com.soywiz.kds.*
import com.soywiz.kds.lock.*
import com.soywiz.kgl.*
import com.soywiz.korag.*
import com.soywiz.korag.AGNativeObject

class GLGlobalState(val gl: KmlGl, val agGlobalState: AGGlobalState) {
    internal val objectsToDeleteLock = Lock()
    internal val objectsToDelete = fastArrayListOf<GLBaseObject>()
}

internal open class GLBaseObject(val glboalState: GLGlobalState) : AGNativeObject {
    val gl: KmlGl = glboalState.gl

    open fun delete() {
    }

    final override fun markToDelete() {
        glboalState.objectsToDeleteLock { glboalState.objectsToDelete += this }
    }
}

internal fun AGBuffer.gl(state: GLGlobalState): GLBuffer = this.createOnce(state) { GLBuffer(state) }
internal class GLBuffer(state: GLGlobalState) : GLBaseObject(state) {
    var id = gl.genBuffer()
    override fun delete() {
        gl.deleteBuffer(id)
        id = -1
    }
}

internal fun AGFrameBuffer.gl(state: GLGlobalState): GLFrameBuffer = this.createOnce(state) { GLFrameBuffer(state, this) }
internal class GLFrameBuffer(state: GLGlobalState, val ag: AGFrameBuffer) : GLBaseObject(state) {
    var renderBufferId = gl.genRenderbuffer()
    var frameBufferId = gl.genFramebuffer()
    val width: Int get() = ag.width
    val height: Int get() = ag.height

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

    override fun delete() {
        gl.deleteTexture(id)
        id = -1
    }
}

////////////////

internal fun <T : AGObject, R: AGNativeObject> T.createOnce(state: GLGlobalState, block: (T) -> R): R {
    if (this._native == null || this._cachedContextVersion != state.agGlobalState.contextVersion) {
        this._cachedContextVersion = state.agGlobalState.contextVersion
        this._native = block(this)
    }
    return this._native as R
}

