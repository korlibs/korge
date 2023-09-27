// WARNING: File autogenerated DO NOT modify
// https://www.khronos.org/registry/OpenGL/api/GLES2/gl2.h
@file:Suppress("unused", "RedundantUnitReturnType", "PropertyName")

package korlibs.kgl

import korlibs.graphics.gl.*
import korlibs.graphics.shader.gl.*
import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.math.*
import korlibs.memory.*
import korlibs.memory.Buffer
import kotlinx.browser.*
import org.khronos.webgl.*
import org.w3c.dom.*
import kotlin.math.*

// https://github.com/shrekshao/MoveWebGL1EngineToWebGL2/blob/master/Move-a-WebGL-1-Engine-To-WebGL-2-Blog-1.md
// https://webglstats.com/
// https://caniuse.com/#feat=webgl
class KmlGlJsCanvas(val canvas: HTMLCanvasElement, val glOpts: dynamic) : KmlGl() {
    var webglVersion = 1
    val gl: WebGLRenderingContext = (null
            ?: canvas.getContext("webgl2", glOpts)?.also { webglVersion = 2 }
            ?: canvas.getContext("webgl", glOpts)
            ?: canvas.getContext("experimental-webgl", glOpts)
        ).unsafeCast<WebGLRenderingContext?>()
        ?.also {
            println("Created WebGL version=$webglVersion, opts=${JSON.stringify(glOpts)}")
        }
        ?.also {
            it.getExtension("OES_standard_derivatives")
            it.getExtension("OES_texture_float")
            it.getExtension("OES_texture_float_linear")
            it.getExtension("OES_element_index_uint")
            Unit
        }
        ?: run {
            try {
                document.body?.prepend((document.createElement("div") as HTMLElement).apply {
                    style.color = "red"
                    style.font = "30px Arial"
                    innerText = "Can't get webgl context. Running in an android emulator without cross-walk?"
                })
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            error("Can't get webgl context")
        }

    override val variant: GLVariant get() = GLVariant.JS_WEBGL(webglVersion)
    private val items = arrayOfNulls<Any>(8 * 1024)
    private val freeList = (1 until items.size).reversed().toMutableList()
    private fun <T> T.alloc(): Int {
		if (this === null) return 0
		if (this.asDynamic().id === undefined) {
		    if (freeList.isEmpty()) error("KmlGlJsCanvas.freeList is empty. (Probably allocating lots of OpenGL objects without releasing them)")
		    val index = freeList.removeAt(freeList.size - 1)
            items[index] = this
            (this.asDynamic()).id = index
		}
        return this.asDynamic().id.unsafeCast<Int>()
	}
	private fun <T> Int.get(): T? = if (this != 0) items[this].unsafeCast<T>() else null
    private fun <T> Int.free(): T? = if (this != 0) { val out = items[this].unsafeCast<T>(); freeList += this; items[this] = null; out } else { null }

    override fun activeTexture(texture: Int): Unit = gl.activeTexture(texture)
    override fun attachShader(program: Int, shader: Int): Unit = gl.attachShader(program.get(), shader.get())
    override fun bindAttribLocation(program: Int, index: Int, name: String): Unit = gl.bindAttribLocation(program.get(), index, name)
    override fun bindBuffer(target: Int, buffer: Int): Unit = gl.bindBuffer(target, buffer.get())
    override fun bindFramebuffer(target: Int, framebuffer: Int): Unit = gl.bindFramebuffer(target, framebuffer.get())
    override fun bindRenderbuffer(target: Int, renderbuffer: Int): Unit = gl.bindRenderbuffer(target, renderbuffer.get())
    override fun bindTexture(target: Int, texture: Int): Unit = gl.bindTexture(target, texture.get())
    override fun blendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = gl.blendColor(red, green, blue, alpha)
    override fun blendEquation(mode: Int): Unit = gl.blendEquation(mode)
    override fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit = gl.blendEquationSeparate(modeRGB, modeAlpha)
    override fun blendFunc(sfactor: Int, dfactor: Int): Unit = gl.blendFunc(sfactor, dfactor)
    override fun blendFuncSeparate(sfactorRGB: Int, dfactorRGB: Int, sfactorAlpha: Int, dfactorAlpha: Int): Unit = gl.blendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha)
    override fun bufferData(target: Int, size: Int, data: Buffer, usage: Int): Unit = gl.bufferData(target, data.sliceBuffer(0, size).arrayUByte, usage)
    override fun bufferSubData(target: Int, offset: Int, size: Int, data: Buffer): Unit = gl.bufferSubData(target, offset, data.sliceBuffer(0, size).arrayUByte)
    override fun checkFramebufferStatus(target: Int): Int = gl.checkFramebufferStatus(target)
    override fun clear(mask: Int): Unit = gl.clear(mask)
    override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = gl.clearColor(red, green, blue, alpha)
    override fun clearDepthf(d: Float): Unit = gl.clearDepth(d)
    override fun clearStencil(s: Int): Unit = gl.clearStencil(s)
    override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit = gl.colorMask(red, green, blue, alpha)
    override fun compileShader(shader: Int): Unit = gl.compileShader(shader.get())
    override fun compressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, imageSize: Int, data: Buffer): Unit = gl.compressedTexImage2D(target, level, internalformat, width, height, border, data.arrayByte)
    override fun compressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, imageSize: Int, data: Buffer): Unit = gl.compressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, data.arrayByte)
    override fun copyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit = gl.copyTexImage2D(target, level, internalformat, x, y, width, height, border)
    override fun copyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit = gl.copyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
    override fun createProgram(): Int = gl.createProgram().alloc()
    override fun createShader(type: Int): Int = gl.createShader(type).alloc()
    override fun cullFace(mode: Int): Unit = gl.cullFace(mode)
    override fun deleteBuffers(n: Int, items: Buffer) { for (p in 0 until n) gl.deleteBuffer(items.arrayInt[p].free()) }
    override fun deleteFramebuffers(n: Int, items: Buffer) { for (p in 0 until n) gl.deleteFramebuffer(items.arrayInt[p].free()) }
    override fun deleteProgram(program: Int): Unit = gl.deleteProgram(program.get())
    override fun deleteRenderbuffers(n: Int, items: Buffer) { for (p in 0 until n) gl.deleteRenderbuffer(items.arrayInt[p].free()) }
    override fun deleteShader(shader: Int): Unit = gl.deleteShader(shader.get())
    override fun deleteTextures(n: Int, items: Buffer) { for (p in 0 until n) gl.deleteTexture(items.arrayInt[p].free()) }
    override fun depthFunc(func: Int): Unit = gl.depthFunc(func)
    override fun depthMask(flag: Boolean): Unit = gl.depthMask(flag)
    override fun depthRangef(n: Float, f: Float): Unit = gl.depthRange(n, f)
    override fun detachShader(program: Int, shader: Int): Unit = gl.detachShader(program.get(), shader.get())
    override fun disable(cap: Int): Unit = gl.disable(cap)
    override fun disableVertexAttribArray(index: Int): Unit = gl.disableVertexAttribArray(index)
    override fun drawArrays(mode: Int, first: Int, count: Int): Unit = gl.drawArrays(mode, first, count)
    override fun drawElements(mode: Int, count: Int, type: Int, indices: Int): Unit = gl.drawElements(mode, count, type, indices)
    override fun enable(cap: Int): Unit = gl.enable(cap)
    override fun enableVertexAttribArray(index: Int): Unit = gl.enableVertexAttribArray(index)
    override fun finish(): Unit = gl.finish()
    override fun flush(): Unit = gl.flush()
    override fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit = gl.framebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer.get())
    override fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int): Unit = gl.framebufferTexture2D(target, attachment, textarget, texture.get(), level)
    override fun frontFace(mode: Int): Unit = gl.frontFace(mode)
    override fun genBuffers(n: Int, buffers: Buffer) { for (p in 0 until n) buffers.arrayInt[p] = gl.createBuffer().alloc() }
    override fun generateMipmap(target: Int): Unit = gl.generateMipmap(target)
    override fun genFramebuffers(n: Int, framebuffers: Buffer) { for (p in 0 until n) framebuffers.arrayInt[p] = gl.createFramebuffer().alloc() }
    override fun genRenderbuffers(n: Int, renderbuffers: Buffer) { for (p in 0 until n) renderbuffers.arrayInt[p] = gl.createRenderbuffer().alloc() }
    override fun genTextures(n: Int, textures: Buffer) { for (p in 0 until n) textures.arrayInt[p] = gl.createTexture().alloc() }
    override fun getActiveAttrib(program: Int, index: Int, bufSize: Int, length: Buffer, size: Buffer, type: Buffer, name: Buffer) { val info = gl.getActiveAttrib(program.get(), index)!!; size.arrayInt[0] = info.size; type.arrayInt[0] = info.type; name.putAsciiString(info.name); length.arrayInt[0] = info.size + 1 }
    override fun getActiveUniform(program: Int, index: Int, bufSize: Int, length: Buffer, size: Buffer, type: Buffer, name: Buffer) { val info = gl.getActiveUniform(program.get(), index)!!; size.arrayInt[0] = info.size; type.arrayInt[0] = info.type; name.putAsciiString(info.name); length.arrayInt[0] = info.size + 1 }
    override fun getAttachedShaders(program: Int, maxCount: Int, count: Buffer, shaders: Buffer) { val ashaders = gl.getAttachedShaders(program.get())!!; count.arrayInt[0] = ashaders.size; for (n in 0 until min(maxCount, ashaders.size)) shaders.arrayInt[n] = ashaders[n].asDynamic().id.unsafeCast<Int>() }
    override fun getAttribLocation(program: Int, name: String): Int = gl.getAttribLocation(program.get(), name)
    override fun getUniformLocation(program: Int, name: String): Int {
        val prg = program.get<WebGLProgram>().asDynamic()
        if (prg.uniforms === undefined) prg.uniforms = js("({})")
        if (prg.uniforms[name] === undefined) prg.uniforms[name] = gl.getUniformLocation(prg, name).alloc()
        return prg.uniforms[name].unsafeCast<Int>()
    }
    override fun getBooleanv(pname: Int, data: Buffer) { data.arrayInt[0] = gl.getParameter(pname).unsafeCast<Int>() }
    override fun getBufferParameteriv(target: Int, pname: Int, params: Buffer) { params.arrayInt[0] = gl.getBufferParameter(target, pname).unsafeCast<Int>() }
    override fun getError(): Int = gl.getError()
    override fun getFloatv(pname: Int, data: Buffer) { data.arrayFloat[0] = gl.getParameter(pname).unsafeCast<Float>() }
    override fun getFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: Buffer) { params.arrayInt[0] = gl.getFramebufferAttachmentParameter(target, attachment, pname).unsafeCast<Int>() }
    override fun getIntegerv(pname: Int, data: Buffer) { data.arrayInt[0] = gl.getParameter(pname).unsafeCast<Int>() }
    override fun getProgramInfoLog(program: Int, bufSize: Int, length: Buffer, infoLog: Buffer) { val str = gl.getProgramInfoLog(program.get()) ?: ""; length.arrayInt[0] = str.length; infoLog.putAsciiString(str) }
    override fun getRenderbufferParameteriv(target: Int, pname: Int, params: Buffer) { params.arrayInt[0] = gl.getRenderbufferParameter(target, pname).unsafeCast<Int>() }
    override fun getProgramiv(program: Int, pname: Int, params: Buffer) {
                when (pname) {
                    INFO_LOG_LENGTH -> params.arrayInt[0] = gl.getProgramInfoLog(program.get())?.length?.plus(1) ?: 1
                    else -> params.arrayInt[0] = gl.getProgramParameter(program.get(), pname).unsafeCast<Int>()
                }
            }
    override fun getShaderiv(shader: Int, pname: Int, params: Buffer) {
                when (pname) {
                    INFO_LOG_LENGTH -> params.arrayInt[0] = gl.getShaderInfoLog(shader.get())?.length?.plus(1) ?: 1
                    else -> params.arrayInt[0] = gl.getShaderParameter(shader.get(), pname).unsafeCast<Int>()
                }
            }
    override fun getShaderInfoLog(shader: Int, bufSize: Int, length: Buffer, infoLog: Buffer) { val str = gl.getShaderInfoLog(shader.get()) ?: ""; length.arrayInt[0] = str.length; infoLog.putAsciiString(str) }
    override fun getShaderPrecisionFormat(shadertype: Int, precisiontype: Int, range: Buffer, precision: Buffer) { val info = gl.getShaderPrecisionFormat(shadertype, precisiontype); if (info != null) { range.arrayInt[0] = info.rangeMin; range.arrayInt[1] = info.rangeMax; precision.arrayInt[0] = info.precision } }
    override fun getShaderSource(shader: Int, bufSize: Int, length: Buffer, source: Buffer) { val str = gl.getShaderSource(shader.get()) ?: ""; length.arrayInt[0] = str.length; source.putAsciiString(str) }
    override fun getString(name: Int): String = gl.getParameter(name).unsafeCast<String>()
    override fun getTexParameterfv(target: Int, pname: Int, params: Buffer) { params.arrayFloat[0] = gl.getTexParameter(target, pname).unsafeCast<Float>() }
    override fun getTexParameteriv(target: Int, pname: Int, params: Buffer) { params.arrayInt[0] = gl.getTexParameter(target, pname).unsafeCast<Int>() }
    override fun getUniformfv(program: Int, location: Int, params: Buffer) { params.arrayFloat[0] = gl.getUniform(program.get(), location.get()).unsafeCast<Float>() }
    override fun getUniformiv(program: Int, location: Int, params: Buffer) { params.arrayInt[0] = gl.getUniform(program.get(), location.get()).unsafeCast<Int>() }
    override fun getVertexAttribfv(index: Int, pname: Int, params: Buffer) { params.arrayFloat[0] = gl.getVertexAttrib(index, pname).unsafeCast<Float>() }
    override fun getVertexAttribiv(index: Int, pname: Int, params: Buffer) { params.arrayInt[0] = gl.getVertexAttrib(index, pname).unsafeCast<Int>() }
    override fun getVertexAttribPointerv(index: Int, pname: Int, pointer: Buffer) { pointer.arrayInt[0] = gl.getVertexAttrib(index, pname).unsafeCast<Int>() }
    override fun hint(target: Int, mode: Int): Unit = gl.hint(target, mode)
    override fun isBuffer(buffer: Int): Boolean = gl.isBuffer(buffer.get())
    override fun isEnabled(cap: Int): Boolean = gl.isEnabled(cap)
    override fun isFramebuffer(framebuffer: Int): Boolean = gl.isFramebuffer(framebuffer.get())
    override fun isProgram(program: Int): Boolean = gl.isProgram(program.get())
    override fun isRenderbuffer(renderbuffer: Int): Boolean = gl.isRenderbuffer(renderbuffer.get())
    override fun isShader(shader: Int): Boolean = gl.isShader(shader.get())
    override fun isTexture(texture: Int): Boolean = gl.isTexture(texture.get())
    override fun lineWidth(width: Float): Unit = gl.lineWidth(width)
    override fun linkProgram(program: Int): Unit = gl.linkProgram(program.get())
    override fun pixelStorei(pname: Int, param: Int): Unit = gl.pixelStorei(pname, param)
    override fun polygonOffset(factor: Float, units: Float): Unit = gl.polygonOffset(factor, units)
    override fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: Buffer): Unit = gl.readPixels(x, y, width, height, format, type, pixels.arrayUByte)
    override fun releaseShaderCompiler(): Unit = Unit
    override fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit = gl.renderbufferStorage(target, internalformat, width, height)
    override fun sampleCoverage(value: Float, invert: Boolean): Unit = gl.sampleCoverage(value, invert)
    override fun scissor(x: Int, y: Int, width: Int, height: Int): Unit = gl.scissor(x, y, width, height)
    override fun shaderBinary(count: Int, shaders: Buffer, binaryformat: Int, binary: Buffer, length: Int): Unit = throw KmlGlException("shaderBinary not implemented in Webgl")
    override fun shaderSource(shader: Int, string: String) = gl.shaderSource(shader.get(), string)
    override fun stencilFunc(func: Int, ref: Int, mask: Int): Unit = gl.stencilFunc(func, ref, mask)
    override fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit = gl.stencilFuncSeparate(face, func, ref, mask)
    override fun stencilMask(mask: Int): Unit = gl.stencilMask(mask)
    override fun stencilMaskSeparate(face: Int, mask: Int): Unit = gl.stencilMaskSeparate(face, mask)
    override fun stencilOp(fail: Int, zfail: Int, zpass: Int): Unit = gl.stencilOp(fail, zfail, zpass)
    override fun stencilOpSeparate(face: Int, sfail: Int, dpfail: Int, dppass: Int): Unit = gl.stencilOpSeparate(face, sfail, dpfail, dppass)
    override fun texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: Buffer?) {
        val vpixels: ArrayBufferView? = when (type) {
            FLOAT -> pixels?.arrayFloat
            else -> pixels?.arrayUByte
        }
        gl.texImage2D(target, level, internalformat, width, height, border, format, type, vpixels)
    }
    override fun texImage2D(target: Int, level: Int, internalformat: Int, format: Int, type: Int, data: NativeImage): Unit {
        gl.pixelStorei(UNPACK_PREMULTIPLY_ALPHA_WEBGL, (!data.asumePremultiplied && data.premultiplied).toInt())
        gl.texImage2D(target, level, internalformat, format, type, (data as HtmlNativeImage).texSource)
        gl.pixelStorei(UNPACK_PREMULTIPLY_ALPHA_WEBGL, 0)
    }
    override fun texParameterf(target: Int, pname: Int, param: Float): Unit = gl.texParameterf(target, pname, param)
    override fun texParameterfv(target: Int, pname: Int, params: Buffer): Unit = gl.texParameterf(target, pname, params.arrayFloat[0])
    override fun texParameteri(target: Int, pname: Int, param: Int): Unit = gl.texParameteri(target, pname, param)
    override fun texParameteriv(target: Int, pname: Int, params: Buffer): Unit = gl.texParameteri(target, pname, params.arrayInt[0])
    override fun texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, pixels: Buffer): Unit = gl.texSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels.arrayUByte)
    override fun uniform1f(location: Int, v0: Float): Unit = gl.uniform1f(location.get(), v0)
    override fun uniform1fv(location: Int, count: Int, value: Buffer): Unit = gl.uniform1fv(location.get(), value.arrayFloat.subarray(0, count * 1))
    override fun uniform1i(location: Int, v0: Int): Unit = gl.uniform1i(location.get(), v0)
    override fun uniform1iv(location: Int, count: Int, value: Buffer): Unit = gl.uniform1iv(location.get(), value.arrayInt.subarray(0, count * 1))
    override fun uniform2f(location: Int, v0: Float, v1: Float): Unit = gl.uniform2f(location.get(), v0, v1)
    override fun uniform2fv(location: Int, count: Int, value: Buffer): Unit = gl.uniform2fv(location.get(), value.arrayFloat.subarray(0, count * 2))
    override fun uniform2i(location: Int, v0: Int, v1: Int): Unit = gl.uniform2i(location.get(), v0, v1)
    override fun uniform2iv(location: Int, count: Int, value: Buffer): Unit = gl.uniform2iv(location.get(), value.arrayInt.subarray(0, count * 2))
    override fun uniform3f(location: Int, v0: Float, v1: Float, v2: Float): Unit = gl.uniform3f(location.get(), v0, v1, v2)
    override fun uniform3fv(location: Int, count: Int, value: Buffer): Unit = gl.uniform3fv(location.get(), value.arrayFloat.subarray(0, count * 3))
    override fun uniform3i(location: Int, v0: Int, v1: Int, v2: Int): Unit = gl.uniform3i(location.get(), v0, v1, v2)
    override fun uniform3iv(location: Int, count: Int, value: Buffer): Unit = gl.uniform3iv(location.get(), value.arrayInt.subarray(0, count * 3))
    override fun uniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit = gl.uniform4f(location.get(), v0, v1, v2, v3)
    override fun uniform4fv(location: Int, count: Int, value: Buffer): Unit = gl.uniform4fv(location.get(), value.arrayFloat.subarray(0, count * 4))
    override fun uniform4i(location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit = gl.uniform4i(location.get(), v0, v1, v2, v3)
    override fun uniform4iv(location: Int, count: Int, value: Buffer): Unit = gl.uniform4iv(location.get(), value.arrayInt.subarray(0, count * 4))
    override fun uniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: Buffer): Unit = gl.uniformMatrix2fv(location.get(), transpose, value.arrayFloat.subarray(0, count * (2*2)))
    override fun uniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: Buffer): Unit = gl.uniformMatrix3fv(location.get(), transpose, value.arrayFloat.subarray(0, count * (3*3)))
    override fun uniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: Buffer): Unit = gl.uniformMatrix4fv(location.get(), transpose, value.arrayFloat.subarray(0, count * (4*4)))
    override fun useProgram(program: Int): Unit = gl.useProgram(program.get())
    override fun validateProgram(program: Int): Unit = gl.validateProgram(program.get())
    override fun vertexAttrib1f(index: Int, x: Float): Unit = gl.vertexAttrib1f(index, x)
    override fun vertexAttrib1fv(index: Int, v: Buffer): Unit = gl.vertexAttrib1fv(index, v)
    override fun vertexAttrib2f(index: Int, x: Float, y: Float): Unit = gl.vertexAttrib2f(index, x, y)
    override fun vertexAttrib2fv(index: Int, v: Buffer): Unit = gl.vertexAttrib2fv(index, v)
    override fun vertexAttrib3f(index: Int, x: Float, y: Float, z: Float): Unit = gl.vertexAttrib3f(index, x, y, z)
    override fun vertexAttrib3fv(index: Int, v: Buffer): Unit = gl.vertexAttrib3fv(index, v)
    override fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float): Unit = gl.vertexAttrib4f(index, x, y, z, w)
    override fun vertexAttrib4fv(index: Int, v: Buffer): Unit = gl.vertexAttrib4fv(index, v)
    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, pointer: Long): Unit = gl.vertexAttribPointer(index, size, type, normalized, stride, pointer.toInt())
    override fun viewport(x: Int, y: Int, width: Int, height: Int): Unit = gl.viewport(x, y, width, height)

    private fun Float32Array.sliceIfRequired(count: Int): Float32Array = if (this.length == count) this else Float32Array(this.buffer, 0, count)

    override val extensions by lazy { (gl.getSupportedExtensions() ?: arrayOf()).toSet() }

    override val isFloatTextureSupported: Boolean by lazy {
        //println("extensions: $extensions")

        gl.getExtension("OES_texture_float_linear") != null // Also request it to support linear filtering if possible!
            || gl.getExtension("OES_texture_float") != null
            || webglVersion >= 2 // Supported by default in WebGL 2
    }

    val instancedArrays: WebGLExtension = WebGLExtension(
        this, "ANGLE_instanced_arrays", coreSince = 2, functions = listOf("drawArraysInstanced", "drawElementsInstanced", "vertexAttribDivisor"), suffix = "ANGLE"
    )
    override val isInstancedSupported: Boolean get() = instancedArrays.supported

    override fun renderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int) {
        if (webglVersion >= 2) {
            gl.asDynamic().renderbufferStorageMultisample(target, samples, internalformat, width, height)
        } else {
            TODO()
        }
    }

    override fun drawArraysInstanced(mode: Int, first: Int, count: Int, instancecount: Int) {
        gl.asDynamic().drawArraysInstanced(mode, first, count, instancecount)
    }

    override fun drawElementsInstanced(mode: Int, count: Int, type: Int, indices: Int, instancecount: Int) {
        gl.asDynamic().drawElementsInstanced(mode, count, type, indices, instancecount)
    }

    override fun vertexAttribDivisor(index: Int, divisor: Int) {
        gl.asDynamic().vertexAttribDivisor(index, divisor)
    }

    override val isUniformBuffersSupported: Boolean get() = versionInt >= 2

    override fun bindBufferRange(target: Int, index: Int, buffer: Int, offset: Int, size: Int) {
        gl.asDynamic().bindBufferRange(target, index, buffer.get<WebGLBuffer>(), offset, size)
    }

    override fun getUniformBlockIndex(program: Int, name: String): Int {
        return gl.asDynamic().getUniformBlockIndex(program.get<WebGLProgram>(), name)
    }

    override fun uniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int) {
        gl.asDynamic().uniformBlockBinding(program.get<WebGLProgram>(), uniformBlockIndex, uniformBlockBinding)
    }

    val vertexArrayObject: WebGLExtension = WebGLExtension(
        this, "OES_vertex_array_object", coreSince = 2,
        functions = listOf(
            "createVertexArrayOES", "deleteVertexArrayOES", "isVertexArrayOES",
            "bindVertexArrayOES"
        ),
        suffix = "OES"
    )
    override val isVertexArraysSupported: Boolean get() = vertexArrayObject.supported

    override fun genVertexArrays(n: Int, arrays: Buffer) {
        for (i in 0 until n) arrays.setInt32(i, (gl.asDynamic().createVertexArray().unsafeCast<WebGLVertexArrayObject>()).alloc())
    }

    override fun deleteVertexArrays(n: Int, arrays: Buffer) {
        for (i in 0 until n) {
            val v = arrays.getInt32(i)
            (gl.asDynamic().deleteVertexArray(v.free<WebGLVertexArrayObject>()))
        }
    }

    override fun bindVertexArray(array: Int) {
        gl.asDynamic().bindVertexArray(array.get<WebGLVertexArrayObject>())
    }
}

class WebGLExtension(val canvas: KmlGlJsCanvas, val name: String, val coreSince: Int = 1000, val functions: List<String> = emptyList(), val suffix: String = "") {
    private var _set: Boolean = false
    private var _value: dynamic = null
    val value: dynamic
        get() {
            if (!_set) {
                _set = true
                _value = canvas.gl.getExtension(name)
                for (func in functions) {
                    val base = func.removeSuffix(suffix)
                    val vfunc = _value[func]
                    if (jsTypeOf(vfunc) == "function") {
                        canvas.gl.asDynamic()[base] = vfunc.bind(_value)
                    }
                }
            }
            return _value
        }

    val supported: Boolean get() = (canvas.webglVersion >= coreSince) || (value != null)
}

external interface WebGLVertexArrayObject

