package com.soywiz.korag

import com.soywiz.kgl.*
import com.soywiz.korag.shader.*
import kotlin.test.*

class NAGOpenglTest {
    @Test
    fun test() {
        val gl = KmlGlProxyLogToString()
        val ag = NAGOpengl(gl)
        val fb = NAGFrameBuffer().set(100, 100)
        ag.draw(NAGBatch(
            vertexData = NAGVertices(
                NAGVerticesPart(VertexLayout(), NAGBuffer().upload()),
            ),
            indexData = NAGBuffer().upload(),
            batches = listOf(
                NAGUniformBatch(
                    fb,
                    DefaultShaders.PROGRAM_DEBUG,
                    AGUniformValues(),
                    AGFullState(),
                    NAGDrawCommandArray {
                        it.add(AGDrawType.TRIANGLES, AGIndexType.NONE, 0, 100)
                    }
                )
            ),
        ))
        fb.delete()
        ag.finish()
        println(gl.log.joinToString("\n"))
    }
}
