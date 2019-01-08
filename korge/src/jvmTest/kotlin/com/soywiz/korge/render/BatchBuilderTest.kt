package com.soywiz.korge.render

import com.soywiz.korag.log.*
import kotlin.test.*

class BatchBuilderTest {
	val ag = LogAG(16, 16)
	val bb = BatchBuilder2D(ag)

	@Test
	fun simpleBatch() {
		val tex = Texture(ag.createTexture(), 100, 100)
		bb.drawQuad(tex, 0f, 0f)
		bb.flush()

		assertEquals(
			"""
			createBuffer(VERTEX):0
			createBuffer(INDEX):1
			createTexture():0
			Buffer[0].afterSetMem(mem[393216])
			Buffer[1].afterSetMem(mem[49152])
			draw(vertices=Buffer[0], indices=Buffer[1], program=Program(name=BatchBuilder2D.Premultiplied.Tinted, attributes=[a_Tex, a_Col, a_Col2, a_Pos], uniforms=[u_ProjMat, u_ViewMat, u_Tex]), type=TRIANGLES, vertexLayout=VertexLayout[a_Pos, a_Tex, a_Col, a_Col2], vertexCount=6, offset=0, blending=Blending(srcRGB=SOURCE_ALPHA, dstRGB=ONE_MINUS_SOURCE_ALPHA, srcA=ONE, dstA=ONE_MINUS_SOURCE_ALPHA, eqRGB=ADD, eqA=ADD), uniforms={Uniform(u_ProjMat)=Matrix3D([0.125, 0.0, 0.0, 0.0, 0.0, -0.125, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, -1.0, 1.0, -0.0, 1.0]), Uniform(u_ViewMat)=Matrix3D([1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0]), Uniform(u_Tex)=TextureUnit(texture=Texture[0], linear=true)}, stencil=StencilState(enabled=false, triangleFace=FRONT_AND_BACK, compareMode=ALWAYS, actionOnBothPass=KEEP, actionOnDepthFail=KEEP, actionOnDepthPassStencilFail=KEEP, referenceValue=0, readMask=255, writeMask=255), colorMask=ColorMaskState(red=true, green=true, blue=true, alpha=true))
			::draw.indices=[0, 1, 2, 3, 0, 2]
			::draw.vertex[0]: a_Pos[vec2(0.0,0.0)], a_Tex[vec2(0.0,0.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
			::draw.vertex[1]: a_Pos[vec2(100.0,0.0)], a_Tex[vec2(1.0,0.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
			::draw.vertex[2]: a_Pos[vec2(100.0,100.0)], a_Tex[vec2(1.0,1.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
			::draw.vertex[3]: a_Pos[vec2(0.0,100.0)], a_Tex[vec2(0.0,1.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
			""".trimIndent(),
			ag.getLogAsString()
		)
	}

	@Test
	fun batch2() {
		val tex = Texture(ag.createTexture(), 100, 100)
		bb.drawQuad(tex, 0f, 0f)
		bb.drawQuad(tex, 100f, 0f)
		bb.flush()

		assertEquals(
			"""
			createBuffer(VERTEX):0
			createBuffer(INDEX):1
			createTexture():0
			Buffer[0].afterSetMem(mem[393216])
			Buffer[1].afterSetMem(mem[49152])
			draw(vertices=Buffer[0], indices=Buffer[1], program=Program(name=BatchBuilder2D.Premultiplied.Tinted, attributes=[a_Tex, a_Col, a_Col2, a_Pos], uniforms=[u_ProjMat, u_ViewMat, u_Tex]), type=TRIANGLES, vertexLayout=VertexLayout[a_Pos, a_Tex, a_Col, a_Col2], vertexCount=12, offset=0, blending=Blending(srcRGB=SOURCE_ALPHA, dstRGB=ONE_MINUS_SOURCE_ALPHA, srcA=ONE, dstA=ONE_MINUS_SOURCE_ALPHA, eqRGB=ADD, eqA=ADD), uniforms={Uniform(u_ProjMat)=Matrix3D([0.125, 0.0, 0.0, 0.0, 0.0, -0.125, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, -1.0, 1.0, -0.0, 1.0]), Uniform(u_ViewMat)=Matrix3D([1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0]), Uniform(u_Tex)=TextureUnit(texture=Texture[0], linear=true)}, stencil=StencilState(enabled=false, triangleFace=FRONT_AND_BACK, compareMode=ALWAYS, actionOnBothPass=KEEP, actionOnDepthFail=KEEP, actionOnDepthPassStencilFail=KEEP, referenceValue=0, readMask=255, writeMask=255), colorMask=ColorMaskState(red=true, green=true, blue=true, alpha=true))
			::draw.indices=[0, 1, 2, 3, 0, 2, 4, 5, 6, 7, 4, 6]
			::draw.vertex[0]: a_Pos[vec2(0.0,0.0)], a_Tex[vec2(0.0,0.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
			::draw.vertex[1]: a_Pos[vec2(100.0,0.0)], a_Tex[vec2(1.0,0.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
			::draw.vertex[2]: a_Pos[vec2(100.0,100.0)], a_Tex[vec2(1.0,1.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
			::draw.vertex[3]: a_Pos[vec2(0.0,100.0)], a_Tex[vec2(0.0,1.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
			::draw.vertex[4]: a_Pos[vec2(100.0,0.0)], a_Tex[vec2(0.0,0.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
			::draw.vertex[5]: a_Pos[vec2(200.0,0.0)], a_Tex[vec2(1.0,0.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
			::draw.vertex[6]: a_Pos[vec2(200.0,100.0)], a_Tex[vec2(1.0,1.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
			::draw.vertex[7]: a_Pos[vec2(100.0,100.0)], a_Tex[vec2(0.0,1.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
			""".trimIndent(),
			ag.getLogAsString()
		)
	}
}
