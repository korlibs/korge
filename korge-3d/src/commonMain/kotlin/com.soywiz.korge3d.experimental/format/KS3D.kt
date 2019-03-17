package com.soywiz.korge3d.experimental.format

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korge3d.experimental.*
import com.soywiz.korio.file.*
import com.soywiz.korio.stream.*

// KorGE Scene 3D file format
@Korge3DExperimental
object KS3D {
}

@Korge3DExperimental
suspend fun VfsFile.writeKs3d(library: Library3D) {
	val names = IndexedTable<String>()

	library.geometryDefs.fastForEach { key, value ->
		names.add(key)
	}

	writeBytes(MemorySyncStreamToByteArray {
		writeString("KS3D")
		write32LE(0) // Version
		write32LE(names.size)
		for (name in names.instances) writeStringVL(name)
		library.geometryDefs.fastForEach { key, geom ->
			val mesh = geom.mesh
			writeU_VL(names[key])
			writeU_VL(mesh.fbuffer.size)
			writeU_VL(mesh.hasTexture.toInt())
			writeU_VL(mesh.maxWeights)
			writeU_VL(mesh.vertexCount)
			mesh.fbuffer.getAlignedArrayInt8(0, ByteArray(mesh.fbuffer.size), 0, mesh.fbuffer.size)
		}
	})
}

@Korge3DExperimental
suspend fun VfsFile.readKs3d(): Library3D {
	TODO()
}
