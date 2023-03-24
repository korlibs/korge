package korlibs.kgl

import korlibs.memory.*
import korlibs.image.bitmap.*
import korlibs.io.lang.*

interface IKmlGl {
	fun startFrame() = Unit
	fun endFrame(): Unit = Unit
	fun activeTexture(texture: Int): Unit
	fun attachShader(program: Int, shader: Int): Unit
	fun bindAttribLocation(program: Int, index: Int, name: String): Unit
	fun bindBuffer(target: Int, buffer: Int): Unit
	fun bindFramebuffer(target: Int, framebuffer: Int): Unit
	fun bindRenderbuffer(target: Int, renderbuffer: Int): Unit
	fun bindTexture(target: Int, texture: Int): Unit
	fun blendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit
	fun blendEquation(mode: Int): Unit
	fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit
	fun blendFunc(sfactor: Int, dfactor: Int): Unit
	fun blendFuncSeparate(sfactorRGB: Int, dfactorRGB: Int, sfactorAlpha: Int, dfactorAlpha: Int): Unit
	fun bufferData(target: Int, size: Int, data: Buffer, usage: Int): Unit
	fun bufferSubData(target: Int, offset: Int, size: Int, data: Buffer): Unit
	fun checkFramebufferStatus(target: Int): Int
	fun clear(mask: Int): Unit
	fun clearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit
	fun clearDepthf(d: Float): Unit
	fun clearStencil(s: Int): Unit
	fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit
	fun compileShader(shader: Int): Unit
	fun compressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, imageSize: Int, data: Buffer): Unit
	fun compressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, imageSize: Int, data: Buffer): Unit
	fun copyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit
	fun copyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit
	fun createProgram(): Int
	fun createShader(type: Int): Int
	fun cullFace(mode: Int): Unit
	fun deleteBuffers(n: Int, items: Buffer): Unit
	fun deleteFramebuffers(n: Int, items: Buffer): Unit
	fun deleteProgram(program: Int): Unit
	fun deleteRenderbuffers(n: Int, items: Buffer): Unit
	fun deleteShader(shader: Int): Unit
	fun deleteTextures(n: Int, items: Buffer): Unit
	fun depthFunc(func: Int): Unit
	fun depthMask(flag: Boolean): Unit
	fun depthRangef(n: Float, f: Float): Unit
	fun detachShader(program: Int, shader: Int): Unit
	fun disable(cap: Int): Unit
	fun disableVertexAttribArray(index: Int): Unit
	fun drawArrays(mode: Int, first: Int, count: Int): Unit
	fun drawElements(mode: Int, count: Int, type: Int, indices: Int): Unit
	fun enable(cap: Int): Unit
	fun enableVertexAttribArray(index: Int): Unit
	fun finish(): Unit
	fun flush(): Unit
	fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit
	fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int): Unit
	fun frontFace(mode: Int): Unit
	fun genBuffers(n: Int, buffers: Buffer): Unit
	fun generateMipmap(target: Int): Unit
	fun genFramebuffers(n: Int, framebuffers: Buffer): Unit
	fun genRenderbuffers(n: Int, renderbuffers: Buffer): Unit
	fun genTextures(n: Int, textures: Buffer): Unit
	fun getActiveAttrib(program: Int, index: Int, bufSize: Int, length: Buffer, size: Buffer, type: Buffer, name: Buffer): Unit
	fun getActiveUniform(program: Int, index: Int, bufSize: Int, length: Buffer, size: Buffer, type: Buffer, name: Buffer): Unit
	fun getAttachedShaders(program: Int, maxCount: Int, count: Buffer, shaders: Buffer): Unit
	fun getAttribLocation(program: Int, name: String): Int
	fun getUniformLocation(program: Int, name: String): Int
	fun getBooleanv(pname: Int, data: Buffer): Unit
	fun getBufferParameteriv(target: Int, pname: Int, params: Buffer): Unit
	fun getError(): Int
	fun getFloatv(pname: Int, data: Buffer): Unit
	fun getFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: Buffer): Unit
	fun getIntegerv(pname: Int, data: Buffer): Unit
	fun getProgramInfoLog(program: Int, bufSize: Int, length: Buffer, infoLog: Buffer): Unit
	fun getRenderbufferParameteriv(target: Int, pname: Int, params: Buffer): Unit
	fun getProgramiv(program: Int, pname: Int, params: Buffer): Unit
	fun getShaderiv(shader: Int, pname: Int, params: Buffer): Unit
	fun getShaderInfoLog(shader: Int, bufSize: Int, length: Buffer, infoLog: Buffer): Unit
	fun getShaderPrecisionFormat(shadertype: Int, precisiontype: Int, range: Buffer, precision: Buffer): Unit
	fun getShaderSource(shader: Int, bufSize: Int, length: Buffer, source: Buffer): Unit
	fun getString(name: Int): String
	fun getTexParameterfv(target: Int, pname: Int, params: Buffer): Unit
	fun getTexParameteriv(target: Int, pname: Int, params: Buffer): Unit
	fun getUniformfv(program: Int, location: Int, params: Buffer): Unit
	fun getUniformiv(program: Int, location: Int, params: Buffer): Unit
	fun getVertexAttribfv(index: Int, pname: Int, params: Buffer): Unit
	fun getVertexAttribiv(index: Int, pname: Int, params: Buffer): Unit
	fun getVertexAttribPointerv(index: Int, pname: Int, pointer: Buffer): Unit
	fun hint(target: Int, mode: Int): Unit
	fun isBuffer(buffer: Int): Boolean
	fun isEnabled(cap: Int): Boolean
	fun isFramebuffer(framebuffer: Int): Boolean
	fun isProgram(program: Int): Boolean
	fun isRenderbuffer(renderbuffer: Int): Boolean
	fun isShader(shader: Int): Boolean
	fun isTexture(texture: Int): Boolean
	fun lineWidth(width: Float): Unit
	fun linkProgram(program: Int): Unit
	fun pixelStorei(pname: Int, param: Int): Unit
	fun polygonOffset(factor: Float, units: Float): Unit
	fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: Buffer): Unit
	fun releaseShaderCompiler(): Unit
	fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit
	fun sampleCoverage(value: Float, invert: Boolean): Unit
	fun scissor(x: Int, y: Int, width: Int, height: Int): Unit
	fun shaderBinary(count: Int, shaders: Buffer, binaryformat: Int, binary: Buffer, length: Int): Unit
	fun shaderSource(shader: Int, string: String): Unit
	fun stencilFunc(func: Int, ref: Int, mask: Int): Unit
	fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit
	fun stencilMask(mask: Int): Unit
	fun stencilMaskSeparate(face: Int, mask: Int): Unit
	fun stencilOp(fail: Int, zfail: Int, zpass: Int): Unit
	fun stencilOpSeparate(face: Int, sfail: Int, dpfail: Int, dppass: Int): Unit
	fun texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: Buffer?): Unit
	fun texImage2D(target: Int, level: Int, internalformat: Int, format: Int, type: Int, data: NativeImage): Unit
	fun texParameterf(target: Int, pname: Int, param: Float): Unit
	fun texParameterfv(target: Int, pname: Int, params: Buffer): Unit
	fun texParameteri(target: Int, pname: Int, param: Int): Unit
	fun texParameteriv(target: Int, pname: Int, params: Buffer): Unit
	fun texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, pixels: Buffer): Unit
	fun uniform1f(location: Int, v0: Float): Unit
	fun uniform1fv(location: Int, count: Int, value: Buffer): Unit
	fun uniform1i(location: Int, v0: Int): Unit
	fun uniform1iv(location: Int, count: Int, value: Buffer): Unit
	fun uniform2f(location: Int, v0: Float, v1: Float): Unit
	fun uniform2fv(location: Int, count: Int, value: Buffer): Unit
	fun uniform2i(location: Int, v0: Int, v1: Int): Unit
	fun uniform2iv(location: Int, count: Int, value: Buffer): Unit
	fun uniform3f(location: Int, v0: Float, v1: Float, v2: Float): Unit
	fun uniform3fv(location: Int, count: Int, value: Buffer): Unit
	fun uniform3i(location: Int, v0: Int, v1: Int, v2: Int): Unit
	fun uniform3iv(location: Int, count: Int, value: Buffer): Unit
	fun uniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit
	fun uniform4fv(location: Int, count: Int, value: Buffer): Unit
	fun uniform4i(location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit
	fun uniform4iv(location: Int, count: Int, value: Buffer): Unit
	fun uniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: Buffer): Unit
	fun uniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: Buffer): Unit
	fun uniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: Buffer): Unit
	fun useProgram(program: Int): Unit
	fun validateProgram(program: Int): Unit
	fun vertexAttrib1f(index: Int, x: Float): Unit
	fun vertexAttrib1fv(index: Int, v: Buffer): Unit
	fun vertexAttrib2f(index: Int, x: Float, y: Float): Unit
	fun vertexAttrib2fv(index: Int, v: Buffer): Unit
	fun vertexAttrib3f(index: Int, x: Float, y: Float, z: Float): Unit
	fun vertexAttrib3fv(index: Int, v: Buffer): Unit
	fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float): Unit
	fun vertexAttrib4fv(index: Int, v: Buffer): Unit
	fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, pointer: Long): Unit
	fun viewport(x: Int, y: Int, width: Int, height: Int): Unit
    fun enableDisable(cap: Int, enable: Boolean): Boolean {
        if (enable) enable(cap) else disable(cap)
        return enable
    }
    fun enableDisableVertexAttribArray(index: Int, enable: Boolean) {
        if (enable) enableVertexAttribArray(index) else disableVertexAttribArray(index)
    }
    // https://www.khronos.org/registry/webgl/extensions/ANGLE_instanced_arrays/
    fun drawArraysInstanced(mode: Int, first: Int, count: Int, instancecount: Int): Unit = unsupported("Not supported instanced drawing")
    fun drawElementsInstanced(mode: Int, count: Int, type: Int, indices: Int, instancecount: Int): Unit = unsupported("Not supported instanced drawing")
    fun vertexAttribDivisor(index: Int, divisor: Int): Unit = unsupported()
    fun renderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int): Unit = unsupported("Not supported MSAA")
    fun texImage2DMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int, fixedsamplelocations: Boolean): Unit = unsupported("Not supported MSAA")

    fun shaderSourceWithExt(shader: Int, string: String) {
        val stringLines = string.lines()
        val versionLines = arrayListOf<String>()
        val extensionLines = arrayListOf<String>()
        val normalLines = arrayListOf<String>()
        for (line in stringLines) {
            when {
                line.startsWith("#version") -> versionLines += line
                line.startsWith("#extension") -> extensionLines += line
                else -> normalLines += line
            }
        }
        shaderSource(shader, listOf(
            // @TODO: This shouldn't be necessary. Just do not include it in the shader source code, and include it here
            *versionLines.toTypedArray(),
            *extensionLines.toTypedArray(),
            "#extension GL_OES_standard_derivatives : enable",
            "#ifdef GL_ES",
            "precision mediump float;",
            "#endif",
            *normalLines.toTypedArray(),
        ).joinToString("\n"))
    }
}

inline fun IKmlGl.enableDisable(cap: Int, enable: Boolean, block: () -> Unit) {
    if (enableDisable(cap, enable)) block()
}