package com.soywiz.kgl

import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.Rectangle
import com.soywiz.krypto.encoding.*
import kotlin.native.concurrent.ThreadLocal

class KmlGlException(message: String) : RuntimeException(message)

@ThreadLocal
private val tempFBufferByte = FBuffer(1)
@ThreadLocal
private val tempFBuffer1 = FBuffer(4)
@ThreadLocal
private val tempFBuffer4 = FBuffer(4 * 4)

private inline fun tempByte1Buffer(value: Int = 0, block: (FBuffer) -> Unit): Int = tempFBufferByte.let {
    it.setByte(0, value.toByte())
    block(it)
    it.getByte(0).toInt() and 0xFF
}
private inline fun tempInt1Buffer(value: Int = 0, block: (FBuffer) -> Unit): Int = tempFBuffer1.let {
    it.setInt(0, value)
    block(it)
    it.getInt(0)
}
private inline fun tempFloat1Buffer(value: Float = 0f, block: (FBuffer) -> Unit): Float = tempFBuffer1.let {
    it.setFloat(0, value)
    block(it)
    it.getFloat(0)
}

fun KmlGl.getShaderiv(shader: Int, type: Int): Int = tempInt1Buffer { getShaderiv(shader, type, it) }
fun KmlGl.getProgramiv(program: Int, type: Int): Int = tempInt1Buffer { getProgramiv(program, type, it) }

fun KmlGl.getBooleanv(pname: Int): Boolean = tempByte1Buffer { getBooleanv(pname, it) } != 0
fun KmlGl.getFloatv(pname: Int): Float = tempFloat1Buffer { getFloatv(pname, it) }
fun KmlGl.getIntegerv(pname: Int): Int = tempInt1Buffer { getIntegerv(pname, it) }
fun KmlGl.getVertexAttribiv(index: Int, pname: Int): Int = tempInt1Buffer { getVertexAttribiv(index, pname, it) }
fun KmlGl.getRectanglev(pname: Int, out: Rectangle = Rectangle()): Rectangle = tempFBuffer4.let {
    it.setFloat(0, 0f)
    it.setFloat(1, 0f)
    it.setFloat(2, 0f)
    it.setFloat(3, 0f)
    getFloatv(pname, it)
    out.setTo(it.getFloat(0), it.getFloat(1), it.getFloat(2), it.getFloat(3))
}

fun KmlGl.genBuffer(): Int = tempInt1Buffer { genBuffers(1, it) }
fun KmlGl.genTexture(): Int = tempInt1Buffer { genTextures(1, it) }
fun KmlGl.genRenderbuffer(): Int = tempInt1Buffer { genRenderbuffers(1, it) }
fun KmlGl.genFramebuffer(): Int = tempInt1Buffer { genFramebuffers(1, it) }

fun KmlGl.deleteBuffer(id: Int): Unit { tempInt1Buffer(id) { deleteBuffers(1, it) } }
fun KmlGl.deleteTexture(id: Int): Unit { tempInt1Buffer(id) { deleteTextures(1, it) } }
fun KmlGl.deleteRenderbuffer(id: Int): Unit { tempInt1Buffer(id) { deleteRenderbuffers(1, it) } }
fun KmlGl.deleteFramebuffer(id: Int): Unit { tempInt1Buffer(id) { deleteFramebuffers(1, it) } }

private inline fun KmlGl.getInfoLog(
	obj: Int,
	getiv: (Int, Int) -> Int,
	getInfoLog: (Int, Int, FBuffer, FBuffer) -> Unit
): String {
	val size = getiv(obj, INFO_LOG_LENGTH)
	return fbuffer(4 * 1) { sizev ->
		fbuffer(size) { mbuffer ->
			getInfoLog(obj, size, sizev, mbuffer)
			mbuffer.toAsciiString()
		}
	}
}

fun KmlGl.getShaderInfoLog(shader: Int): String = getInfoLog(shader, ::getShaderiv, ::getShaderInfoLog)
fun KmlGl.getProgramInfoLog(shader: Int): String = getInfoLog(shader, ::getProgramiv, ::getProgramInfoLog)

fun KmlGl.compileShaderAndCheck(shader: Int) {
	compileShader(shader)
	if (getShaderiv(shader, COMPILE_STATUS) != GTRUE) {
		throw KmlGlException(getShaderInfoLog(shader))
	}
}

fun KmlGl.linkProgramAndCheck(program: Int) {
	linkProgram(program)
	if (getProgramiv(program, LINK_STATUS) != GTRUE) {
		throw KmlGlException(getProgramInfoLog(program))
	}
}

fun KmlGl.getErrorString(error: Int = getError()): String {
	return when (error) {
		NO_ERROR -> "NO_ERROR"
		INVALID_ENUM -> "INVALID_ENUM"
		INVALID_VALUE -> "INVALID_VALUE"
		INVALID_OPERATION -> "INVALID_OPERATION"
		OUT_OF_MEMORY -> "OUT_OF_MEMORY"
		else -> "UNKNOWN_ERROR$error"
	}
}

fun KmlGl.checkError(message: String) {
    val error = getError()
    if (error != NO_ERROR) {
        Console.error("glGetError after $message with error $error (${error.hex})")
    }
}
