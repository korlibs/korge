package com.soywiz.korge.render

import com.soywiz.korag.log.LogAG
import org.junit.Assert
import org.junit.Test

class BatchBuilderTest {
	val ag = LogAG()
	val bb = BatchBuilder2D(ag)

	@Test
	fun name() {
		val tex = Texture(ag.createTexture(), 100, 100)
		bb.addQuad(tex, 0f, 0f)
		bb.flush()

		Assert.assertEquals(
			"""
				createTexture():0
				createBuffer(VERTEX):0
				Buffer[0].afterSetMem(mem[64])
				createBuffer(INDEX):1
				Buffer[1].afterSetMem(mem[12])
				draw(vertices=Buffer[0], indices=Buffer[1], program=Program[BatchBuilder2D], type=TRIANGLES, vertexLayout=VertexLayout[a_Pos, a_Tex], vertexCount=6, offset=0, blending=OVERLAY, uniforms={})
				::draw.indices=[0, 1, 2, 3, 1, 2]
				::draw.vertex[0]: a_Pos[vec2(0.0,0.0)], a_Tex[vec2(0.0,0.0)]
				::draw.vertex[1]: a_Pos[vec2(100.0,0.0)], a_Tex[vec2(1.0,0.0)]
				::draw.vertex[1]: a_Pos[vec2(100.0,0.0)], a_Tex[vec2(1.0,0.0)]
				::draw.vertex[2]: a_Pos[vec2(100.0,100.0)], a_Tex[vec2(1.0,1.0)]
				::draw.vertex[2]: a_Pos[vec2(100.0,100.0)], a_Tex[vec2(1.0,1.0)]
				::draw.vertex[3]: a_Pos[vec2(0.0,100.0)], a_Tex[vec2(0.0,1.0)]
				Buffer[1].close()
				Buffer[0].close()
			""".trimIndent(),
			ag.getLogAsString()
		)

	}
}