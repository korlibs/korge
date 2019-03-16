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
			writeU_VL(names[key])
			writeU_VL(geom.mesh.data.size * 4)
			writeU_VL(geom.mesh.hasTexture.toInt())
			writeU_VL(geom.mesh.maxWeights)
			writeU_VL(geom.mesh.vertexCount)
			writeFloatArrayLE(geom.mesh.data) // @TODO: Improve performance of this. Using FBuffer?

		}
	})
}

@Korge3DExperimental
suspend fun VfsFile.readKs3d(): Library3D {
	TODO()
}
