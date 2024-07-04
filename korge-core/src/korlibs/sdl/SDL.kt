package korlibs.sdl

import korlibs.annotations.*
import korlibs.ffi.*
import korlibs.image.bitmap.*
import korlibs.kgl.*
import korlibs.math.*
import korlibs.memory.*
import korlibs.platform.*

val SDLPath by lazy {
    //"https://github.com/libsdl-org/SDL/releases/download/release-2.30.5/SDL2-2.30.5-win32-x64.zip"
    //"SDL"
    when {
        Platform.isMac -> "/Library/Frameworks/SDL2.framework/SDL2"
        else -> "SDL2.dll"
    }
}

@KeepNames
open class SDL : FFILib(SDLPath) {
    companion object {
        const val SDL_WINDOWPOS_UNDEFINED = 0x1FFF0000
        const val SDL_WINDOWPOS_CENTERED = 0x2FFF0000

        // https://wiki.libsdl.org/SDL2/SDL_WindowFlags
        // SDL_WindowFlags
        const val SDL_WINDOW_OPENGL = 0x2
        const val SDL_WINDOW_RESIZABLE = 0x20

        // https://wiki.libsdl.org/SDL2/SDL_Event
        // SDL_Event
        const val SDL_QUIT = 0x100
    }

    val SDL_Init by func<(flags: Int) -> Int>()
    val SDL_CreateWindow by func<(title: String, x: Int, y: Int, w: Int, h: Int, flags: Int) -> FFIPointer>()
    val SDL_SetWindowTitle by func<(window: FFIPointer, title: String) -> Unit>()
    val SDL_SetWindowSize by func<(window: FFIPointer, w: Int, h: Int) -> Unit>()
    val SDL_SetWindowPosition by func<(window: FFIPointer, x: Int, y: Int) -> Unit>()
    val SDL_ShowWindow by func<(window: FFIPointer) -> Unit>()
    val SDL_RaiseWindow by func<(window: FFIPointer) -> Unit>()
    val SDL_PollEvent by func<(event: FFIPointer) -> Boolean>()
    val SDL_Quit by func<() -> Unit>()

    // OpenGL related functions
    val SDL_GL_CreateContext by func<(window: FFIPointer) -> FFIPointer>()
    val SDL_GL_DeleteContext by func<(context: FFIPointer) -> Unit>()
    val SDL_GL_MakeCurrent by func<(window: FFIPointer?, context: FFIPointer?) -> Int>()
    val SDL_GL_SwapWindow by func<(window: FFIPointer) -> Unit>()
}


private typealias GLboolean = Int // Check https://github.com/korlibs/korge/issues/268#issuecomment-729056184 for details
private typealias GLint = Int
private typealias GLuint = Int
private typealias GLsizei = Int
private typealias GLenum = Int
private typealias GLbitfield = Int
private typealias GLfloat = Float
private typealias GLvoid = Unit
private typealias GLvoidPtr = FFIPointer?
private typealias GLintPtr = FFIPointer?
private typealias GLenumPtr = FFIPointer?
private typealias GLuintPtr = FFIPointer?
private typealias GLfloatPtr = FFIPointer?
private typealias GLbooleanPtr = FFIPointer?
private typealias GLsizeiPtr = FFIPointer?
private typealias GLcharPtr = FFIPointer?
private typealias GLvoidPtrPtr = FFIPointer?
private typealias GLcharPtrPtr = FFIPointer?
private typealias GLString = String?



@KeepNames
open class OpenGL : FFILib("/System/Library/Frameworks/OpenGL.framework/OpenGL") {
    companion object {
        const val GL_COLOR_BUFFER_BIT = 0x4000
    }

    //val glClearColor by func<(r: Float, g: Float, b: Float, a: Float) -> Unit>()
    //val glClear by func<(mask: Int) -> Unit>()
    //val glViewport by func<(x: Int, y: Int, width: Int, height: Int) -> Unit>()
    //val glFlush by func<() -> Unit>()
    //val glFinish by func<() -> Unit>()
    
    val glDrawArraysInstanced by func<(GLenum, GLint, GLsizei, GLint) -> GLvoid>()
    val glDrawElementsInstanced by func<(GLenum, GLsizei, GLenum, GLvoidPtr, GLint) -> GLvoid>()
    val glVertexAttribDivisor by func<(GLint, GLint) -> GLvoid>()

    val glTexSubImage2D by func<(GLenum, GLint, GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, GLvoidPtr) -> GLvoid>()
    val glTexImage2D by func<(GLenum, GLint, GLint, GLsizei, GLsizei, GLint, GLenum, GLenum, GLvoidPtr) -> GLvoid>()

    val glTexParameteri by func<(GLenum, GLenum, GLint) -> GLvoid>()
    val glTexParameterf by func<(GLenum, GLenum, GLfloat) -> GLvoid>()

    val glTexParameterfv by func<(GLenum, GLenum, GLintPtr) -> GLvoid>()
    val glTexParameteriv by func<(GLenum, GLenum, GLfloatPtr) -> GLvoid>()

    val glStencilOp by func<(GLenum, GLenum, GLenum) -> GLvoid>()
    val glStencilMask by func<(GLuint) -> GLvoid>()
    val glReadPixels by func<(GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, GLvoidPtr) -> GLvoid>()
    val glStencilFunc by func<(GLenum, GLint, GLuint) -> GLvoid>()

    val glScissor by func<(GLint, GLint, GLsizei, GLsizei) -> GLvoid>()
    val glPolygonOffset by func<(GLfloat, GLfloat) -> GLvoid>()
    val glPixelStorei by func<(GLenum, GLint) -> GLvoid>()
    val glLineWidth by func<(GLfloat) -> GLvoid>()

    val glGetTexParameterfv by func<(GLenum, GLenum, GLfloatPtr) -> GLvoid>()
    val glGetTexParameteriv by func<(GLenum, GLenum, GLintPtr) -> GLvoid>()

    val glGetError by func<() -> GLenum>()
    val glGetFloatv by func<(GLenum, GLfloatPtr) -> GLvoid>()
    val glGetIntegerv by func<(GLenum, GLintPtr) -> GLvoid>()
    val glGetBooleanv by func<(GLenum, GLbooleanPtr) -> GLvoid>()
    val glGenTextures by func<(GLsizei, GLuintPtr) -> GLvoid>()
    val glDepthRange by func<(GLfloat, GLfloat) -> GLvoid>()
    val glDepthRangef by func<(GLfloat, GLfloat) -> GLvoid>()
    val glClearDepth by func<(GLfloat) -> GLvoid>()
    val glClearDepthf by func<(GLfloat) -> GLvoid>()
    val glDrawArrays by func<(GLenum, GLint, GLsizei) -> GLvoid>()
    val glDrawElements by func<(GLenum, GLsizei, GLenum, GLvoidPtr) -> GLvoid>()
    val glFrontFace by func<(GLenum) -> GLvoid>()
    val glFinish by func<() -> GLvoid>()
    val glFlush by func<() -> GLvoid>()
    val glEnable by func<(GLenum) -> GLvoid>()
    val glIsEnabled by func<(GLenum) -> GLboolean>()
    val glDisable by func<(GLenum) -> GLvoid>()
    val glDepthMask by func<(GLboolean) -> GLvoid>()
    val glDepthFunc by func<(GLenum) -> GLvoid>()
    val glDeleteTextures by func<(GLsizei, GLuintPtr) -> GLvoid>()
    val glCullFace by func<(GLenum) -> GLvoid>()
    val glCopyTexImage2D by func<(GLenum, GLint, GLenum, GLint, GLint, GLsizei, GLsizei, GLint) -> GLvoid>()
    val glCopyTexSubImage2D by func<(GLenum, GLint, GLint, GLint, GLint, GLint, GLsizei, GLsizei) -> GLvoid>()
    val glColorMask by func<(GLboolean, GLboolean, GLboolean, GLboolean) -> GLvoid>()
    val glClearStencil by func<(GLint) -> GLvoid>()
    val glClearColor by func<(GLfloat, GLfloat, GLfloat, GLfloat) -> GLvoid>()
    val glClear by func<(GLbitfield) -> GLvoid>()
    val glBindTexture by func<(GLenum, GLuint) -> GLvoid>()
    val glHint by func<(GLenum, GLenum) -> GLvoid>()
    val glViewport by func<(GLint, GLint, GLsizei, GLsizei) -> GLvoid>()

    val glGetString by func<(GLenum) -> GLString>()
    val glGetStringi by func<(GLenum, GLuint) -> GLString>()
    val glVertexAttribPointer by func<(GLuint, GLint, GLenum, GLboolean, GLsizei, GLvoidPtr) -> Unit>()
    val glVertexAttrib4fv by func<(GLuint, GLfloatPtr) -> Unit>()
    val glActiveTexture by func<(GLenum) -> Unit>()
    val glAttachShader by func<(GLuint, GLuint) -> Unit>()
    val glBindAttribLocation by func<(GLuint, GLuint, GLString) -> Unit>()
    val glBindBuffer by func<(GLenum, GLuint) -> GLvoid>()
    val glBindFramebuffer by func<(GLenum, GLuint) -> GLvoid>()
    val glBindRenderbuffer by func<(GLenum, GLuint) -> GLvoid>()
    val glBlendColor by func<(GLfloat, GLfloat, GLfloat, GLfloat) -> GLvoid>()
    val glBlendEquation by func<(GLenum) -> GLvoid>()
    val glBlendEquationSeparate by func<(GLenum, GLenum) -> GLvoid>()
    val glBlendFunc by func<(GLenum, GLenum) -> GLvoid>()
    val glBlendFuncSeparate by func<(GLenum, GLenum, GLenum, GLenum) -> GLvoid>()
    val glBufferData by func<(GLenum, GLsizeiPtr, GLvoidPtr, GLenum) -> GLvoid>()
    val glBufferSubData by func<(GLenum, GLsizeiPtr, GLsizeiPtr, GLvoidPtr) -> GLvoid>()
    val glCheckFramebufferStatus by func<(GLenum) -> GLenum>()
    val glCompileShader by func<(GLuint) -> GLvoid>()
    val glCompressedTexImage2D by func<(GLenum, GLint, GLenum, GLsizei, GLsizei, GLint, GLsizei, GLvoidPtr) -> GLvoid>()
    val glCompressedTexSubImage2D by func<(GLenum, GLint, GLint, GLint, GLsizei, GLsizei, GLenum, GLsizei, GLvoidPtr) -> GLvoid>()
    val glCreateProgram by func<() -> GLuint>()
    val glCreateShader by func<(GLenum) -> GLuint>()
    val glDeleteBuffers by func<(GLsizei, GLuintPtr) -> GLvoid>()
    val glDeleteFramebuffers by func<(GLsizei, GLuintPtr) -> GLvoid>()
    val glDeleteProgram by func<(GLuint) -> GLvoid>()
    val glDeleteRenderbuffers by func<(GLsizei, GLuintPtr) -> GLvoid>()
    val glDeleteShader by func<(GLuint) -> GLvoid>()
    val glDetachShader by func<(GLuint, GLuint) -> GLvoid>()
    val glDisableVertexAttribArray by func<(GLuint) -> GLvoid>()
    val glEnableVertexAttribArray by func<(GLuint) -> GLvoid>()
    val glFramebufferRenderbuffer by func<(GLenum, GLenum, GLenum, GLuint) -> GLvoid>()
    val glFramebufferTexture2D by func<(GLenum, GLenum , GLenum, GLuint, GLuint) -> GLvoid>()
    val glGenBuffers by func<(GLsizei, GLuintPtr) -> GLvoid>()
    val glGenerateMipmap by func<(GLenum) -> GLvoid>()
    val glGenFramebuffers by func<(GLsizei, GLuintPtr) -> GLvoid>()
    val glGenRenderbuffers by func<(GLsizei, GLuintPtr) -> GLvoid>()
    val glGetActiveAttrib by func<(GLuint, GLuint, GLsizei, GLsizeiPtr, GLintPtr, GLenumPtr, GLString) -> GLvoid>()
    val glGetActiveUniform by func<(GLuint, GLuint, GLsizei, GLsizeiPtr, GLintPtr, GLenumPtr, GLString) -> GLvoid>()
    val glGetAttachedShaders by func<(GLuint, GLsizei, GLsizeiPtr, GLuintPtr) -> GLvoid>()
    val glGetAttribLocation by func<(GLuint, GLString) -> GLint>()
    val glGetUniformLocation by func<(GLuint, GLString) -> GLint>()
    val glGetBufferParameteriv by func<(GLenum, GLenum, GLintPtr) -> GLvoid>()
    val glGetFramebufferAttachmentParameteriv by func<(GLenum, GLenum, GLenum, GLintPtr) -> GLvoid>()
    //val glGetProgramInfoLog by func<(GLuint, GLsizei, GLsizeiPtr, GLString) -> GLvoid>()
    val glGetProgramInfoLog by func<(GLuint, GLsizei, GLsizeiPtr, GLcharPtr) -> GLvoid>()
    val glGetRenderbufferParameteriv by func<(GLenum, GLenum, GLintPtr) -> GLvoid>()
    val glGetProgramiv by func<(GLuint, GLenum, GLintPtr) -> GLvoid>()
    val glGetShaderiv by func<(GLuint, GLenum, GLintPtr) -> GLvoid>()
    val glGetShaderInfoLog by func<(GLuint, GLsizei, GLsizeiPtr, GLcharPtr) -> GLvoid>()
    val glGetShaderPrecisionFormat by func<(GLenum, GLenumPtr, GLintPtr) -> GLvoid>()
    val glGetShaderSource by func<(GLuint, GLsizei, GLsizeiPtr, GLcharPtr) -> GLvoid>()
    val glGetUniformfv by func<(GLuint, GLint, GLfloatPtr) -> GLvoid>()
    val glGetUniformiv by func<(GLuint, GLint, GLintPtr) -> GLvoid>()
    val glGetVertexAttribfv by func<(GLuint, GLenum, GLfloatPtr) -> GLvoid>()
    val glGetVertexAttribiv by func<(GLuint, GLenum, GLintPtr) -> GLvoid>()
    val glGetVertexAttribPointerv by func<(GLuint, GLenum, GLvoidPtrPtr) -> GLvoid>()
    val glIsBuffer by func<(GLuint) -> GLboolean>()
    val glIsFramebuffer by func<(GLuint) -> GLboolean>()
    val glIsProgram by func<(GLuint) -> GLboolean>()
    val glIsRenderbuffer by func<(GLuint) -> GLboolean>()
    val glIsTexture by func<(GLuint) -> GLboolean>()
    val glIsShader by func<(GLuint) -> GLboolean>()
    val glLinkProgram by func<(GLuint) -> GLvoid>()
    val glReleaseShaderCompiler by func<() -> GLvoid>()
    val glRenderbufferStorage by func<(GLenum, GLenum, GLsizei, GLsizei) -> GLvoid>()
    val glSampleCoverage by func<(GLfloat, GLboolean) -> GLvoid>()
    val glShaderBinary by func<(GLsizei, GLuintPtr, GLenum, GLvoidPtr, GLsizei) -> GLvoid>()
    val glShaderSource by func<(GLuint, GLsizei, GLcharPtrPtr, GLintPtr) -> GLvoid>()
    val glStencilFuncSeparate by func<(GLenum, GLenum, GLint, GLuint) -> GLvoid>()
    val glStencilMaskSeparate by func<(GLenum, GLuint) -> GLvoid>()
    val glStencilOpSeparate by func<(GLenum, GLenum, GLenum, GLenum) -> GLvoid>()
    val glUniform1f by func<(GLint, GLfloat) -> GLvoid>()
    val glUniform2f by func<(GLint, GLfloat, GLfloat) -> GLvoid>()
    val glUniform3f by func<(GLint, GLfloat, GLfloat, GLfloat) -> GLvoid>()
    val glUniform4f by func<(GLint, GLfloat, GLfloat, GLfloat, GLfloat) -> GLvoid>()
    val glUniform1i by func<(GLint, GLint) -> GLvoid>()
    val glUniform2i by func<(GLint, GLint, GLint) -> GLvoid>()
    val glUniform3i by func<(GLint, GLint, GLint, GLint) -> GLvoid>()
    val glUniform4i by func<(GLint, GLint, GLint, GLint, GLint) -> GLvoid>()
    val glUniform1fv by func<(GLint, GLsizei, GLfloatPtr) -> GLvoid>()
    val glUniform2fv by func<(GLint, GLsizei, GLfloatPtr) -> GLvoid>()
    val glUniform3fv by func<(GLint, GLsizei, GLfloatPtr) -> GLvoid>()
    val glUniform4fv by func<(GLint, GLsizei, GLfloatPtr) -> GLvoid>()
    val glUniform1iv by func<(GLint, GLsizei, GLintPtr) -> GLvoid>()
    val glUniform2iv by func<(GLint, GLsizei, GLintPtr) -> GLvoid>()
    val glUniform3iv by func<(GLint, GLsizei, GLintPtr) -> GLvoid>()
    val glUniform4iv by func<(GLint, GLsizei, GLintPtr) -> GLvoid>()
    val glUniformMatrix2fv by func<(GLint, GLsizei, GLboolean, GLfloatPtr) -> GLvoid>()
    val glUniformMatrix3fv by func<(GLint, GLsizei, GLboolean, GLfloatPtr) -> GLvoid>()
    val glUniformMatrix4fv by func<(GLint, GLsizei, GLboolean, GLfloatPtr) -> GLvoid>()
    val glUseProgram by func<(GLuint) -> GLvoid>()
    val glValidateProgram by func<(GLuint) -> GLvoid>()
    val glVertexAttrib1f by func<(GLuint, GLfloat) -> GLvoid>()
    val glVertexAttrib2f by func<(GLuint, GLfloat, GLfloat) -> GLvoid>()
    val glVertexAttrib3f by func<(GLuint, GLfloat, GLfloat, GLfloat) -> GLvoid>()
    val glVertexAttrib4f by func<(GLuint, GLfloat, GLfloat, GLfloat, GLfloat) -> GLvoid>()
    val glVertexAttrib1fv by func<(GLuint, GLfloatPtr) -> GLvoid>()
    val glVertexAttrib2fv by func<(GLuint, GLfloatPtr) -> GLvoid>()
    val glVertexAttrib3fv by func<(GLuint, GLfloatPtr) -> GLvoid>()

    val glBindBufferRange by func<(GLenum, GLuint, GLuint, GLintPtr, GLsizeiPtr) -> GLvoid>()
    val glGetUniformBlockIndex by func<(GLuint, GLString) -> GLint>()
    val glUniformBlockBinding by func<(GLuint, GLuint, GLuint) -> GLvoid>()
    val glGenVertexArrays by func<(GLsizei, GLuintPtr) -> GLvoid>()
    val glDeleteVertexArrays by func<(GLsizei, GLuintPtr) -> GLvoid>()
    val glBindVertexArray by func<(GLuint) -> GLvoid>()
}


// @TODO: Optimize this
fun arraycopy(src: Buffer, srcPos: Int, dst: FFIPointer, dstPos: Int, length: Int) {
    for (n in 0 until length) dst.set8(src.getS8(srcPos + n), dstPos + n)
}

// @TODO: Optimize this
fun arraycopy(src: FFIPointer, srcPos: Int, dst: Buffer, dstPos: Int, length: Int) {
    for (n in 0 until length) dst.set8(dstPos + n, src.getS8(srcPos + n))
}

// @TODO: Optimize this
fun arraycopy(src: FFIPointer, srcPos: Int, dst: FFIPointer, dstPos: Int, length: Int) {
    for (n in 0 until length) dst.set8(src.getS8(srcPos + n), dstPos + n)
}

class KmlGlOpenGL(val gl: OpenGL = OpenGL()) : KmlGl() {
    fun Int.toBool(): Boolean = this != 0

    val tempMem = CreateFFIMemory(16 * 1024)
    inline fun <T> tempBufferAddress(minSize: Int, block: (FFIPointer) -> T): T =
        (if (minSize < 16 * 1024) tempMem else CreateFFIMemory(minSize)).usePointer(block)
    inline fun <T> tempBufferAddressIn(data: Buffer, nbytes: Int = data.sizeInBytes, block: (FFIPointer) -> T): T = tempBufferAddress(nbytes) { ptr ->
        arraycopy(data, 0, ptr, 0, nbytes)
        block(ptr)
    }
    inline fun <T> tempBufferAddressOut(data: Buffer, nbytes: Int = data.sizeInBytes, block: (FFIPointer) -> T): T = tempBufferAddress(nbytes) { ptr ->
        block(ptr).also { arraycopy(ptr, 0, data, 0, nbytes) }
    }

    fun Int.convert(): FFIPointer? = CreateFFIPointer(this.toLong())
    fun Long.convert(): FFIPointer? = CreateFFIPointer(this)

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
    override fun bufferData(target: Int, size: Int, data: Buffer, usage: Int): Unit = gl.glBufferData(target, size.convert(), data.pointer, usage)
    override fun bufferSubData(target: Int, offset: Int, size: Int, data: Buffer): Unit = gl.glBufferSubData(target, offset.convert(), size.convert(), data.pointer)
    override fun checkFramebufferStatus(target: Int): Int = gl.glCheckFramebufferStatus(target)
    override fun clear(mask: Int): Unit = gl.glClear(mask)
    override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = gl.glClearColor(red, green, blue, alpha)
    override fun clearDepthf(d: Float): Unit = gl.glClearDepthf(d)
    override fun clearStencil(s: Int): Unit = gl.glClearStencil(s)
    override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit = gl.glColorMask(red.toInt(), green.toInt(), blue.toInt(), alpha.toInt())
    override fun compileShader(shader: Int): Unit = gl.glCompileShader(shader)
    override fun compressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, imageSize: Int, data: Buffer): Unit =
        tempBufferAddressIn(data) { gl.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, it) }
    override fun compressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, imageSize: Int, data: Buffer): Unit {
        tempBufferAddressIn(data) { gl.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, it) }
    }
    override fun copyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit = gl.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border)
    override fun copyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit = gl.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
    override fun createProgram(): Int = gl.glCreateProgram()
    override fun createShader(type: Int): Int = gl.glCreateShader(type)
    override fun cullFace(mode: Int): Unit = gl.glCullFace(mode)
    override fun deleteBuffers(n: Int, items: Buffer): Unit = tempBufferAddressIn(items, n * 4) { gl.glDeleteBuffers(n, it) }
    override fun deleteFramebuffers(n: Int, items: Buffer): Unit = tempBufferAddressIn(items, n * 4) { gl.glDeleteFramebuffers(n, it) }
    override fun deleteProgram(program: Int): Unit = gl.glDeleteProgram(program)
    override fun deleteRenderbuffers(n: Int, items: Buffer): Unit = tempBufferAddressIn(items, n * 4) { gl.glDeleteRenderbuffers(n, it) }
    override fun deleteShader(shader: Int): Unit = gl.glDeleteShader(shader)
    override fun deleteTextures(n: Int, items: Buffer): Unit = tempBufferAddressIn(items, n * 4) { gl.glDeleteTextures(n, it) }
    override fun depthFunc(func: Int): Unit = gl.glDepthFunc(func)
    override fun depthMask(flag: Boolean): Unit = gl.glDepthMask(flag.toInt())
    override fun depthRangef(n: Float, f: Float): Unit = gl.glDepthRangef(n, f)
    override fun detachShader(program: Int, shader: Int): Unit = gl.glDetachShader(program, shader)
    override fun disable(cap: Int): Unit = gl.glDisable(cap)
    override fun disableVertexAttribArray(index: Int): Unit = gl.glDisableVertexAttribArray(index)
    override fun drawArrays(mode: Int, first: Int, count: Int): Unit = gl.glDrawArrays(mode, first, count)
    override fun drawElements(mode: Int, count: Int, type: Int, indices: Int): Unit = gl.glDrawElements(mode, count, type, indices.convert())
    override fun enable(cap: Int): Unit = gl.glEnable(cap)
    override fun enableVertexAttribArray(index: Int): Unit = gl.glEnableVertexAttribArray(index)
    override fun finish(): Unit = gl.glFinish()
    override fun flush(): Unit = gl.glFlush()
    override fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit = gl.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
    override fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int): Unit = gl.glFramebufferTexture2D(target, attachment, textarget, texture, level)
    override fun frontFace(mode: Int): Unit = gl.glFrontFace(mode)
    override fun genBuffers(n: Int, buffers: Buffer): Unit = tempBufferAddressOut(buffers, n) { gl.glGenBuffers(n, it) }
    override fun generateMipmap(target: Int): Unit = gl.glGenerateMipmap(target)
    override fun genFramebuffers(n: Int, framebuffers: Buffer): Unit = tempBufferAddressOut(framebuffers, n * 4) { gl.glGenFramebuffers(n, it) }
    override fun genRenderbuffers(n: Int, renderbuffers: Buffer): Unit = tempBufferAddressOut(renderbuffers, n * 4) { gl.glGenRenderbuffers(n, it) }
    override fun genTextures(n: Int, textures: Buffer): Unit = tempBufferAddressOut(textures, n * 4) { gl.glGenTextures(n, it) }
    //override fun getActiveAttrib(program: Int, index: Int, bufSize: Int, length: Buffer, size: Buffer, type: Buffer, name: Buffer): Unit = gl.glGetActiveAttrib(program, index, bufSize, length, size, type, name)
    //override fun getActiveUniform(program: Int, index: Int, bufSize: Int, length: Buffer, size: Buffer, type: Buffer, name: Buffer): Unit = gl.glGetActiveUniform(program, index, bufSize, length, size, type, name)
    //override fun getAttachedShaders(program: Int, maxCount: Int, count: Buffer, shaders: Buffer): Unit = gl.glGetAttachedShaders(program, maxCount, count, shaders)
    override fun getAttribLocation(program: Int, name: String): Int = gl.glGetAttribLocation(program, name)
    override fun getUniformLocation(program: Int, name: String): Int = gl.glGetUniformLocation(program, name)
    override fun getBooleanv(pname: Int, data: Buffer): Unit = tempBufferAddressOut(data, 4) { gl.glGetBooleanv(pname, it) }
    override fun getBufferParameteriv(target: Int, pname: Int, params: Buffer): Unit = tempBufferAddressOut(params, 4) { gl.glGetBufferParameteriv(target, pname, it) }
    override fun getError(): Int = gl.glGetError()
    override fun getFloatv(pname: Int, data: Buffer): Unit = tempBufferAddressOut(data, 1) { gl.glGetFloatv(pname, it) }
    override fun getFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: Buffer): Unit { tempBufferAddressOut(params, 4) { gl.glGetFramebufferAttachmentParameteriv(target, attachment, pname, it) } }
    override fun getIntegerv(pname: Int, data: Buffer): Unit = tempBufferAddressOut(data, 1) { gl.glGetIntegerv(pname, it) }
    override fun getProgramInfoLog(program: Int, bufSize: Int, length: Buffer, infoLog: Buffer): Unit {
        val lengthMem = CreateFFIMemory(8)
        lengthMem.usePointer { lengthPtr ->
            tempBufferAddressOut(infoLog) { infoPtr ->
                gl.glGetProgramInfoLog(program, bufSize, lengthPtr, infoPtr)
            }
            length.set32LE(0, lengthPtr.getS32())
        }
    }
    override fun getRenderbufferParameteriv(target: Int, pname: Int, params: Buffer): Unit = tempBufferAddressOut(params, 4) { gl.glGetRenderbufferParameteriv(target, pname, it) }
    override fun getProgramiv(program: Int, pname: Int, params: Buffer): Unit = tempBufferAddressOut(params, 4) { gl.glGetProgramiv(program, pname, it) }
    override fun getShaderiv(shader: Int, pname: Int, params: Buffer): Unit = tempBufferAddressOut(params, 4) { gl.glGetShaderiv(shader, pname, it) }
    override fun getShaderInfoLog(shader: Int, bufSize: Int, length: Buffer, infoLog: Buffer): Unit {
        val lengthMem = CreateFFIMemory(8)
        lengthMem.usePointer { lengthPtr ->
            tempBufferAddressOut(infoLog) { infoPtr ->
                gl.glGetShaderInfoLog(shader, bufSize, lengthPtr, infoPtr)
            }
            length.set32LE(0, lengthPtr.getS32())
        }
    }
    override fun getShaderPrecisionFormat(shadertype: Int, precisiontype: Int, range: Buffer, precision: Buffer): Unit = Unit
    override fun getShaderSource(shader: Int, bufSize: Int, length: Buffer, source: Buffer): Unit {
        val lengthMem = CreateFFIMemory(8)
        lengthMem.usePointer { lengthPtr ->
            tempBufferAddressOut(source) { sourcePtr ->
                gl.glGetShaderSource(shader, bufSize, lengthPtr, sourcePtr)
            }
            length.set32LE(0, lengthPtr.getS32(0))
        }
    }
    override fun getString(name: Int): String = gl.glGetString(name) ?: ""
    override fun getTexParameterfv(target: Int, pname: Int, params: Buffer): Unit = tempBufferAddressOut(params, 4) { gl.glGetTexParameterfv(target, pname, it) }
    override fun getTexParameteriv(target: Int, pname: Int, params: Buffer): Unit = tempBufferAddressOut(params, 4) { gl.glGetTexParameteriv(target, pname, it) }
    override fun getUniformfv(program: Int, location: Int, params: Buffer): Unit  = tempBufferAddressOut(params, 4) { gl.glGetUniformfv(program, location, it) }
    override fun getUniformiv(program: Int, location: Int, params: Buffer): Unit  = tempBufferAddressOut(params, 4) { gl.glGetUniformiv(program, location, it) }
    override fun getVertexAttribfv(index: Int, pname: Int, params: Buffer): Unit  = tempBufferAddressOut(params, 4) { gl.glGetVertexAttribfv(index, pname, it) }
    override fun getVertexAttribiv(index: Int, pname: Int, params: Buffer): Unit  = tempBufferAddressOut(params, 4) { gl.glGetVertexAttribiv(index, pname, it) }
    override fun getVertexAttribPointerv(index: Int, pname: Int, pointer: Buffer): Unit = tempBufferAddressOut(pointer, 8) { gl.glGetVertexAttribPointerv(index, pname, it) }
    override fun hint(target: Int, mode: Int): Unit = gl.glHint(target, mode)
    override fun isBuffer(buffer: Int): Boolean = gl.glIsBuffer(buffer).toBool()
    override fun isEnabled(cap: Int): Boolean = gl.glIsEnabled(cap).toBool()
    override fun isFramebuffer(framebuffer: Int): Boolean = gl.glIsFramebuffer(framebuffer).toBool()
    override fun isProgram(program: Int): Boolean = gl.glIsProgram(program).toBool()
    override fun isRenderbuffer(renderbuffer: Int): Boolean = gl.glIsRenderbuffer(renderbuffer).toBool()
    override fun isShader(shader: Int): Boolean = gl.glIsShader(shader).toBool()
    override fun isTexture(texture: Int): Boolean = gl.glIsTexture(texture).toBool()
    override fun lineWidth(width: Float): Unit = gl.glLineWidth(width)
    override fun linkProgram(program: Int): Unit = gl.glLinkProgram(program)
    override fun pixelStorei(pname: Int, param: Int): Unit = gl.glPixelStorei(pname, param)
    override fun polygonOffset(factor: Float, units: Float): Unit = gl.glPolygonOffset(factor, units)
    override fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: Buffer): Unit =
        tempBufferAddressIn(pixels) { gl.glReadPixels(x, y, width, height, format, type, it) }
    override fun releaseShaderCompiler(): Unit = Unit
    override fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit = gl.glRenderbufferStorage(target, internalformat, width, height)
    override fun sampleCoverage(value: Float, invert: Boolean): Unit = gl.glSampleCoverage(value, invert.toInt())
    override fun scissor(x: Int, y: Int, width: Int, height: Int): Unit = gl.glScissor(x, y, width, height)
    override fun shaderBinary(count: Int, shaders: Buffer, binaryformat: Int, binary: Buffer, length: Int): Unit = throw KmlGlException("shaderBinary not implemented in Native")
    override fun shaderSource(shader: Int, string: String): Unit {
        CreateFFIMemory(8).usePointer { lengthsPtr ->
            val bytes = "$string\u0000".encodeToByteArray()
            lengthsPtr.set32(bytes.size - 1)
            CreateFFIMemory(bytes).usePointer { strPtr ->
                CreateFFIMemory(8).usePointer { strPtrPtr ->
                    strPtrPtr.setFFIPointer(strPtr)
                    gl.glShaderSource(shader, 1, strPtrPtr, lengthsPtr)
                }
            }
        }
    }
    override fun stencilFunc(func: Int, ref: Int, mask: Int): Unit = gl.glStencilFunc(func, ref, mask)
    override fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit = gl.glStencilFuncSeparate(face, func, ref, mask)
    override fun stencilMask(mask: Int): Unit = gl.glStencilMask(mask)
    override fun stencilMaskSeparate(face: Int, mask: Int): Unit = gl.glStencilMaskSeparate(face, mask)
    override fun stencilOp(fail: Int, zfail: Int, zpass: Int): Unit = gl.glStencilOp(fail, zfail, zpass)
    override fun stencilOpSeparate(face: Int, sfail: Int, dpfail: Int, dppass: Int): Unit = gl.glStencilOpSeparate(face, sfail, dpfail, dppass)
    override fun texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: Buffer?): Unit {
        if (pixels != null) {
            tempBufferAddressIn(pixels) {
                gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, it)
            }
        } else {
            gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, null)
        }
    }
    override fun texImage2D(target: Int, level: Int, internalformat: Int, format: Int, type: Int, data: NativeImage): Unit {
        val buffer = Buffer(data.width * data.height * 4)
        buffer.setArrayLE(0, data.toBMP32IfRequired().ints)
        texImage2D(target, level, internalformat, data.width, data.height, 0, format, type, buffer)
    }
    override fun texParameterf(target: Int, pname: Int, param: Float): Unit = gl.glTexParameterf(target, pname, param)
    override fun texParameterfv(target: Int, pname: Int, params: Buffer): Unit = tempBufferAddressIn(params, 4) { gl.glTexParameterfv(target, pname, it) }
    override fun texParameteri(target: Int, pname: Int, param: Int): Unit = gl.glTexParameteri(target, pname, param)
    override fun texParameteriv(target: Int, pname: Int, params: Buffer): Unit = tempBufferAddressIn(params, 4) { gl.glTexParameteriv(target, pname, it) }
    override fun texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, pixels: Buffer): Unit {
        //gl.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels)
        TODO()
    }
    override fun uniform1f(location: Int, v0: Float): Unit = gl.glUniform1f(location, v0)
    override fun uniform1fv(location: Int, count: Int, value: Buffer): Unit = tempBufferAddressIn(value, 1 * count * 4) { gl.glUniform1fv(location, count, it) }
    override fun uniform1i(location: Int, v0: Int): Unit = gl.glUniform1i(location, v0)
    override fun uniform1iv(location: Int, count: Int, value: Buffer): Unit = tempBufferAddressIn(value, 1 * count * 4) { gl.glUniform1iv(location, count, it) }
    override fun uniform2f(location: Int, v0: Float, v1: Float): Unit = gl.glUniform2f(location, v0, v1)
    override fun uniform2fv(location: Int, count: Int, value: Buffer): Unit = tempBufferAddressIn(value, 2 * count * 4) { gl.glUniform2fv(location, count, it) }
    override fun uniform2i(location: Int, v0: Int, v1: Int): Unit = gl.glUniform2i(location, v0, v1)
    override fun uniform2iv(location: Int, count: Int, value: Buffer): Unit = tempBufferAddressIn(value, 2 * count * 4) { gl.glUniform2iv(location, count, it) }
    override fun uniform3f(location: Int, v0: Float, v1: Float, v2: Float): Unit = gl.glUniform3f(location, v0, v1, v2)
    override fun uniform3fv(location: Int, count: Int, value: Buffer): Unit = tempBufferAddressIn(value, 1 * count * 3) { gl.glUniform3fv(location, count, it) }
    override fun uniform3i(location: Int, v0: Int, v1: Int, v2: Int): Unit = gl.glUniform3i(location, v0, v1, v2)
    override fun uniform3iv(location: Int, count: Int, value: Buffer): Unit = tempBufferAddressIn(value, 3 * count * 4) { gl.glUniform3iv(location, count, it) }
    override fun uniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit = gl.glUniform4f(location, v0, v1, v2, v3)
    override fun uniform4fv(location: Int, count: Int, value: Buffer): Unit = tempBufferAddressIn(value, 4 * count * 4) { gl.glUniform4fv(location, count, it) }
    override fun uniform4i(location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit = gl.glUniform4i(location, v0, v1, v2, v3)
    override fun uniform4iv(location: Int, count: Int, value: Buffer): Unit = tempBufferAddressIn(value, 4 * count * 4) { gl.glUniform4iv(location, count, it) }
    override fun uniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: Buffer): Unit = tempBufferAddressIn(value, 2 * 2 * 4 * count) { gl.glUniformMatrix2fv(location, count, transpose.toInt(), it) }
    override fun uniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: Buffer): Unit = tempBufferAddressIn(value, 3 * 3 * 4 * count) { gl.glUniformMatrix3fv(location, count, transpose.toInt(), it) }
    override fun uniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: Buffer): Unit = tempBufferAddressIn(value, 4 * 4 * 4 * count) { gl.glUniformMatrix4fv(location, count, transpose.toInt(), it) }
    override fun useProgram(program: Int): Unit = gl.glUseProgram(program)
    override fun validateProgram(program: Int): Unit = gl.glValidateProgram(program)
    override fun vertexAttrib1f(index: Int, x: Float): Unit = gl.glVertexAttrib1f(index, x)
    override fun vertexAttrib1fv(index: Int, v: Buffer): Unit = tempBufferAddressIn(v, 1 * 4) { gl.glVertexAttrib1fv(index, it) }
    override fun vertexAttrib2f(index: Int, x: Float, y: Float): Unit = gl.glVertexAttrib2f(index, x, y)
    override fun vertexAttrib2fv(index: Int, v: Buffer): Unit = tempBufferAddressIn(v, 12* 4) { gl.glVertexAttrib2fv(index, it) }
    override fun vertexAttrib3f(index: Int, x: Float, y: Float, z: Float): Unit = gl.glVertexAttrib3f(index, x, y, z)
    override fun vertexAttrib3fv(index: Int, v: Buffer): Unit = tempBufferAddressIn(v, 3 * 4) { gl.glVertexAttrib3fv(index, it) }
    override fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float): Unit = gl.glVertexAttrib4f(index, x, y, z, w)
    override fun vertexAttrib4fv(index: Int, v: Buffer): Unit = tempBufferAddressIn(v, 4 * 4) { gl.glVertexAttrib4fv(index, it) }
    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, pointer: Long): Unit =
        gl.glVertexAttribPointer(index, size, type, normalized.toInt(), stride, pointer.convert())
    override fun viewport(x: Int, y: Int, width: Int, height: Int): Unit = gl.glViewport(x, y, width, height)

    override val isInstancedSupported: Boolean get() = true

    override fun drawArraysInstanced(mode: Int, first: Int, count: Int, instancecount: Int) = gl.glDrawArraysInstanced(mode, first, count, instancecount)
    override fun drawElementsInstanced(mode: Int, count: Int, type: Int, indices: Int, instancecount: Int) = gl.glDrawElementsInstanced(mode, count, type, indices.convert(), instancecount)
    override fun vertexAttribDivisor(index: Int, divisor: Int) = gl.glVertexAttribDivisor(index, divisor)

    override val isUniformBuffersSupported: Boolean get() = true

    override fun bindBufferRange(target: Int, index: Int, buffer: Int, offset: Int, size: Int) = gl.glBindBufferRange(target, index, buffer, offset.convert(), size.convert())
    override fun getUniformBlockIndex(program: Int, name: String): Int = gl.glGetUniformBlockIndex(program, name).toInt()
    override fun uniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int) = gl.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)

    override val isVertexArraysSupported: Boolean get() = true

    override fun genVertexArrays(n: Int, arrays: Buffer) = tempBufferAddressOut(arrays, 4 * n) { gl.glGenVertexArrays(n, it) }
    override fun deleteVertexArrays(n: Int, arrays: Buffer) = tempBufferAddressIn(arrays, 4 * n) { gl.glDeleteBuffers(n, it) }
    override fun bindVertexArray(array: Int) = gl.glBindVertexArray(array)
}
