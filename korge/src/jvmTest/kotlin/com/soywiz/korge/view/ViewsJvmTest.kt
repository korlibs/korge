package com.soywiz.korge.view

import com.soywiz.korge.scene.*
import com.soywiz.korge.tests.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class ViewsJvmTest : ViewsForTesting(log = true) {
	val tex = Bitmap32(10, 10)

	@Test
	fun name() {
		views.stage += Container().apply {
			this += Image(tex)
		}
		assertEquals(
			"""
				Stage
				 Container
				  Image:bitmap=BitmapSlice(null:SizeInt(width=10, height=10))
			""".trimIndent(),
			views.stage.dumpToString()
		)
		views.render()
		assertEquals(
			"""
            clear(#000000ff, 1.0, 0, true, true, true)
            createBuffer(VERTEX):0
            createBuffer(INDEX):1
            createTexture():0
            Texture[0].uploadedBitmap(SyncBitmapSource(rgba=true, width=10, height=10), 10, 10)
            Buffer[0].afterSetMem(mem[393216])
            Buffer[1].afterSetMem(mem[49152])
            draw(vertices=Buffer[0], indices=Buffer[1], program=Program(name=BatchBuilder2D.NoPremultiplied.Tinted, attributes=[a_Tex, a_Col, a_Col2, a_Pos], uniforms=[u_ProjMat, u_ViewMat, u_Tex]), type=TRIANGLES, vertexLayout=VertexLayout[a_Pos, a_Tex, a_Col, a_Col2], vertexCount=6, offset=0, blending=Blending(srcRGB=SOURCE_ALPHA, dstRGB=ONE_MINUS_SOURCE_ALPHA, srcA=ONE, dstA=ONE_MINUS_SOURCE_ALPHA, eqRGB=ADD, eqA=ADD), uniforms={Uniform(u_ProjMat)=Matrix3D(
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
            ::draw.indices=[0, 1, 2, 3, 0, 2]
            ::draw.vertex[0]: a_Pos[vec2(0.0,0.0)], a_Tex[vec2(0.0,0.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[1]: a_Pos[vec2(10.0,0.0)], a_Tex[vec2(1.0,0.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[2]: a_Pos[vec2(10.0,10.0)], a_Tex[vec2(1.0,1.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            ::draw.vertex[3]: a_Pos[vec2(0.0,10.0)], a_Tex[vec2(0.0,1.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
            disposeTemporalPerFrameStuff()
            flipInternal()
			""".trimIndent(),
            (logAg)?.getLogAsString()
		)
	}

	@Test
	fun textGetBounds1() = viewsTest {
		val font = debugBmpFont
		assertEquals(Rectangle(0, 0, 77, 8), TextOld("Hello World", font = font, textSize = 8.0).globalBounds)
	}

    @Test
    fun textGetBounds2() = viewsTest {
        val font = debugBmpFont
        assertEquals(Rectangle(0, 0, 154, 16), TextOld("Hello World", font = font, textSize = 16.0).globalBounds)
    }
}
