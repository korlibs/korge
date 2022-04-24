package com.soywiz.kgl

import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.Rectangle
import com.soywiz.krypto.encoding.*
import kotlin.native.concurrent.ThreadLocal

class KmlGlException(message: String) : RuntimeException(message)

@ThreadLocal
private val tempFBuffer = FBuffer(4 * 4)

fun KmlGl.getShaderiv(shader: Int, type: Int): Int =
    tempFBuffer.let { getShaderiv(shader, type, it); it.getInt(0) }

fun KmlGl.getProgramiv(program: Int, type: Int): Int =
    tempFBuffer.let { getProgramiv(program, type, it); it.getInt(0) }

fun KmlGl.getBooleanv(pname: Int): Boolean = tempFBuffer.let { getBooleanv(pname, it); it[0] != 0 }
fun KmlGl.getFloatv(pname: Int): Float = tempFBuffer.let { getFloatv(pname, it); it.getFloat(0) }
fun KmlGl.getIntegerv(pname: Int): Int = tempFBuffer.let { getIntegerv(pname, it); it.getInt(0) }
fun KmlGl.getVertexAttribiv(index: Int, pname: Int): Int = tempFBuffer.let { getVertexAttribiv(index, pname, it); it.getInt(0) }
fun KmlGl.getRectanglev(pname: Int, out: Rectangle = Rectangle()): Rectangle = tempFBuffer.let {
    getFloatv(pname, it)
    out.setTo(it.getFloat(0), it.getFloat(1), it.getFloat(2), it.getFloat(3))
}

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
