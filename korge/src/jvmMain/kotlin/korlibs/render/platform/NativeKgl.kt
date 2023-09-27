package korlibs.render.platform

import com.sun.jna.*
import korlibs.graphics.shader.gl.*
import korlibs.image.awt.*
import korlibs.image.bitmap.*
import korlibs.kgl.*
import korlibs.math.*
import korlibs.memory.*

open class NativeKgl constructor(private val gl: INativeGL) : KmlGl() {
    override val variant: GLVariant = GLVariant.JVM

    override fun activeTexture(texture: Int): Unit = gl.glActiveTexture(texture)
    override fun attachShader(program: Int, shader: Int): Unit = gl.glAttachShader(program, shader)
    override fun bindAttribLocation(program: Int, index: Int, name: String): Unit = gl.glBindAttribLocation(program, index, name)
    override fun bindBuffer(target: Int, buffer: Int): Unit = gl.glBindBuffer(target, buffer)
    override fun bindFramebuffer(target: Int, framebuffer: Int): Unit = gl.glBindFramebuffer(target, framebuffer)
    override fun bindRenderbuffer(target: Int, renderbuffer: Int): Unit = gl.glBindRenderbuffer(target, renderbuffer)
    override fun bindTexture(target: Int, texture: Int): Unit = gl.glBindTexture(target, texture)
    override fun blendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = gl.glBlendColor(red, green, blue, alpha)
    override fun blendEquation(mode: Int): Unit = gl.glBlendEquation(mode)
    override fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit = gl.glBlendEquationSeparate(modeRGB, modeAlpha)
    override fun blendFunc(sfactor: Int, dfactor: Int): Unit = gl.glBlendFunc(sfactor, dfactor)
    override fun blendFuncSeparate(sfactorRGB: Int, dfactorRGB: Int, sfactorAlpha: Int, dfactorAlpha: Int): Unit = gl.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha)
    override fun bufferData(target: Int, size: Int, data: Buffer, usage: Int): Unit = gl.glBufferData(target, NativeLong(size.toLong()), data.nioBuffer, usage)
    override fun bufferSubData(target: Int, offset: Int, size: Int, data: Buffer): Unit = gl.glBufferSubData(target, NativeLong(offset.toLong()), NativeLong(size.toLong()), data.nioBuffer)
    override fun checkFramebufferStatus(target: Int): Int = gl.glCheckFramebufferStatus(target)
    override fun clear(mask: Int): Unit = gl.glClear(mask)
    override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = gl.glClearColor(red, green, blue, alpha)
    override fun clearDepthf(d: Float): Unit = gl.glClearDepth(d.toDouble())
    override fun clearStencil(s: Int): Unit = gl.glClearStencil(s)
    override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit = gl.glColorMask(red.toByte(), green.toByte(), blue.toByte(), alpha.toByte())
    override fun compileShader(shader: Int): Unit = gl.glCompileShader(shader)
    override fun compressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, imageSize: Int, data: Buffer): Unit = gl.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data.nioBuffer)
    override fun compressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, imageSize: Int, data: Buffer): Unit = gl.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data.nioBuffer)
    override fun copyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit = gl.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border)
    override fun copyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit = gl.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
    override fun createProgram(): Int = gl.glCreateProgram()
    override fun createShader(type: Int): Int = gl.glCreateShader(type)
    override fun cullFace(mode: Int): Unit = gl.glCullFace(mode)
    override fun deleteBuffers(n: Int, items: Buffer): Unit = gl.glDeleteBuffers(n, items.directIntBuffer)
    override fun deleteFramebuffers(n: Int, items: Buffer): Unit = gl.glDeleteFramebuffers(n, items.directIntBuffer)
    override fun deleteProgram(program: Int): Unit = gl.glDeleteProgram(program)
    override fun deleteRenderbuffers(n: Int, items: Buffer): Unit = gl.glDeleteRenderbuffers(n, items.directIntBuffer)
    override fun deleteShader(shader: Int): Unit = gl.glDeleteShader(shader)
    override fun deleteTextures(n: Int, items: Buffer): Unit = gl.glDeleteTextures(n, items.directIntBuffer)
    override fun depthFunc(func: Int): Unit = gl.glDepthFunc(func)
    override fun depthMask(flag: Boolean): Unit = gl.glDepthMask(flag.toByte())
    override fun depthRangef(n: Float, f: Float): Unit = gl.glDepthRange(n.toDouble(), f.toDouble())
    override fun detachShader(program: Int, shader: Int): Unit = gl.glDetachShader(program, shader)
    override fun disable(cap: Int): Unit = gl.glDisable(cap)
    override fun disableVertexAttribArray(index: Int): Unit = gl.glDisableVertexAttribArray(index)
    override fun drawArrays(mode: Int, first: Int, count: Int): Unit = gl.glDrawArrays(mode, first, count)
    override fun drawElements(mode: Int, count: Int, type: Int, indices: Int): Unit = gl.glDrawElements(mode, count, type, NativeLong(indices.toLong()))
    override fun enable(cap: Int): Unit = gl.glEnable(cap)
    override fun enableVertexAttribArray(index: Int): Unit = gl.glEnableVertexAttribArray(index)
    override fun finish(): Unit = gl.glFinish()
    override fun flush(): Unit = gl.glFlush()
    override fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit = gl.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
    override fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int): Unit = gl.glFramebufferTexture2D(target, attachment, textarget, texture, level)
    override fun frontFace(mode: Int): Unit = gl.glFrontFace(mode)
    override fun genBuffers(n: Int, buffers: Buffer): Unit = gl.glGenBuffers(n, buffers.directIntBuffer)
    override fun generateMipmap(target: Int): Unit = gl.glGenerateMipmap(target)
    override fun genFramebuffers(n: Int, framebuffers: Buffer): Unit = gl.glGenFramebuffers(n, framebuffers.directIntBuffer)
    override fun genRenderbuffers(n: Int, renderbuffers: Buffer): Unit = gl.glGenRenderbuffers(n, renderbuffers.directIntBuffer)
    override fun genTextures(n: Int, textures: Buffer): Unit = gl.glGenTextures(n, textures.directIntBuffer)
    override fun getActiveAttrib(program: Int, index: Int, bufSize: Int, length: Buffer, size: Buffer, type: Buffer, name: Buffer): Unit = gl.glGetActiveAttrib(program, index, bufSize, length.directIntBuffer, size.directIntBuffer, type.directIntBuffer, name.nioBuffer)
    override fun getActiveUniform(program: Int, index: Int, bufSize: Int, length: Buffer, size: Buffer, type: Buffer, name: Buffer): Unit = gl.glGetActiveUniform(program, index, bufSize, length.directIntBuffer, size.directIntBuffer, type.directIntBuffer, name.nioBuffer)
    override fun getAttachedShaders(program: Int, maxCount: Int, count: Buffer, shaders: Buffer): Unit = gl.glGetAttachedShaders(program, maxCount, count.directIntBuffer, shaders.directIntBuffer)
    override fun getAttribLocation(program: Int, name: String): Int = gl.glGetAttribLocation(program, name)
    override fun getUniformLocation(program: Int, name: String): Int = gl.glGetUniformLocation(program, name)
    override fun getBooleanv(pname: Int, data: Buffer): Unit = gl.glGetBooleanv(pname, data.nioBuffer)
    override fun getBufferParameteriv(target: Int, pname: Int, params: Buffer): Unit = gl.glGetBufferParameteriv(target, pname, params.directIntBuffer)
    override fun getError(): Int = gl.glGetError()
    override fun getFloatv(pname: Int, data: Buffer): Unit = gl.glGetFloatv(pname, data.directFloatBuffer)
    override fun getFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: Buffer): Unit = gl.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params.directIntBuffer)
    override fun getIntegerv(pname: Int, data: Buffer): Unit = gl.glGetIntegerv(pname, data.directIntBuffer)
    override fun getProgramInfoLog(program: Int, bufSize: Int, length: Buffer, infoLog: Buffer): Unit = gl.glGetProgramInfoLog(program, bufSize, length.directIntBuffer, infoLog.nioBuffer)
    override fun getRenderbufferParameteriv(target: Int, pname: Int, params: Buffer): Unit = gl.glGetRenderbufferParameteriv(target, pname, params.directIntBuffer)
    override fun getProgramiv(program: Int, pname: Int, params: Buffer): Unit = gl.glGetProgramiv(program, pname, params.directIntBuffer)
    override fun getShaderiv(shader: Int, pname: Int, params: Buffer): Unit = gl.glGetShaderiv(shader, pname, params.directIntBuffer)
    override fun getShaderInfoLog(shader: Int, bufSize: Int, length: Buffer, infoLog: Buffer): Unit = gl.glGetShaderInfoLog(shader, bufSize, length.directIntBuffer, infoLog.nioBuffer)
    override fun getShaderPrecisionFormat(shadertype: Int, precisiontype: Int, range: Buffer, precision: Buffer): Unit = gl.glGetShaderPrecisionFormat(shadertype, precisiontype, range.directIntBuffer, precision.directIntBuffer)
    override fun getShaderSource(shader: Int, bufSize: Int, length: Buffer, source: Buffer): Unit = gl.glGetShaderSource(shader, bufSize, length.directIntBuffer, source.nioBuffer)
    override fun getString(name: Int): String = gl.glGetString(name) ?: ""
    override fun getTexParameterfv(target: Int, pname: Int, params: Buffer): Unit = gl.glGetTexParameterfv(target, pname, params.directFloatBuffer)
    override fun getTexParameteriv(target: Int, pname: Int, params: Buffer): Unit = gl.glGetTexParameteriv(target, pname, params.directIntBuffer)
    override fun getUniformfv(program: Int, location: Int, params: Buffer): Unit = gl.glGetUniformfv(program, location, params.directFloatBuffer)
    override fun getUniformiv(program: Int, location: Int, params: Buffer): Unit = gl.glGetUniformiv(program, location, params.directIntBuffer)
    override fun getVertexAttribfv(index: Int, pname: Int, params: Buffer): Unit = gl.glGetVertexAttribfv(index, pname, params.directFloatBuffer)
    override fun getVertexAttribiv(index: Int, pname: Int, params: Buffer): Unit = gl.glGetVertexAttribiv(index, pname, params.directIntBuffer)
    override fun getVertexAttribPointerv(index: Int, pname: Int, pointer: Buffer): Unit = gl.glGetVertexAttribiv(index, pname, pointer.directIntBuffer)
    override fun hint(target: Int, mode: Int): Unit = gl.glHint(target, mode)
    override fun isBuffer(buffer: Int): Boolean = gl.glIsBuffer(buffer).toBoolean()
    override fun isEnabled(cap: Int): Boolean = gl.glIsEnabled(cap).toBoolean()
    override fun isFramebuffer(framebuffer: Int): Boolean = gl.glIsFramebuffer(framebuffer).toBoolean()
    override fun isProgram(program: Int): Boolean = gl.glIsProgram(program).toBoolean()
    override fun isRenderbuffer(renderbuffer: Int): Boolean = gl.glIsRenderbuffer(renderbuffer).toBoolean()
    override fun isShader(shader: Int): Boolean = gl.glIsShader(shader).toBoolean()
    override fun isTexture(texture: Int): Boolean = gl.glIsTexture(texture).toBoolean()
    override fun lineWidth(width: Float): Unit = gl.glLineWidth(width)
    override fun linkProgram(program: Int): Unit = gl.glLinkProgram(program)
    override fun pixelStorei(pname: Int, param: Int): Unit = gl.glPixelStorei(pname, param)
    override fun polygonOffset(factor: Float, units: Float): Unit = gl.glPolygonOffset(factor, units)
    override fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: Buffer): Unit = gl.glReadPixels(x, y, width, height, format, type, pixels.nioBuffer)
    override fun releaseShaderCompiler(): Unit = gl.glReleaseShaderCompiler()
    override fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit = gl.glRenderbufferStorage(target, internalformat, width, height)
    override fun sampleCoverage(value: Float, invert: Boolean): Unit = gl.glSampleCoverage(value, invert.toByte())
    override fun scissor(x: Int, y: Int, width: Int, height: Int): Unit = gl.glScissor(x, y, width, height)
    override fun shaderBinary(count: Int, shaders: Buffer, binaryformat: Int, binary: Buffer, length: Int): Unit = gl.glShaderBinary(count, shaders.directIntBuffer, binaryformat, binary.nioBuffer, length)
    override fun shaderSource(shader: Int, string: String): Unit = gl.glShaderSource(shader, 1, arrayOf(string), intArrayOf(string.length))
    override fun stencilFunc(func: Int, ref: Int, mask: Int): Unit = gl.glStencilFunc(func, ref, mask)
    override fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit = gl.glStencilFuncSeparate(face, func, ref, mask)
    override fun stencilMask(mask: Int): Unit = gl.glStencilMask(mask)
    override fun stencilMaskSeparate(face: Int, mask: Int): Unit = gl.glStencilMaskSeparate(face, mask)
    override fun stencilOp(fail: Int, zfail: Int, zpass: Int): Unit = gl.glStencilOp(fail, zfail, zpass)
    override fun stencilOpSeparate(face: Int, sfail: Int, dpfail: Int, dppass: Int): Unit = gl.glStencilOpSeparate(face, sfail, dpfail, dppass)
    override fun texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: Buffer?): Unit = gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels?.nioBuffer)
    override fun texImage2D(target: Int, level: Int, internalformat: Int, format: Int, type: Int, data: NativeImage): Unit {
        gl.glTexImage2D(target, level, internalformat, data.width, data.height, 0, format, type, (data as BaseAwtNativeImage).buffer)
    }
    override fun texParameterf(target: Int, pname: Int, param: Float): Unit = gl.glTexParameterf(target, pname, param)
    override fun texParameterfv(target: Int, pname: Int, params: Buffer): Unit = gl.glTexParameterfv(target, pname, params.directFloatBuffer)
    override fun texParameteri(target: Int, pname: Int, param: Int): Unit = gl.glTexParameteri(target, pname, param)
    override fun texParameteriv(target: Int, pname: Int, params: Buffer): Unit = gl.glTexParameteriv(target, pname, params.directIntBuffer)
    override fun texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, pixels: Buffer): Unit = gl.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels.nioBuffer)
    override fun uniform1f(location: Int, v0: Float): Unit = gl.glUniform1f(location, v0)
    override fun uniform1fv(location: Int, count: Int, value: Buffer): Unit = gl.glUniform1fv(location, count, value.directFloatBuffer)
    override fun uniform1i(location: Int, v0: Int): Unit = gl.glUniform1i(location, v0)
    override fun uniform1iv(location: Int, count: Int, value: Buffer): Unit = gl.glUniform1iv(location, count, value.directIntBuffer)
    override fun uniform2f(location: Int, v0: Float, v1: Float): Unit = gl.glUniform2f(location, v0, v1)
    override fun uniform2fv(location: Int, count: Int, value: Buffer): Unit = gl.glUniform2fv(location, count, value.directFloatBuffer)
    override fun uniform2i(location: Int, v0: Int, v1: Int): Unit = gl.glUniform2i(location, v0, v1)
    override fun uniform2iv(location: Int, count: Int, value: Buffer): Unit = gl.glUniform2iv(location, count, value.directIntBuffer)
    override fun uniform3f(location: Int, v0: Float, v1: Float, v2: Float): Unit = gl.glUniform3f(location, v0, v1, v2)
    override fun uniform3fv(location: Int, count: Int, value: Buffer): Unit = gl.glUniform3fv(location, count, value.directFloatBuffer)
    override fun uniform3i(location: Int, v0: Int, v1: Int, v2: Int): Unit = gl.glUniform3i(location, v0, v1, v2)
    override fun uniform3iv(location: Int, count: Int, value: Buffer): Unit = gl.glUniform3iv(location, count, value.directIntBuffer)
    override fun uniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit = gl.glUniform4f(location, v0, v1, v2, v3)
    override fun uniform4fv(location: Int, count: Int, value: Buffer): Unit = gl.glUniform4fv(location, count, value.directFloatBuffer)
    override fun uniform4i(location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit = gl.glUniform4i(location, v0, v1, v2, v3)
    override fun uniform4iv(location: Int, count: Int, value: Buffer): Unit = gl.glUniform4iv(location, count, value.directIntBuffer)
    override fun uniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: Buffer): Unit = gl.glUniformMatrix2fv(location, count, transpose.toByte(), value.directFloatBuffer.sliceBuffer(0, 4 * count))
    override fun uniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: Buffer): Unit = gl.glUniformMatrix3fv(location, count, transpose.toByte(), value.directFloatBuffer.sliceBuffer(0, 9 * count))
    override fun uniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: Buffer): Unit = gl.glUniformMatrix4fv(location, count, transpose.toByte(), value.directFloatBuffer.sliceBuffer(0, 16 * count))
    override fun useProgram(program: Int): Unit = gl.glUseProgram(program)
    override fun validateProgram(program: Int): Unit = gl.glValidateProgram(program)
    override fun vertexAttrib1f(index: Int, x: Float): Unit = gl.glVertexAttrib1f(index, x)
    override fun vertexAttrib1fv(index: Int, v: Buffer): Unit = gl.glVertexAttrib1fv(index, v.directFloatBuffer)
    override fun vertexAttrib2f(index: Int, x: Float, y: Float): Unit = gl.glVertexAttrib2f(index, x, y)
    override fun vertexAttrib2fv(index: Int, v: Buffer): Unit = gl.glVertexAttrib2fv(index, v.directFloatBuffer)
    override fun vertexAttrib3f(index: Int, x: Float, y: Float, z: Float): Unit = gl.glVertexAttrib3f(index, x, y, z)
    override fun vertexAttrib3fv(index: Int, v: Buffer): Unit = gl.glVertexAttrib3fv(index, v.directFloatBuffer)
    override fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float): Unit = gl.glVertexAttrib4f(index, x, y, z, w)
    override fun vertexAttrib4fv(index: Int, v: Buffer): Unit = gl.glVertexAttrib4fv(index, v.directFloatBuffer)
    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, pointer: Long): Unit = gl.glVertexAttribPointer(index, size, type, normalized.toByte(), stride, NativeLong(pointer))
    override fun viewport(x: Int, y: Int, width: Int, height: Int): Unit = gl.glViewport(x, y, width, height)

    // GL_ARB_instanced_arrays
    val isInstancedSupportedActual: Boolean get() = "GL_ARB_instanced_arrays" in extensions
    override val isInstancedSupported: Boolean get() = true
    override fun drawArraysInstanced(mode: Int, first: Int, count: Int, instancecount: Int): Unit = gl.glDrawArraysInstanced(mode, first, count, instancecount)
    override fun drawElementsInstanced(mode: Int, count: Int, type: Int, indices: Int, instancecount: Int): Unit = gl.glDrawElementsInstanced(mode, count, type, NativeLong(indices.toLong()), instancecount)
    override fun vertexAttribDivisor(index: Int, divisor: Int): Unit = gl.glVertexAttribDivisor(index, divisor)
    override fun renderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int) {
        gl.glRenderbufferStorageMultisample(target, samples, internalformat, width, height)
    }

    override val isUniformBuffersSupported: Boolean get() = true

    override fun bindBufferRange(target: Int, index: Int, buffer: Int, offset: Int, size: Int) {
        return gl.glBindBufferRange(target, index, buffer, NativeLong(offset.toLong()), NativeLong(size.toLong()))
    }
    override fun getUniformBlockIndex(program: Int, name: String): Int = gl.glGetUniformBlockIndex(program, name)
    override fun uniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int) =
        gl.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)

    override var isVertexArraysSupported: Boolean = true

    override fun genVertexArrays(n: Int, arrays: Buffer): Unit {
        val intBuffer = arrays.directIntBuffer
        gl.glGenVertexArrays(n, intBuffer)
        if (intBuffer[0] <= 0) {
            val error = gl.glGetError()
            println("ERROR: genVertexArray: count=$n, error=${KmlGl.errorString(error)}, firstBuffer=${intBuffer[0]}, OPENGL.VERSION=${gl.glGetString(KmlGl.VERSION)}")
            isVertexArraysSupported = false
            arrays.setInt32(0, -1)
            return
        }
    }
    override fun deleteVertexArrays(n: Int, arrays: Buffer): Unit {
        val intBuffer = arrays.directIntBuffer
        gl.glDeleteVertexArrays(n, arrays.directIntBuffer)
        println("deleteVertexArrays, $n: " + intBuffer[0])
    }
    override fun bindVertexArray(array: Int): Unit { gl.glBindVertexArray(array) }
}


private const val GL_NUM_EXTENSIONS = 0x821D
