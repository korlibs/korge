package com.soywiz.kgl

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.concurrent.atomic.*
import kotlinx.cinterop.*
import kotlin.reflect.*

abstract class NativeBaseKmlGl : KmlGlWithExtensions() {
    val tempBufferAddress = TempBufferAddress()

    override fun activeTexture(texture: Int): Unit = tempBufferAddress { glActiveTextureExt(texture.convert()) }
    override fun attachShader(program: Int, shader: Int): Unit = tempBufferAddress { glAttachShaderExt(program.convert(), shader.convert()) }
    override fun bindAttribLocation(program: Int, index: Int, name: String): Unit = memScoped { tempBufferAddress { glBindAttribLocationExt(program.convert(), index.convert(), ((name).cstr.getPointer(this@memScoped))) } }
    override fun bindBuffer(target: Int, buffer: Int): Unit = tempBufferAddress { glBindBufferExt(target.convert(), buffer.convert()) }
    override fun bindFramebuffer(target: Int, framebuffer: Int): Unit = tempBufferAddress { glBindFramebufferExt(target.convert(), framebuffer.convert()) }
    override fun bindRenderbuffer(target: Int, renderbuffer: Int): Unit = tempBufferAddress { glBindRenderbufferExt(target.convert(), renderbuffer.convert()) }
    override fun bindTexture(target: Int, texture: Int): Unit = tempBufferAddress { glBindTextureExt(target.convert(), texture.convert()) }
    override fun blendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = tempBufferAddress { glBlendColorExt(red, green, blue, alpha) }
    override fun blendEquation(mode: Int): Unit = tempBufferAddress { glBlendEquationExt(mode.convert()) }
    override fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit = tempBufferAddress { glBlendEquationSeparateExt(modeRGB.convert(), modeAlpha.convert()) }
    override fun blendFunc(sfactor: Int, dfactor: Int): Unit = tempBufferAddress { glBlendFuncExt(sfactor.convert(), dfactor.convert()) }
    override fun blendFuncSeparate(sfactorRGB: Int, dfactorRGB: Int, sfactorAlpha: Int, dfactorAlpha: Int): Unit = tempBufferAddress { glBlendFuncSeparateExt(sfactorRGB.convert(), dfactorRGB.convert(), sfactorAlpha.convert(), dfactorAlpha.convert()) }
    override fun bufferData(target: Int, size: Int, data: FBuffer, usage: Int): Unit = tempBufferAddress { glBufferDataExt(target.convert(), size.toSizeiPtr(), data.unsafeAddress().reinterpret(), usage.convert()) }
    override fun bufferSubData(target: Int, offset: Int, size: Int, data: FBuffer): Unit = tempBufferAddress { glBufferSubDataExt(target.convert(), offset.toSizeiPtr(), size.toSizeiPtr(), data.unsafeAddress().reinterpret()) }
    override fun checkFramebufferStatus(target: Int): Int = tempBufferAddress { glCheckFramebufferStatusExt(target.convert()).convert() }
    override fun clear(mask: Int): Unit = tempBufferAddress { glClearExt(mask.convert()) }
    override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = tempBufferAddress { glClearColorExt(red, green, blue, alpha) }
    override fun clearDepthf(d: Float): Unit = tempBufferAddress { glClearDepthfExt(d) }
    override fun clearStencil(s: Int): Unit = tempBufferAddress { glClearStencilExt(s.convert()) }
    override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit = tempBufferAddress { glColorMaskExt(red.toInt().convert(), green.toInt().convert(), blue.toInt().convert(), alpha.toInt().convert()) }
    override fun compileShader(shader: Int): Unit = tempBufferAddress { glCompileShaderExt(shader.convert()) }
    override fun compressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, imageSize: Int, data: FBuffer): Unit = tempBufferAddress { glCompressedTexImage2DExt(target.convert(), level.convert(), internalformat.convert(), width.convert(), height.convert(), border.convert(), imageSize.convert(), data.unsafeAddress().reinterpret()) }
    override fun compressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, imageSize: Int, data: FBuffer): Unit = tempBufferAddress { glCompressedTexSubImage2DExt(target.convert(), level.convert(), xoffset.convert(), yoffset.convert(), width.convert(), height.convert(), format.convert(), imageSize.convert(), data.unsafeAddress().reinterpret()) }
    override fun copyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit = tempBufferAddress { glCopyTexImage2DExt(target.convert(), level.convert(), internalformat.convert(), x.convert(), y.convert(), width.convert(), height.convert(), border.convert()) }
    override fun copyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit = tempBufferAddress { glCopyTexSubImage2DExt(target.convert(), level.convert(), xoffset.convert(), yoffset.convert(), x.convert(), y.convert(), width.convert(), height.convert()) }
    override fun createProgram(): Int = tempBufferAddress { glCreateProgramExt().convert() }
    override fun createShader(type: Int): Int = tempBufferAddress { glCreateShaderExt(type.convert()).convert() }
    override fun cullFace(mode: Int): Unit = tempBufferAddress { glCullFaceExt(mode.convert()) }
    override fun deleteBuffers(n: Int, items: FBuffer): Unit = tempBufferAddress { glDeleteBuffersExt(n.convert(), items.unsafeAddress().reinterpret()) }
    override fun deleteFramebuffers(n: Int, items: FBuffer): Unit = tempBufferAddress { glDeleteFramebuffersExt(n.convert(), items.unsafeAddress().reinterpret()) }
    override fun deleteProgram(program: Int): Unit = tempBufferAddress { glDeleteProgramExt(program.convert()) }
    override fun deleteRenderbuffers(n: Int, items: FBuffer): Unit = tempBufferAddress { glDeleteRenderbuffersExt(n.convert(), items.unsafeAddress().reinterpret()) }
    override fun deleteShader(shader: Int): Unit = tempBufferAddress { glDeleteShaderExt(shader.convert()) }
    override fun deleteTextures(n: Int, items: FBuffer): Unit = tempBufferAddress { glDeleteTexturesExt(n.convert(), items.unsafeAddress().reinterpret()) }
    override fun depthFunc(func: Int): Unit = tempBufferAddress { glDepthFuncExt(func.convert()) }
    override fun depthMask(flag: Boolean): Unit = tempBufferAddress { glDepthMaskExt(flag.toInt().convert()) }
    override fun depthRangef(n: Float, f: Float): Unit = tempBufferAddress { glDepthRangefExt(n, f) }
    override fun detachShader(program: Int, shader: Int): Unit = tempBufferAddress { glDetachShaderExt(program.convert(), shader.convert()) }
    override fun disable(cap: Int): Unit = tempBufferAddress { glDisableExt(cap.convert()) }
    override fun disableVertexAttribArray(index: Int): Unit = tempBufferAddress { glDisableVertexAttribArrayExt(index.convert()) }
    override fun drawArrays(mode: Int, first: Int, count: Int): Unit = tempBufferAddress { glDrawArraysExt(mode.convert(), first.convert(), count.convert()) }
    override fun drawElements(mode: Int, count: Int, type: Int, indices: Int): Unit = tempBufferAddress { glDrawElementsExt(mode.convert(), count.convert(), type.convert(), indices.toLong().toCPointer<IntVar>()?.reinterpret()) }
    override fun enable(cap: Int): Unit = tempBufferAddress { glEnableExt(cap.convert()) }
    override fun enableVertexAttribArray(index: Int): Unit = tempBufferAddress { glEnableVertexAttribArrayExt(index.convert()) }
    override fun finish(): Unit = tempBufferAddress { glFinishExt() }
    override fun flush(): Unit = tempBufferAddress { glFlushExt() }
    override fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit = tempBufferAddress { glFramebufferRenderbufferExt(target.convert(), attachment.convert(), renderbuffertarget.convert(), renderbuffer.convert()) }
    override fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int): Unit = tempBufferAddress { glFramebufferTexture2DExt(target.convert(), attachment.convert(), textarget.convert(), texture.convert(), level.convert()) }
    override fun frontFace(mode: Int): Unit = tempBufferAddress { glFrontFaceExt(mode.convert()) }
    override fun genBuffers(n: Int, buffers: FBuffer): Unit = tempBufferAddress { glGenBuffersExt(n.convert(), buffers.unsafeAddress().reinterpret()) }
    override fun generateMipmap(target: Int): Unit = tempBufferAddress { glGenerateMipmapExt(target.convert()) }
    override fun genFramebuffers(n: Int, framebuffers: FBuffer): Unit = tempBufferAddress { glGenFramebuffersExt(n.convert(), framebuffers.unsafeAddress().reinterpret()) }
    override fun genRenderbuffers(n: Int, renderbuffers: FBuffer): Unit = tempBufferAddress { glGenRenderbuffersExt(n.convert(), renderbuffers.unsafeAddress().reinterpret()) }
    override fun genTextures(n: Int, textures: FBuffer): Unit = tempBufferAddress { glGenTexturesExt(n.convert(), textures.unsafeAddress().reinterpret()) }
    override fun getActiveAttrib(program: Int, index: Int, bufSize: Int, length: FBuffer, size: FBuffer, type: FBuffer, name: FBuffer): Unit = tempBufferAddress { glGetActiveAttribExt(program.convert(), index.convert(), bufSize.convert(), length.unsafeAddress().reinterpret(), size.unsafeAddress().reinterpret(), type.unsafeAddress().reinterpret(), name.unsafeAddress().reinterpret()) }
    override fun getActiveUniform(program: Int, index: Int, bufSize: Int, length: FBuffer, size: FBuffer, type: FBuffer, name: FBuffer): Unit = tempBufferAddress { glGetActiveUniformExt(program.convert(), index.convert(), bufSize.convert(), length.unsafeAddress().reinterpret(), size.unsafeAddress().reinterpret(), type.unsafeAddress().reinterpret(), name.unsafeAddress().reinterpret()) }
    override fun getAttachedShaders(program: Int, maxCount: Int, count: FBuffer, shaders: FBuffer): Unit = tempBufferAddress { glGetAttachedShadersExt(program.convert(), maxCount.convert(), count.unsafeAddress().reinterpret(), shaders.unsafeAddress().reinterpret()) }
    override fun getAttribLocation(program: Int, name: String): Int = memScoped { tempBufferAddress { glGetAttribLocationExt(program.convert(), ((name).cstr.getPointer(this@memScoped))).convert() } }
    override fun getUniformLocation(program: Int, name: String): Int = memScoped { tempBufferAddress { glGetUniformLocationExt(program.convert(), ((name).cstr.getPointer(this@memScoped))).convert() } }
    override fun getBooleanv(pname: Int, data: FBuffer): Unit = tempBufferAddress { glGetBooleanvExt(pname.convert(), data.unsafeAddress().reinterpret()) }
    override fun getBufferParameteriv(target: Int, pname: Int, params: FBuffer): Unit = tempBufferAddress { glGetBufferParameterivExt(target.convert(), pname.convert(), params.unsafeAddress().reinterpret()) }
    override fun getError(): Int = tempBufferAddress { glGetErrorExt().convert() }
    override fun getFloatv(pname: Int, data: FBuffer): Unit = tempBufferAddress { glGetFloatvExt(pname.convert(), data.unsafeAddress().reinterpret()) }
    override fun getFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: FBuffer): Unit = tempBufferAddress { glGetFramebufferAttachmentParameterivExt(target.convert(), attachment.convert(), pname.convert(), params.unsafeAddress().reinterpret()) }
    override fun getIntegerv(pname: Int, data: FBuffer): Unit = tempBufferAddress { glGetIntegervExt(pname.convert(), data.unsafeAddress().reinterpret()) }
    override fun getProgramInfoLog(program: Int, bufSize: Int, length: FBuffer, infoLog: FBuffer): Unit = tempBufferAddress { glGetProgramInfoLogExt(program.convert(), bufSize.convert(), length.unsafeAddress().reinterpret(), infoLog.unsafeAddress().reinterpret()) }
    override fun getRenderbufferParameteriv(target: Int, pname: Int, params: FBuffer): Unit = tempBufferAddress { glGetRenderbufferParameterivExt(target.convert(), pname.convert(), params.unsafeAddress().reinterpret()) }
    override fun getProgramiv(program: Int, pname: Int, params: FBuffer): Unit = tempBufferAddress { glGetProgramivExt(program.convert(), pname.convert(), params.unsafeAddress().reinterpret()) }
    override fun getShaderiv(shader: Int, pname: Int, params: FBuffer): Unit = tempBufferAddress { glGetShaderivExt(shader.convert(), pname.convert(), params.unsafeAddress().reinterpret()) }
    override fun getShaderInfoLog(shader: Int, bufSize: Int, length: FBuffer, infoLog: FBuffer): Unit = tempBufferAddress { glGetShaderInfoLogExt(shader.convert(), bufSize.convert(), length.unsafeAddress().reinterpret(), infoLog.unsafeAddress().reinterpret()) }
    override fun getShaderPrecisionFormat(shadertype: Int, precisiontype: Int, range: FBuffer, precision: FBuffer): Unit = tempBufferAddress { Unit }
    override fun getShaderSource(shader: Int, bufSize: Int, length: FBuffer, source: FBuffer): Unit = tempBufferAddress { glGetShaderSourceExt(shader.convert(), bufSize.convert(), length.unsafeAddress().reinterpret(), source.unsafeAddress().reinterpret()) }
    override fun getString(name: Int): String = tempBufferAddress { glGetStringExt(name.convert())?.toKString() ?: "" }
    override fun getStringi(name: Int, index: Int): String? = tempBufferAddress { glGetStringiExt(name.convert(), index.convert())?.toKString() }
    override fun getTexParameterfv(target: Int, pname: Int, params: FBuffer): Unit = tempBufferAddress { glGetTexParameterfvExt(target.convert(), pname.convert(), params.unsafeAddress().reinterpret()) }
    override fun getTexParameteriv(target: Int, pname: Int, params: FBuffer): Unit = tempBufferAddress { glGetTexParameterivExt(target.convert(), pname.convert(), params.unsafeAddress().reinterpret()) }
    override fun getUniformfv(program: Int, location: Int, params: FBuffer): Unit = tempBufferAddress { glGetUniformfvExt(program.convert(), location.convert(), params.unsafeAddress().reinterpret()) }
    override fun getUniformiv(program: Int, location: Int, params: FBuffer): Unit = tempBufferAddress { glGetUniformivExt(program.convert(), location.convert(), params.unsafeAddress().reinterpret()) }
    override fun getVertexAttribfv(index: Int, pname: Int, params: FBuffer): Unit = tempBufferAddress { glGetVertexAttribfvExt(index.convert(), pname.convert(), params.unsafeAddress().reinterpret()) }
    override fun getVertexAttribiv(index: Int, pname: Int, params: FBuffer): Unit = tempBufferAddress { glGetVertexAttribivExt(index.convert(), pname.convert(), params.unsafeAddress().reinterpret()) }
    override fun getVertexAttribPointerv(index: Int, pname: Int, pointer: FBuffer): Unit = tempBufferAddress { glGetVertexAttribPointervExt(index.convert(), pname.convert(), pointer.unsafeAddress().reinterpret()) }
    override fun hint(target: Int, mode: Int): Unit = tempBufferAddress { glHintExt(target.convert(), mode.convert()) }
    override fun isBuffer(buffer: Int): Boolean = tempBufferAddress { glIsBufferExt(buffer.convert()).toBool() }
    override fun isEnabled(cap: Int): Boolean = tempBufferAddress { glIsEnabledExt(cap.convert()).toBool() }
    override fun isFramebuffer(framebuffer: Int): Boolean = tempBufferAddress { glIsFramebufferExt(framebuffer.convert()).toBool() }
    override fun isProgram(program: Int): Boolean = tempBufferAddress { glIsProgramExt(program.convert()).toBool() }
    override fun isRenderbuffer(renderbuffer: Int): Boolean = tempBufferAddress { glIsRenderbufferExt(renderbuffer.convert()).toBool() }
    override fun isShader(shader: Int): Boolean = tempBufferAddress { glIsShaderExt(shader.convert()).toBool() }
    override fun isTexture(texture: Int): Boolean = tempBufferAddress { glIsTextureExt(texture.convert()).toBool() }
    override fun lineWidth(width: Float): Unit = tempBufferAddress { glLineWidthExt(width) }
    override fun linkProgram(program: Int): Unit = tempBufferAddress { glLinkProgramExt(program.convert()) }
    override fun pixelStorei(pname: Int, param: Int): Unit = tempBufferAddress { glPixelStoreiExt(pname.convert(), param.convert()) }
    override fun polygonOffset(factor: Float, units: Float): Unit = tempBufferAddress { glPolygonOffsetExt(factor, units) }
    override fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: FBuffer): Unit = tempBufferAddress { glReadPixelsExt(x.convert(), y.convert(), width.convert(), height.convert(), format.convert(), type.convert(), pixels.unsafeAddress()) }
    override fun releaseShaderCompiler(): Unit = tempBufferAddress { glReleaseShaderCompilerExt() }
    override fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit = tempBufferAddress { glRenderbufferStorageExt(target.convert(), internalformat.convert(), width.convert(), height.convert()) }
    override fun sampleCoverage(value: Float, invert: Boolean): Unit = tempBufferAddress { glSampleCoverageExt(value, invert.toInt().convert()) }
    override fun scissor(x: Int, y: Int, width: Int, height: Int): Unit = tempBufferAddress { glScissorExt(x.convert(), y.convert(), width.convert(), height.convert()) }
    override fun shaderBinary(count: Int, shaders: FBuffer, binaryformat: Int, binary: FBuffer, length: Int): Unit = tempBufferAddress { throw KmlGlException("shaderBinary not implemented in Native") }
    override fun shaderSource(shader: Int, string: String): Unit = tempBufferAddress {
        memScoped {
            val lengths = allocArray<IntVar>(1)
            val strings = allocArray<CPointerVar<ByteVar>>(1)
            val cstring = string.cstr.placeTo(this)
            lengths[0] = strlen(cstring).convert()
            strings[0] = cstring
            glShaderSourceExt(shader.convert(), 1.convert(), strings.reinterpret(), lengths.reinterpret())
        }
    }
    override fun stencilFunc(func: Int, ref: Int, mask: Int): Unit = tempBufferAddress { glStencilFuncExt(func.convert(), ref.convert(), mask.convert()) }
    override fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit = tempBufferAddress { glStencilFuncSeparateExt(face.convert(), func.convert(), ref.convert(), mask.convert()) }
    override fun stencilMask(mask: Int): Unit = tempBufferAddress { glStencilMaskExt(mask.convert()) }
    override fun stencilMaskSeparate(face: Int, mask: Int): Unit = tempBufferAddress { glStencilMaskSeparateExt(face.convert(), mask.convert()) }
    override fun stencilOp(fail: Int, zfail: Int, zpass: Int): Unit = tempBufferAddress { glStencilOpExt(fail.convert(), zfail.convert(), zpass.convert()) }
    override fun stencilOpSeparate(face: Int, sfail: Int, dpfail: Int, dppass: Int): Unit = tempBufferAddress { glStencilOpSeparateExt(face.convert(), sfail.convert(), dpfail.convert(), dppass.convert()) }
    override fun texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: FBuffer?): Unit = tempBufferAddress { glTexImage2DExt(target.convert(), level.convert(), internalformat.convert(), width.convert(), height.convert(), border.convert(), format.convert(), type.convert(), pixels?.unsafeAddress()) }
    override fun texImage2D(target: Int, level: Int, internalformat: Int, format: Int, type: Int, data: NativeImage): Unit = tempBufferAddress {
        val intData = (data as BitmapNativeImage).intData;
        intData.usePinned { dataPin ->
            glTexImage2DExt(target.convert(), level.convert(), internalformat.convert(), data.width.convert(), data.height.convert(), 0.convert(), format.convert(), type.convert(), dataPin.startAddressOf.reinterpret())
        }
    }
    override fun texParameterf(target: Int, pname: Int, param: Float): Unit = tempBufferAddress { glTexParameterfExt(target.convert(), pname.convert(), param) }
    override fun texParameterfv(target: Int, pname: Int, params: FBuffer): Unit = tempBufferAddress { glTexParameterfvExt(target.convert(), pname.convert(), params.unsafeAddress().reinterpret()) }
    override fun texParameteri(target: Int, pname: Int, param: Int): Unit = tempBufferAddress { glTexParameteriExt(target.convert(), pname.convert(), param.convert()) }
    override fun texParameteriv(target: Int, pname: Int, params: FBuffer): Unit = tempBufferAddress { glTexParameterivExt(target.convert(), pname.convert(), params.unsafeAddress().reinterpret()) }
    override fun texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, pixels: FBuffer): Unit = tempBufferAddress { glTexSubImage2DExt(target.convert(), level.convert(), xoffset.convert(), yoffset.convert(), width.convert(), height.convert(), format.convert(), type.convert(), pixels.unsafeAddress()) }
    override fun uniform1f(location: Int, v0: Float): Unit = tempBufferAddress { glUniform1fExt(location.convert(), v0) }
    override fun uniform1fv(location: Int, count: Int, value: FBuffer): Unit = tempBufferAddress { glUniform1fvExt(location.convert(), count.convert(), value.unsafeAddress().reinterpret()) }
    override fun uniform1i(location: Int, v0: Int): Unit = tempBufferAddress { glUniform1iExt(location.convert(), v0.convert()) }
    override fun uniform1iv(location: Int, count: Int, value: FBuffer): Unit = tempBufferAddress { glUniform1ivExt(location.convert(), count.convert(), value.unsafeAddress().reinterpret()) }
    override fun uniform2f(location: Int, v0: Float, v1: Float): Unit = tempBufferAddress { glUniform2fExt(location.convert(), v0, v1) }
    override fun uniform2fv(location: Int, count: Int, value: FBuffer): Unit = tempBufferAddress { glUniform2fvExt(location.convert(), count.convert(), value.unsafeAddress().reinterpret()) }
    override fun uniform2i(location: Int, v0: Int, v1: Int): Unit = tempBufferAddress { glUniform2iExt(location.convert(), v0.convert(), v1.convert()) }
    override fun uniform2iv(location: Int, count: Int, value: FBuffer): Unit = tempBufferAddress { glUniform2ivExt(location.convert(), count.convert(), value.unsafeAddress().reinterpret()) }
    override fun uniform3f(location: Int, v0: Float, v1: Float, v2: Float): Unit = tempBufferAddress { glUniform3fExt(location.convert(), v0, v1, v2) }
    override fun uniform3fv(location: Int, count: Int, value: FBuffer): Unit = tempBufferAddress { glUniform3fvExt(location.convert(), count.convert(), value.unsafeAddress().reinterpret()) }
    override fun uniform3i(location: Int, v0: Int, v1: Int, v2: Int): Unit = tempBufferAddress { glUniform3iExt(location.convert(), v0.convert(), v1.convert(), v2.convert()) }
    override fun uniform3iv(location: Int, count: Int, value: FBuffer): Unit = tempBufferAddress { glUniform3ivExt(location.convert(), count.convert(), value.unsafeAddress().reinterpret()) }
    override fun uniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit = tempBufferAddress { glUniform4fExt(location.convert(), v0, v1, v2, v3) }
    override fun uniform4fv(location: Int, count: Int, value: FBuffer): Unit = tempBufferAddress { glUniform4fvExt(location.convert(), count.convert(), value.unsafeAddress().reinterpret()) }
    override fun uniform4i(location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit = tempBufferAddress { glUniform4iExt(location.convert(), v0.convert(), v1.convert(), v2.convert(), v3.convert()) }
    override fun uniform4iv(location: Int, count: Int, value: FBuffer): Unit = tempBufferAddress { glUniform4ivExt(location.convert(), count.convert(), value.unsafeAddress().reinterpret()) }
    override fun uniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FBuffer): Unit = tempBufferAddress { glUniformMatrix2fvExt(location.convert(), count.convert(), transpose.toInt().convert(), value.unsafeAddress().reinterpret()) }
    override fun uniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FBuffer): Unit = tempBufferAddress { glUniformMatrix3fvExt(location.convert(), count.convert(), transpose.toInt().convert(), value.unsafeAddress().reinterpret()) }
    override fun uniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FBuffer): Unit = tempBufferAddress { glUniformMatrix4fvExt(location.convert(), count.convert(), transpose.toInt().convert(), value.unsafeAddress().reinterpret()) }
    override fun useProgram(program: Int): Unit = tempBufferAddress { glUseProgramExt(program.convert()) }
    override fun validateProgram(program: Int): Unit = tempBufferAddress { glValidateProgramExt(program.convert()) }
    override fun vertexAttrib1f(index: Int, x: Float): Unit = tempBufferAddress { glVertexAttrib1fExt(index.convert(), x) }
    override fun vertexAttrib1fv(index: Int, v: FBuffer): Unit = tempBufferAddress { glVertexAttrib1fvExt(index.convert(), v.unsafeAddress().reinterpret()) }
    override fun vertexAttrib2f(index: Int, x: Float, y: Float): Unit = tempBufferAddress { glVertexAttrib2fExt(index.convert(), x, y) }
    override fun vertexAttrib2fv(index: Int, v: FBuffer): Unit = tempBufferAddress { glVertexAttrib2fvExt(index.convert(), v.unsafeAddress().reinterpret()) }
    override fun vertexAttrib3f(index: Int, x: Float, y: Float, z: Float): Unit = tempBufferAddress { glVertexAttrib3fExt(index.convert(), x, y, z) }
    override fun vertexAttrib3fv(index: Int, v: FBuffer): Unit = tempBufferAddress { glVertexAttrib3fvExt(index.convert(), v.unsafeAddress().reinterpret()) }
    override fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float): Unit = tempBufferAddress { glVertexAttrib4fExt(index.convert(), x, y, z, w) }
    override fun vertexAttrib4fv(index: Int, v: FBuffer): Unit = tempBufferAddress { glVertexAttrib4fvExt(index.convert(), v.unsafeAddress().reinterpret()) }
    override fun viewport(x: Int, y: Int, width: Int, height: Int): Unit = tempBufferAddress { glViewportExt(x.convert(), y.convert(), width.convert(), height.convert()) }
    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, pointer: Long): Unit = tempBufferAddress { glVertexAttribPointerExt(index.convert(), size.convert(), type.convert(), normalized.toInt().convert(), stride.convert(), pointer.toCPointer<IntVar>()?.reinterpret()) }

    override val isInstancedSupported: Boolean get() = true
    override fun drawArraysInstanced(mode: Int, first: Int, count: Int, instancecount: Int): Unit = glDrawArraysInstancedExt(mode.convert(), first.convert(), count.convert(), instancecount.convert())
    override fun drawElementsInstanced(mode: Int, count: Int, type: Int, indices: Int, instancecount: Int): Unit = glDrawElementsInstancedExt(mode.convert(), count.convert(), type.convert(), indices.toLong().toCPointer<IntVar>()?.reinterpret(), instancecount.convert())
    override fun vertexAttribDivisor(index: Int, divisor: Int): Unit = glVertexAttribDivisorExt(index, divisor)

    companion object {
        const val GL_NUM_EXTENSIONS = 0x821D
        const val GL_COLOR_BUFFER_BIT = 0x00004000
        const val GL_VENDOR = 0x1F00
        const val GL_VERSION = 0x1F02

        val glDrawArraysInstancedExt by GLFunc<(GLenum, GLint, GLsizei, GLint) -> GLvoid>()
        val glDrawElementsInstancedExt by GLFunc<(GLenum, GLsizei, GLenum, GLvoidPtr, GLint) -> GLvoid>()
        val glVertexAttribDivisorExt by GLFunc<(GLint, GLint) -> GLvoid>()

        val glTexSubImage2DExt by GLFunc<(GLenum, GLint, GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, GLvoidPtr) -> GLvoid>()
        val glTexImage2DExt by GLFunc<(GLenum, GLint, GLint, GLsizei, GLsizei, GLint, GLenum, GLenum, GLvoidPtr) -> GLvoid>()

        val glTexParameteriExt by GLFunc<(GLenum, GLenum, GLint) -> GLvoid>()
        val glTexParameterfExt by GLFunc<(GLenum, GLenum, GLfloat) -> GLvoid>()

        val glTexParameterfvExt by GLFunc<(GLenum, GLenum, GLintPtr) -> GLvoid>()
        val glTexParameterivExt by GLFunc<(GLenum, GLenum, GLfloatPtr) -> GLvoid>()

        val glStencilOpExt by GLFunc<(GLenum, GLenum, GLenum) -> GLvoid>()
        val glStencilMaskExt by GLFunc<(GLuint) -> GLvoid>()
        val glReadPixelsExt by GLFunc<(GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, GLvoidPtr) -> GLvoid>()
        val glStencilFuncExt by GLFunc<(GLenum, GLint, GLuint) -> GLvoid>()

        val glScissorExt by GLFunc<(GLint, GLint, GLsizei, GLsizei) -> GLvoid>()
        val glPolygonOffsetExt by GLFunc<(GLfloat, GLfloat) -> GLvoid>()
        val glPixelStoreiExt by GLFunc<(GLenum, GLint) -> GLvoid>()
        val glLineWidthExt by GLFunc<(GLfloat) -> GLvoid>()

        val glGetTexParameterfvExt by GLFunc<(GLenum, GLenum, GLfloatPtr) -> GLvoid>()
        val glGetTexParameterivExt by GLFunc<(GLenum, GLenum, GLintPtr) -> GLvoid>()

        val glGetErrorExt by GLFunc<() -> GLenum>()
        val glGetFloatvExt by GLFunc<(GLenum, GLfloatPtr) -> GLvoid>()
        val glGetIntegervExt by GLFunc<(GLenum, GLintPtr) -> GLvoid>()
        val glGetBooleanvExt by GLFunc<(GLenum, GLbooleanPtr) -> GLvoid>()
        val glGenTexturesExt by GLFunc<(GLsizei, GLuintPtr) -> GLvoid>()
        val glDepthRangefExt by GLFunc<(GLfloat, GLfloat) -> GLvoid>()
        val glClearDepthfExt by GLFunc<(GLfloat) -> GLvoid>()
        val glDrawArraysExt by GLFunc<(GLenum, GLint, GLsizei) -> GLvoid>()
        val glDrawElementsExt by GLFunc<(GLenum, GLsizei, GLenum, GLvoidPtr) -> GLvoid>()
        val glFrontFaceExt by GLFunc<(GLenum) -> GLvoid>()
        val glFinishExt by GLFunc<() -> GLvoid>()
        val glFlushExt by GLFunc<() -> GLvoid>()
        val glEnableExt by GLFunc<(GLenum) -> GLvoid>()
        val glIsEnabledExt by GLFunc<(GLenum) -> GLboolean>()
        val glDisableExt by GLFunc<(GLenum) -> GLvoid>()
        val glDepthMaskExt by GLFunc<(GLboolean) -> GLvoid>()
        val glDepthFuncExt by GLFunc<(GLenum) -> GLvoid>()
        val glDeleteTexturesExt by GLFunc<(GLsizei, GLuintPtr) -> GLvoid>()
        val glCullFaceExt by GLFunc<(GLenum) -> GLvoid>()
        val glCopyTexImage2DExt by GLFunc<(GLenum, GLint, GLenum, GLint, GLint, GLsizei, GLsizei, GLint) -> GLvoid>()
        val glCopyTexSubImage2DExt by GLFunc<(GLenum, GLint, GLint, GLint, GLint, GLint, GLsizei, GLsizei) -> GLvoid>()
        val glColorMaskExt by GLFunc<(GLboolean, GLboolean, GLboolean, GLboolean) -> GLvoid>()
        val glClearStencilExt by GLFunc<(GLint) -> GLvoid>()
        val glClearColorExt by GLFunc<(GLfloat, GLfloat, GLfloat, GLfloat) -> GLvoid>()
        val glClearExt by GLFunc<(GLbitfield) -> GLvoid>()
        val glBindTextureExt by GLFunc<(GLenum, GLuint) -> GLvoid>()
        val glHintExt by GLFunc<(GLenum, GLenum) -> GLvoid>()
        val glViewportExt by GLFunc<(GLint, GLint, GLsizei, GLsizei) -> GLvoid>()

        val glGetStringExt by GLFunc<(GLenum) -> GLString>()
        val glGetStringiExt by GLFunc<(GLenum, GLuint) -> GLString>()
        val glVertexAttribPointerExt by GLFunc<(GLuint, GLint, GLenum, GLboolean, GLsizei, GLvoidPtr) -> Unit>()
        val glVertexAttrib4fvExt by GLFunc<(GLuint, GLfloatPtr) -> Unit>()
        val glActiveTextureExt by GLFunc<(GLenum) -> Unit>()
        val glAttachShaderExt by GLFunc<(GLuint, GLuint) -> Unit>()
        val glBindAttribLocationExt by GLFunc<(GLuint, GLuint, GLString) -> Unit>()
        val glBindBufferExt by GLFunc<(GLenum, GLuint) -> GLvoid>()
        val glBindFramebufferExt by GLFunc<(GLenum, GLuint) -> GLvoid>()
        val glBindRenderbufferExt by GLFunc<(GLenum, GLuint) -> GLvoid>()
        val glBlendColorExt by GLFunc<(GLfloat, GLfloat, GLfloat, GLfloat) -> GLvoid>()
        val glBlendEquationExt by GLFunc<(GLenum) -> GLvoid>()
        val glBlendEquationSeparateExt by GLFunc<(GLenum, GLenum) -> GLvoid>()
        val glBlendFuncExt by GLFunc<(GLenum, GLenum) -> GLvoid>()
        val glBlendFuncSeparateExt by GLFunc<(GLenum, GLenum, GLenum, GLenum) -> GLvoid>()
        val glBufferDataExt by GLFunc<(GLenum, GLsizeiPtr, GLvoidPtr, GLenum) -> GLvoid>()
        val glBufferSubDataExt by GLFunc<(GLenum, GLsizeiPtr, GLsizeiPtr, GLvoidPtr) -> GLvoid>()
        val glCheckFramebufferStatusExt by GLFunc<(GLenum) -> GLenum>()
        val glCompileShaderExt by GLFunc<(GLuint) -> GLvoid>()
        val glCompressedTexImage2DExt by GLFunc<(GLenum, GLint, GLenum, GLsizei, GLsizei, GLint, GLsizei, GLvoidPtr) -> GLvoid>()
        val glCompressedTexSubImage2DExt by GLFunc<(GLenum, GLint, GLint, GLint, GLsizei, GLsizei, GLenum, GLsizei, GLvoidPtr) -> GLvoid>()
        val glCreateProgramExt by GLFunc<() -> GLuint>()
        val glCreateShaderExt by GLFunc<(GLenum) -> GLuint>()
        val glDeleteBuffersExt by GLFunc<(GLsizei, GLuintPtr) -> GLvoid>()
        val glDeleteFramebuffersExt by GLFunc<(GLsizei, GLuintPtr) -> GLvoid>()
        val glDeleteProgramExt by GLFunc<(GLuint) -> GLvoid>()
        val glDeleteRenderbuffersExt by GLFunc<(GLsizei, GLuintPtr) -> GLvoid>()
        val glDeleteShaderExt by GLFunc<(GLuint) -> GLvoid>()
        val glDetachShaderExt by GLFunc<(GLuint, GLuint) -> GLvoid>()
        val glDisableVertexAttribArrayExt by GLFunc<(GLuint) -> GLvoid>()
        val glEnableVertexAttribArrayExt by GLFunc<(GLuint) -> GLvoid>()
        val glFramebufferRenderbufferExt by GLFunc<(GLenum, GLenum, GLenum, GLuint) -> GLvoid>()
        val glFramebufferTexture2DExt by GLFunc<(GLenum, GLenum , GLenum, GLuint, GLuint) -> GLvoid>()
        val glGenBuffersExt by GLFunc<(GLsizei, GLuintPtr) -> GLvoid>()
        val glGenerateMipmapExt by GLFunc<(GLenum) -> GLvoid>()
        val glGenFramebuffersExt by GLFunc<(GLsizei, GLuintPtr) -> GLvoid>()
        val glGenRenderbuffersExt by GLFunc<(GLsizei, GLuintPtr) -> GLvoid>()
        val glGetActiveAttribExt by GLFunc<(GLuint, GLuint, GLsizei, GLsizeiPtr, GLintPtr, GLenumPtr, GLString) -> GLvoid>()
        val glGetActiveUniformExt by GLFunc<(GLuint, GLuint, GLsizei, GLsizeiPtr, GLintPtr, GLenumPtr, GLString) -> GLvoid>()
        val glGetAttachedShadersExt by GLFunc<(GLuint, GLsizei, GLsizeiPtr, GLuintPtr) -> GLvoid>()
        val glGetAttribLocationExt by GLFunc<(GLuint, GLString) -> GLint>()
        val glGetUniformLocationExt by GLFunc<(GLuint, GLString) -> GLint>()
        val glGetBufferParameterivExt by GLFunc<(GLenum, GLenum, GLintPtr) -> GLvoid>()
        val glGetFramebufferAttachmentParameterivExt by GLFunc<(GLenum, GLenum, GLenum, GLintPtr) -> GLvoid>()
        val glGetProgramInfoLogExt by GLFunc<(GLuint, GLsizei, GLsizeiPtr, GLString) -> GLvoid>()
        val glGetRenderbufferParameterivExt by GLFunc<(GLenum, GLenum, GLintPtr) -> GLvoid>()
        val glGetProgramivExt by GLFunc<(GLuint, GLenum, GLintPtr) -> GLvoid>()
        val glGetShaderivExt by GLFunc<(GLuint, GLenum, GLintPtr) -> GLvoid>()
        val glGetShaderInfoLogExt by GLFunc<(GLuint, GLsizei, GLsizeiPtr, GLcharPtr) -> GLvoid>()
        val glGetShaderPrecisionFormatExt by GLFunc<(GLenum, GLenumPtr, GLintPtr) -> GLvoid>()
        val glGetShaderSourceExt by GLFunc<(GLuint, GLsizei, GLsizeiPtr, GLcharPtr) -> GLvoid>()
        val glGetUniformfvExt by GLFunc<(GLuint, GLint, GLfloatPtr) -> GLvoid>()
        val glGetUniformivExt by GLFunc<(GLuint, GLint, GLintPtr) -> GLvoid>()
        val glGetVertexAttribfvExt by GLFunc<(GLuint, GLenum, GLfloatPtr) -> GLvoid>()
        val glGetVertexAttribivExt by GLFunc<(GLuint, GLenum, GLintPtr) -> GLvoid>()
        val glGetVertexAttribPointervExt by GLFunc<(GLuint, GLenum, GLvoidPtrPtr) -> GLvoid>()
        val glIsBufferExt by GLFunc<(GLuint) -> GLboolean>()
        val glIsFramebufferExt by GLFunc<(GLuint) -> GLboolean>()
        val glIsProgramExt by GLFunc<(GLuint) -> GLboolean>()
        val glIsRenderbufferExt by GLFunc<(GLuint) -> GLboolean>()
        val glIsTextureExt by GLFunc<(GLuint) -> GLboolean>()
        val glIsShaderExt by GLFunc<(GLuint) -> GLboolean>()
        val glLinkProgramExt by GLFunc<(GLuint) -> GLvoid>()
        val glReleaseShaderCompilerExt by GLFunc<() -> GLvoid>()
        val glRenderbufferStorageExt by GLFunc<(GLenum, GLenum, GLsizei, GLsizei) -> GLvoid>()
        val glSampleCoverageExt by GLFunc<(GLfloat, GLboolean) -> GLvoid>()
        val glShaderBinaryExt by GLFunc<(GLsizei, GLuintPtr, GLenum, GLvoidPtr, GLsizei) -> GLvoid>()
        val glShaderSourceExt by GLFunc<(GLuint, GLsizei, GLcharPtrPtr, GLintPtr) -> GLvoid>()
        val glStencilFuncSeparateExt by GLFunc<(GLenum, GLenum, GLint, GLuint) -> GLvoid>()
        val glStencilMaskSeparateExt by GLFunc<(GLenum, GLuint) -> GLvoid>()
        val glStencilOpSeparateExt by GLFunc<(GLenum, GLenum, GLenum, GLenum) -> GLvoid>()
        val glUniform1fExt by GLFunc<(GLint, GLfloat) -> GLvoid>()
        val glUniform2fExt by GLFunc<(GLint, GLfloat, GLfloat) -> GLvoid>()
        val glUniform3fExt by GLFunc<(GLint, GLfloat, GLfloat, GLfloat) -> GLvoid>()
        val glUniform4fExt by GLFunc<(GLint, GLfloat, GLfloat, GLfloat, GLfloat) -> GLvoid>()
        val glUniform1iExt by GLFunc<(GLint, GLint) -> GLvoid>()
        val glUniform2iExt by GLFunc<(GLint, GLint, GLint) -> GLvoid>()
        val glUniform3iExt by GLFunc<(GLint, GLint, GLint, GLint) -> GLvoid>()
        val glUniform4iExt by GLFunc<(GLint, GLint, GLint, GLint, GLint) -> GLvoid>()
        val glUniform1fvExt by GLFunc<(GLint, GLsizei, GLfloatPtr) -> GLvoid>()
        val glUniform2fvExt by GLFunc<(GLint, GLsizei, GLfloatPtr) -> GLvoid>()
        val glUniform3fvExt by GLFunc<(GLint, GLsizei, GLfloatPtr) -> GLvoid>()
        val glUniform4fvExt by GLFunc<(GLint, GLsizei, GLfloatPtr) -> GLvoid>()
        val glUniform1ivExt by GLFunc<(GLint, GLsizei, GLintPtr) -> GLvoid>()
        val glUniform2ivExt by GLFunc<(GLint, GLsizei, GLintPtr) -> GLvoid>()
        val glUniform3ivExt by GLFunc<(GLint, GLsizei, GLintPtr) -> GLvoid>()
        val glUniform4ivExt by GLFunc<(GLint, GLsizei, GLintPtr) -> GLvoid>()
        val glUniformMatrix2fvExt by GLFunc<(GLint, GLsizei, GLboolean, GLfloatPtr) -> GLvoid>()
        val glUniformMatrix3fvExt by GLFunc<(GLint, GLsizei, GLboolean, GLfloatPtr) -> GLvoid>()
        val glUniformMatrix4fvExt by GLFunc<(GLint, GLsizei, GLboolean, GLfloatPtr) -> GLvoid>()
        val glUseProgramExt by GLFunc<(GLuint) -> GLvoid>()
        val glValidateProgramExt by GLFunc<(GLuint) -> GLvoid>()
        val glVertexAttrib1fExt by GLFunc<(GLuint, GLfloat) -> GLvoid>()
        val glVertexAttrib2fExt by GLFunc<(GLuint, GLfloat, GLfloat) -> GLvoid>()
        val glVertexAttrib3fExt by GLFunc<(GLuint, GLfloat, GLfloat, GLfloat) -> GLvoid>()
        val glVertexAttrib4fExt by GLFunc<(GLuint, GLfloat, GLfloat, GLfloat, GLfloat) -> GLvoid>()
        val glVertexAttrib1fvExt by GLFunc<(GLuint, GLfloatPtr) -> GLvoid>()
        val glVertexAttrib2fvExt by GLFunc<(GLuint, GLfloatPtr) -> GLvoid>()
        val glVertexAttrib3fvExt by GLFunc<(GLuint, GLfloatPtr) -> GLvoid>()
    }
}

typealias GLString = CPointer<ByteVar>?
typealias GLuint = Int
typealias GLint = Int
typealias GLsizei = Int
typealias GLboolean = Int
typealias GLenum = Int
typealias GLbitfield = Int
typealias GLfloat = Float
typealias GLdouble = Double
typealias GLfloatPtr = CPointer<FloatVar>?
typealias GLuintPtr = CPointer<UIntVar>?
typealias GLbooleanPtr = CPointer<IntVar>?
typealias GLintPtr = CPointer<IntVar>?
typealias GLenumPtr = CPointer<IntVar>?
typealias GLsizeiPtr = CPointer<IntVar>?
typealias GLcharPtr = CPointer<UByteVar>?
typealias GLcharPtrPtr = CPointer<ByteVar>?
typealias GLvoidPtrPtr = CPointer<ByteVar>?
typealias GLvoidPtr = CPointer<ByteVar>?
typealias GLvoid = Unit

internal fun Int.toSizeiPtr() = this.toLong().toCPointer<IntVar>()

open class GLFuncBase<T : Function<*>>(val name: String? = null) {
    private var _set = korAtomic(false)
    private var _value = korAtomic<CPointer<CFunction<T>>?>(null)

    protected fun _getValue(property: KProperty<*>): CPointer<CFunction<T>>? {
        if (!_set.value) {
            _set.value = true
            _value.value = glGetProcAddressT(name ?: property.name.removeSuffix("Ext"))
        }
        return _value.value
    }
}

class GLFunc<T : Function<*>>(name: String? = null) : GLFuncBase<T>(name) {
    operator fun getValue(obj: Any?, property: KProperty<*>): CPointer<CFunction<T>> = _getValue(property)!!
}

class GLFuncNull<T : Function<*>>(name: String? = null) : GLFuncBase<T>(name) {
    operator fun getValue(obj: Any?, property: KProperty<*>): CPointer<CFunction<T>>? = _getValue(property)
}

class ImportFunctionNotFoundException(val name: String) : RuntimeException("Can't find function: '$name'")

internal fun glGetProcAddressAny(name: String): COpaquePointer {
    return glGetProcAddressAnyOrNull(name) ?: throw ImportFunctionNotFoundException(name)
}

internal fun <T : CPointer<*>> glGetProcAddressT(name: String): T {
    return glGetProcAddressAny(name).reinterpret2<T>()
}

internal fun <T : CPointer<*>> CPointer<*>.reinterpret2(): T = this.toLong().toCPointer<IntVar>() as T

internal fun strlen(str: CPointer<ByteVar>): Int {
    var n = 0
    while (str[n++].toInt() != 0) Unit
    return n - 1
}

fun Boolean.toBool(): Boolean = this
fun Byte.toBool(): Boolean = this.toInt() != 0
fun Int.toBool(): Boolean = this != 0
fun Long.toBool(): Boolean = this != 0L

fun UByte.toBool(): Boolean = this.toUInt() != 0u
fun UInt.toBool(): Boolean = this != 0u
fun ULong.toBool(): Boolean = this != 0uL

fun CPointer<UByteVar>.toKString(): String = this.reinterpret<ByteVar>().toKString()
//inline fun <reified R : Any> Boolean.convert(): R = (if (this) 1 else 0).convert() // @TODO: Doesn't work

fun Boolean.toInt(): Int = if (this) 1 else 0

fun Int.convertSize(): Long = this.toLong() // For 64-bit
fun Float.convertFloat(): Double = this.toDouble() // For 64-bit

class TempBufferAddress {
	val pool = arrayListOf<Pinned<ByteArray>>()
	companion object {
		val ARRAY1 = ByteArray(1)
	}
	fun FBuffer.unsafeAddress(): CPointer<ByteVar> {
		val byteArray = this.mem.data
		val rbyteArray = if (byteArray.size > 0) byteArray else ARRAY1
		val pin = rbyteArray.pin()
		pool += pin
		return pin.addressOf(0)
	}

	fun start() {
		pool.clear()
	}

	fun dispose() {
		// Kotlin-native: Try to avoid allocating an iterator (lists not optimized yet)
		for (n in 0 until pool.size) pool[n].unpin()
		//for (p in pool) p.unpin()
		pool.clear()
	}

	inline operator fun <T> invoke(callback: TempBufferAddress.() -> T): T {
		start()
		try {
			return callback()
		} finally {
			dispose()
		}
	}
}
