package korlibs.kgl

import korlibs.logger.Logger
import korlibs.memory.*
import korlibs.crypto.encoding.hex
import korlibs.math.geom.*
import kotlin.native.concurrent.ThreadLocal

class KmlGlException(message: String) : RuntimeException(message)

private val logger = Logger("KmlGlException")
@ThreadLocal
private val tempNBufferByte = Buffer(1, direct = true)
@ThreadLocal
private val tempNBuffer1 = Buffer(4, direct = true)
@ThreadLocal
private val tempNBuffer4 = Buffer(4 * 4, direct = true)

private inline fun tempByte1Buffer(value: Int = 0, block: (Buffer) -> Unit): Int = tempNBufferByte.let {
    it.setInt8(0, value.toByte())
    block(it)
    it.getUInt8(0)
}
private inline fun tempInt1Buffer(value: Int = 0, block: (Buffer) -> Unit): Int = tempNBuffer1.let {
    it.setInt32(0, value)
    block(it)
    it.getInt32(0)
}
private inline fun tempFloat1Buffer(value: Float = 0f, block: (Buffer) -> Unit): Float = tempNBuffer1.let {
    it.setFloat32(0, value)
    block(it)
    it.getFloat32(0)
}

fun KmlGl.getShaderiv(shader: Int, type: Int): Int = tempInt1Buffer { getShaderiv(shader, type, it) }
fun KmlGl.getProgramiv(program: Int, type: Int): Int = tempInt1Buffer { getProgramiv(program, type, it) }

fun KmlGl.getBooleanv(pname: Int): Boolean = tempByte1Buffer { getBooleanv(pname, it) } != 0
fun KmlGl.getFloatv(pname: Int): Float = tempFloat1Buffer { getFloatv(pname, it) }
fun KmlGl.getIntegerv(pname: Int): Int = tempInt1Buffer { getIntegerv(pname, it) }
fun KmlGl.getVertexAttribiv(index: Int, pname: Int): Int = tempInt1Buffer { getVertexAttribiv(index, pname, it) }
fun KmlGl.getRectanglev(pname: Int): Rectangle = tempNBuffer4.let {
    it.setFloat32(0, 0f)
    it.setFloat32(1, 0f)
    it.setFloat32(2, 0f)
    it.setFloat32(3, 0f)
    getFloatv(pname, it)
    Rectangle(it.getFloat32(0), it.getFloat32(1), it.getFloat32(2), it.getFloat32(3))
}

fun KmlGl.genBuffer(): Int = tempInt1Buffer { genBuffers(1, it) }
fun KmlGl.genTexture(): Int = tempInt1Buffer { genTextures(1, it) }
fun KmlGl.genRenderbuffer(): Int = tempInt1Buffer { genRenderbuffers(1, it) }
fun KmlGl.genFramebuffer(): Int = tempInt1Buffer { genFramebuffers(1, it) }

fun KmlGl.deleteBuffer(id: Int) { tempInt1Buffer(id) { deleteBuffers(1, it) } }
fun KmlGl.deleteTexture(id: Int) { tempInt1Buffer(id) { deleteTextures(1, it) } }
fun KmlGl.deleteRenderbuffer(id: Int) { tempInt1Buffer(id) { deleteRenderbuffers(1, it) } }
fun KmlGl.deleteFramebuffer(id: Int) { tempInt1Buffer(id) { deleteFramebuffers(1, it) } }

private inline fun KmlGl.getInfoLog(
	obj: Int,
	getiv: (Int, Int) -> Int,
	getInfoLog: (Int, Int, Buffer, Buffer) -> Unit
): String {
	val size = getiv(obj, INFO_LOG_LENGTH)
	return BufferTemp(4 * 1) { sizev ->
		BufferTemp(size) { mbuffer ->
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
        logger.error { "glGetError after $message with error $error (${error.hex})" }
    }
}
