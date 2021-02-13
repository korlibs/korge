package com.soywiz.kgl

import com.soywiz.klogger.*
import com.soywiz.kmem.*

class KmlGlException(message: String) : RuntimeException(message)

fun KmlGl.getShaderiv(shader: Int, type: Int): Int =
	fbuffer(4) { getShaderiv(shader, type, it); it.getInt(0) }

fun KmlGl.getProgramiv(program: Int, type: Int): Int =
	fbuffer(4) { getProgramiv(program, type, it); it.getInt(0) }

fun KmlGl.getBooleanv(pname: Int): Boolean = fbuffer(4) { getBooleanv(pname, it); it[0] != 0 }
fun KmlGl.getFloatv(pname: Int): Float = fbuffer(4) { getFloatv(pname, it); it.getFloat(0) }
fun KmlGl.getIntegerv(pname: Int): Int = fbuffer(4) { getIntegerv(pname, it); it.getInt(0) }
fun KmlGl.getVertexAttribiv(index: Int, pname: Int): Int = fbuffer(4) { getVertexAttribiv(index, pname, it); it.getInt(0) }


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
        Console.error("glGetError after $message")
    }
}
