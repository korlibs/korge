package com.soywiz.korge.bitmapfont

import com.soywiz.korag.log.*
import com.soywiz.korge.*
import com.soywiz.korge.render.*
import com.soywiz.korim.font.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.coroutines.*
import kotlin.test.*

class BitmapFontTest {
	val ag = LogAG()
	val ctx = RenderContext(ag, coroutineContext = EmptyCoroutineContext)

	@Test
	fun simple() = suspendTest {
		val font = resourcesVfs["font/font.fnt"].readBitmapFont()
		assertEquals(81, font.glyphs.size)
		val glyph = font[64]
		assertEquals(69, glyph.texture.width)
		assertEquals(70, glyph.texture.height)
		assertEquals(4, glyph.xoffset)
		assertEquals(4, glyph.yoffset)
		assertEquals(73, glyph.xadvance)

		font.drawText(ctx, 72.0 / 4.0, "ABF,", 0, 0)
		ctx.flush()

		assertEquals(
			"""
            createBuffer(VERTEX):0
            createBuffer(INDEX):1
            createTexture():0
            Texture[0].uploadedBitmap(SyncBitmapSource(rgba=true, width=361, height=512), 361, 512)
            Buffer[0].afterSetMem(mem[393216])
            Buffer[1].afterSetMem(mem[49152])
            draw(vertices=Buffer[0], indices=Buffer[1], program=Program(name=BatchBuilder2D.Premultiplied.Tinted, attributes=[a_Tex, a_Col, a_Col2, a_Pos], uniforms=[u_ProjMat, u_ViewMat, u_Tex]), type=TRIANGLES, vertexLayout=VertexLayout[a_Pos, a_Tex, a_Col, a_Col2], vertexCount=24, offset=0, blending=Blending(srcRGB=SOURCE_ALPHA, dstRGB=ONE_MINUS_SOURCE_ALPHA, srcA=ONE, dstA=ONE_MINUS_SOURCE_ALPHA, eqRGB=ADD, eqA=ADD), uniforms={Uniform(u_ProjMat)=Matrix3D(
              [ 0.015625, 0, 0, -1 ],
              [ 0, -0.015625, 0, 1 ],
              [ 0, 0, -1, 0 ],
              [ 0, 0, 0, 1 ],
            ), Uniform(u_ViewMat)=Matrix3D(
              [ 1, 0, 0, 0 ],
              [ 0, 1, 0, 0 ],
              [ 0, 0, 1, 0 ],
              [ 0, 0, 0, 1 ],
            ), Uniform(u_Tex)=TextureUnit(texture=Texture[0], linear=true)}, stencil=StencilState(enabled=false, triangleFace=FRONT_AND_BACK, compareMode=ALWAYS, actionOnBothPass=KEEP, actionOnDepthFail=KEEP, actionOnDepthPassStencilFail=KEEP, referenceValue=0, readMask=255, writeMask=255), colorMask=ColorMaskState(red=true, green=true, blue=true, alpha=true))
            ::draw.indices=[0, 1, 2, 3, 0, 2, 4, 5, 6, 7, 4, 6, 8, 9, 10, 11, 8, 10, 12, 13, 14, 15, 12, 14]
            ::draw.vertex[0]: a_Pos[vec2(0.0,1.25)], a_Tex[vec2(0.21052632,0.19726562)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[1]: a_Pos[vec2(12.75,1.25)], a_Tex[vec2(0.35180056,0.19726562)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[2]: a_Pos[vec2(12.75,14.75)], a_Tex[vec2(0.35180056,0.30273438)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[3]: a_Pos[vec2(0.0,14.75)], a_Tex[vec2(0.21052632,0.30273438)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[4]: a_Pos[vec2(13.25,1.25)], a_Tex[vec2(0.33240998,0.0859375)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[5]: a_Pos[vec2(23.5,1.25)], a_Tex[vec2(0.44598338,0.0859375)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[6]: a_Pos[vec2(23.5,14.75)], a_Tex[vec2(0.44598338,0.19140625)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[7]: a_Pos[vec2(13.25,14.75)], a_Tex[vec2(0.33240998,0.19140625)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[8]: a_Pos[vec2(25.5,1.25)], a_Tex[vec2(0.35734072,0.1953125)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[9]: a_Pos[vec2(34.75,1.25)], a_Tex[vec2(0.4598338,0.1953125)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[10]: a_Pos[vec2(34.75,14.75)], a_Tex[vec2(0.4598338,0.30078125)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[11]: a_Pos[vec2(25.5,14.75)], a_Tex[vec2(0.35734072,0.30078125)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[12]: a_Pos[vec2(34.5,12.25)], a_Tex[vec2(0.5734072,0.69921875)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[13]: a_Pos[vec2(37.0,12.25)], a_Tex[vec2(0.601108,0.69921875)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[14]: a_Pos[vec2(37.0,17.25)], a_Tex[vec2(0.601108,0.73828125)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[15]: a_Pos[vec2(34.5,17.25)], a_Tex[vec2(0.5734072,0.73828125)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
			""".trimIndent(),
			ag.getLogAsString()
		)
	}

	@Test

	fun font2() = suspendTest {
		val font = resourcesVfs["font2/font1.fnt"].readBitmapFont()
		assertEquals(95, font.glyphs.size)
		val glyph = font[64]
		assertEquals(52, glyph.texture.width)
		assertEquals(52, glyph.texture.height)
		assertEquals(3, glyph.xoffset)
		assertEquals(8, glyph.yoffset)
		assertEquals(51, glyph.xadvance)
	}
}
