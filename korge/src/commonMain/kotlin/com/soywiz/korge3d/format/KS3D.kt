package com.soywiz.korge3d.format

import com.soywiz.kds.IndexedTable
import com.soywiz.kds.fastForEach
import com.soywiz.kmem.toInt
import com.soywiz.korge3d.Korge3DExperimental
import com.soywiz.korge3d.Library3D
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.stream.MemorySyncStreamToByteArray
import com.soywiz.korio.stream.write32LE
import com.soywiz.korio.stream.writeBytes
import com.soywiz.korio.stream.writeString
import com.soywiz.korio.stream.writeStringVL
import com.soywiz.korio.stream.writeU_VL

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
        //TODO: handle mesh.indexArray
        library.geometryDefs.fastForEach { key, geom ->
            val mesh = geom.mesh
            writeU_VL(names[key])
            writeU_VL(mesh.hasTexture.toInt())
            writeU_VL(mesh.maxWeights)
            writeU_VL(mesh.vertexCount)
            mesh.vertexBuffers.fastForEach {
                writeU_VL(it.buffer.size)
                val temp = ByteArray(it.buffer.size)
                it.buffer.getAlignedArrayInt8(0, temp, 0, it.buffer.size)
                writeBytes(temp)
            }
        }
    })
}

@Korge3DExperimental
suspend fun VfsFile.readKs3d(): Library3D {
    TODO()
}
