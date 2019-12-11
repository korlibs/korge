package com.soywiz.korge3d

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge3d.internal.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*

@Korge3DExperimental
class MeshBuilder3D {
	val layout = VertexLayout(buildList {
		add(Shaders3D.a_pos)
		add(Shaders3D.a_norm)
		add(Shaders3D.a_tex)
	})

	operator fun invoke(callback: MeshBuilder3D.() -> Unit): Mesh3D = this.apply(callback).build()

	companion object {
		operator fun invoke(callback: MeshBuilder3D.() -> Unit): Mesh3D = MeshBuilder3D().apply(callback).build()
	}

	val data = floatArrayListOf()

	fun vertex(pos: Vector3D, normal: Vector3D, texcoords: Vector3D) {
		vertex(pos.x, pos.y, pos.z, normal.x, normal.y, normal.z, texcoords.x, texcoords.y)
	}

	fun vertex(px: Float, py: Float, pz: Float, nx: Float = 0f, ny: Float = 0f, nz: Float = 1f, u: Float = 0f, v: Float = 0f) {
		data.add(px)
		data.add(py)
		data.add(pz)
		data.add(nx)
		data.add(ny)
		data.add(nz)
		data.add(u)
		data.add(v)
	}

	fun build(): Mesh3D = Mesh3D(data.toFBuffer(), layout, null, AG.DrawType.TRIANGLES, true)
}
