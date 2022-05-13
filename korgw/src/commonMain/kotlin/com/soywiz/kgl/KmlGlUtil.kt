package com.soywiz.kgl

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.NativeImage

class KmlGlProgram(val gl: KmlGl, val program: Int, val vertex: Int, val fragment: Int) {
	fun use() = gl.useProgram(program)
	fun unuse() = gl.useProgram(0)
	fun getAttribLocation(name: String) = gl.getAttribLocation(program, name)
	fun getUniformLocation(name: String) = gl.getUniformLocation(program, name)
	fun dispose() {
		gl.deleteProgram(program)
		gl.deleteShader(vertex)
		gl.deleteShader(fragment)
	}

	inline fun use(callback: () -> Unit) {
		val oldProgram = gl.getIntegerv(gl.CURRENT_PROGRAM)
		gl.useProgram(program)
		try {
			callback()
		} finally {
			gl.useProgram(oldProgram)
		}
	}
}

private fun KmlGl.createShader(type: Int, source: String): Int {
	val shader = createShader(type)
	shaderSource(shader, source)
	compileShaderAndCheck(shader)
	return shader
}

// TODO: Release resources on failure
/*
fun KmlGl.createProgram(vertex: String, fragment: String): KmlGlProgram {
    println("************************************************")
	val program = createProgram()
    val hasLayout0 = fragment.contains(GlslGenerator.LAYOUT_LOCATION_0)
    val rfragment = when {
        //hasLayout0 -> fragment.replace(GlslGenerator.LAYOUT_LOCATION_0, "")
        else -> fragment
    }
	val shaderVertex = createShader(VERTEX_SHADER, vertex)
	val shaderFragment = createShader(FRAGMENT_SHADER, rfragment)
	attachShader(program, shaderVertex)
	attachShader(program, shaderFragment)
    if (hasLayout0) {
        bindAttribLocation(program, 0, GlslGenerator.FRAGCOLOR)
    }
    linkProgramAndCheck(program)
	return KmlGlProgram(this, program, shaderVertex, shaderFragment)
}
 */

class KmlGlVertexLayout(val program: KmlGlProgram) {
	data class Element(val index: Int, val size: Int, val type: Int, val pointer: Int, val normalized: Boolean) {
        val pointerLong: Long = pointer.toLong()
    }

	val gl = program.gl
	private var index: Int = 0
	private var size: Int = 0
	private val elements = arrayListOf<Element>()

	//init {
	//    val nattributes = gl.getProgramiv(program.program, gl.ACTIVE_ATTRIBUTES)
	//    println("nattributes: $nattributes")
	//    for (n in 0 until nattributes) {
	//        val namebuf = KmlByteBuffer(1024)
	//        val length = KmlIntBuffer(1)
	//        val size = KmlIntBuffer(1)
	//        val type = KmlIntBuffer(1)
	//        gl.getActiveAttrib(program.program, n, namebuf.size, length, size, type, namebuf)
	//        val name = namebuf.toAsciiString()
	//        println("attribute[$n] = '$name', len=${length[0]}, size=${size[0]}, type=${type[0]}")
	//    }
	//}

	private fun add(name: String, type: Int, esize: Int, count: Int, normalized: Boolean): KmlGlVertexLayout {
		val attribIndex = program.getAttribLocation(name)
		if (attribIndex < 0) throw RuntimeException("Can't find attribute with name '$name' in program $program")
		elements += Element(attribIndex, count, type, size, normalized)
		size += count * esize
		index++
		return this
	}

	fun byte(name: String, count: Int, normalized: Boolean = false) = add(name, gl.BYTE, 1, count, normalized)
	fun ubyte(name: String, count: Int, normalized: Boolean = false) = add(name, gl.UNSIGNED_BYTE, 1, count, normalized)
	fun short(name: String, count: Int, normalized: Boolean = false) = add(name, gl.SHORT, 2, count, normalized)
	fun ushort(name: String, count: Int, normalized: Boolean = false) =
		add(name, gl.UNSIGNED_SHORT, 2, count, normalized)

	fun int(name: String, count: Int, normalized: Boolean = false) = add(name, gl.INT, 4, count, normalized)
	fun float(name: String, count: Int, normalized: Boolean = false) = add(name, gl.FLOAT, 4, count, normalized)

	fun enable(): Unit = gl.run {
		for (index in elements.indices) {
			val element = elements[index]
			enableVertexAttribArray(index)
			vertexAttribPointer(
				element.index,
				element.size,
				element.type,
				element.normalized,
				size,
				element.pointerLong
			)
		}
	}

	fun disable() {
		for (index in elements.indices) {
			gl.disableVertexAttribArray(index)
		}
	}

	inline fun use(callback: () -> Unit) {
		program.use {
			enable()
			try {
				callback()
			} finally {
				disable()
			}
		}
	}
}

fun KmlGlProgram.layout(config: KmlGlVertexLayout.() -> Unit): KmlGlVertexLayout = KmlGlVertexLayout(this).apply(config)

class KmlGlBuffer(val gl: KmlGl, val type: Int, val buf: Int) {
	fun bind() {
		gl.bindBuffer(type, buf)
	}

	fun unbind() {
		gl.bindBuffer(type, 0)
	}

	inline fun bind(callback: () -> Unit) {
		bind()
		try {
			callback()
		} finally {
			unbind()
		}
	}

	fun setData(data: FBuffer, size: Int = data.size): KmlGlBuffer {
		bind()
		gl.bufferData(type, size, data, gl.STATIC_DRAW)
		return this
	}

	fun dispose() {
		fbuffer(4) {
			gl.deleteBuffers(1, it)
		}
	}
}

fun KmlGl.createBuffer(type: Int): KmlGlBuffer {
	val id = fbuffer(4) {
		genBuffers(1, it)
		it.getInt(0)
	}
	return KmlGlBuffer(this, type, id)
}

fun KmlGl.createArrayBuffer(): KmlGlBuffer = createBuffer(ARRAY_BUFFER)
fun KmlGl.createElementArrayBuffer(): KmlGlBuffer = createBuffer(ELEMENT_ARRAY_BUFFER)

inline fun KmlGlVertexLayout.drawArrays(
	vertices: KmlGlBuffer,
	mode: Int,
	first: Int,
	count: Int,
	uniforms: KmlGl.() -> Unit = {}
) {
	this.use {
		vertices.bind {
			uniforms(gl)
			gl.drawArrays(mode, first, count)
		}
	}
}

inline fun KmlGlVertexLayout.drawElements(
	vertices: KmlGlBuffer,
	indices: KmlGlBuffer,
	mode: Int,
	count: Int,
	type: Int = gl.UNSIGNED_SHORT,
	offset: Int = 0,
	uniforms: KmlGl.() -> Unit = {}
) {
	this.use {
		vertices.bind {
			indices.bind {
				uniforms(gl)
				gl.drawElements(mode, count, type, offset)
			}
		}
	}
}

class KmlGlTex(val gl: KmlGl, val tex: Int) {
	var width = 0
	var height = 0

	var smooth: Boolean = true
	var clampToEdge: Boolean = true

	fun bind(unit: Int) = gl.run {
		activeTexture(TEXTURE0 + unit)
		bindTexture(TEXTURE_2D, tex)
		texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, if (smooth) gl.LINEAR else gl.NEAREST)
		texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, if (smooth) gl.LINEAR else gl.NEAREST)
		texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, if (clampToEdge) gl.CLAMP_TO_EDGE else gl.REPEAT)
		texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, if (clampToEdge) gl.CLAMP_TO_EDGE else gl.REPEAT)
	}

	fun upload(
		width: Int,
		height: Int,
		data: FBuffer,
		format: Int = gl.RGBA,
		type: Int = gl.UNSIGNED_BYTE
	): KmlGlTex {
		bind(0)
		gl.texImage2D(gl.TEXTURE_2D, 0, format, width, height, 0, format, type, data)
		this.width = width
		this.height = height
		return this
	}

	fun upload(width: Int, height: Int, data: IntArray, format: Int = gl.RGBA, type: Int = gl.UNSIGNED_BYTE): KmlGlTex {
		return data.toTempBuffer { upload(width, height, it, format, type) }
	}

	fun upload(data: NativeImage, format: Int = gl.RGBA, type: Int = gl.UNSIGNED_BYTE): KmlGlTex {
		bind(0)
		gl.texImage2D(gl.TEXTURE_2D, 0, format, format, type, data)
		this.width = data.width
		this.height = data.height
		return this
	}

	fun dispose() {
		fbuffer(1) {
			it.setInt(0, tex)
			gl.deleteTextures(1, it)
		}
	}
}

fun KmlGl.createKmlTexture(): KmlGlTex {
	val buf = fbuffer(4) {
		genTextures(1, it)
		it.getInt(0)
	}
	return KmlGlTex(this, buf).upload(1, 1, FBuffer(4))
}

fun KmlGl.uniformTex(location: Int, tex: KmlGlTex, unit: Int) {
	tex.bind(unit)
	uniform1i(location, unit)
}

object KmlGlUtil {
	fun ortho(
		width: Int,
		height: Int,
		near: Float = 0f,
		far: Float = 1f,
		out: Float32Buffer = Float32BufferAlloc(4 * 16)
	): MemBuffer {
		return ortho(height.toFloat(), 0f, 0f, width.toFloat(), near, far, out)
	}

	fun ortho(
		bottom: Float, top: Float, left: Float, right: Float,
		near: Float, far: Float,
		M: Float32Buffer = Float32BufferAlloc(16)
	): MemBuffer {
		// set OpenGL perspective projection matrix
		M[0] = 2 / (right - left)
		M[1] = 0f
		M[2] = 0f
		M[3] = 0f

		M[4] = 0f
		M[5] = 2 / (top - bottom)
		M[6] = 0f
		M[7] = 0f

		M[8] = 0f
		M[9] = 0f
		M[10] = -2 / (far - near)
		M[11] = 0f

		M[12] = -(right + left) / (right - left)
		M[13] = -(top + bottom) / (top - bottom)
		M[14] = -(far + near) / (far - near)
		M[15] = 1f
		return M.mem
	}
}

