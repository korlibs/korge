package com.soywiz.korge.view

import com.soywiz.korge.ViewsForTesting
import com.soywiz.korge.render.Texture
import com.soywiz.korio.async.EventLoopTest
import com.soywiz.korio.async.sync
import com.soywiz.korio.async.syncTest
import com.soywiz.korma.geom.Rectangle
import org.junit.Assert
import org.junit.Test

class ViewsTest : ViewsForTesting() {
    val tex = Texture(views.ag.createTexture(), 10, 10)

    @Test
    fun name() = syncTest {
        views.stage += views.container().apply {
            this += views.image(tex)
        }
        Assert.assertEquals(
                """
				|Stage(0)
				| Container(1)
				|  Image(2)
			""".trimMargin(),
                views.stage.dumpToString()
        )
        views.render()
        Assert.assertEquals(
                """
				createTexture():0
				createBuffer(VERTEX):0
				Buffer[0].afterSetMem(mem[96000])
				createBuffer(INDEX):1
				Buffer[1].afterSetMem(mem[12000])
				draw(vertices=Buffer[0], indices=Buffer[1], program=Program(name=BatchBuilder2D.Tinted, attributes=[a_Tex, a_Col, a_Col2, a_Pos], uniforms=[u_ProjMat, u_Tex]), type=TRIANGLES, vertexLayout=VertexLayout[a_Pos, a_Tex, a_Col, a_Col2], vertexCount=6, offset=0, blending=Blending(srcRGB=SOURCE_ALPHA, dstRGB=ONE_MINUS_SOURCE_ALPHA, srcA=ONE, dstA=ONE_MINUS_SOURCE_ALPHA, eqRGB=ADD, eqA=ADD), uniforms={Uniform(u_ProjMat)=Matrix4([0.003125, 0.0, 0.0, 0.0, 0.0, -0.004166667, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, -1.0, 1.0, -0.0, 1.0]), Uniform(u_Tex)=TextureUnit(texture=Texture[0], linear=true)}, stencil=StencilState(enabled=false, triangleFace=FRONT_AND_BACK, compareMode=ALWAYS, actionOnBothPass=KEEP, actionOnDepthFail=KEEP, actionOnDepthPassStencilFail=KEEP, referenceValue=0, readMask=255, writeMask=255), colorMask=ColorMaskState(red=true, green=true, blue=true, alpha=true))
				::draw.indices=[0, 1, 2, 3, 0, 2]
				::draw.vertex[0]: a_Pos[vec2(0.0,0.0)], a_Tex[vec2(0.0,0.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
				::draw.vertex[1]: a_Pos[vec2(10.0,0.0)], a_Tex[vec2(1.0,0.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
				::draw.vertex[2]: a_Pos[vec2(10.0,10.0)], a_Tex[vec2(1.0,1.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
				::draw.vertex[3]: a_Pos[vec2(0.0,10.0)], a_Tex[vec2(0.0,1.0)], a_Col[byte4(-1)], a_Col2[byte4(2139062143)]
				Buffer[1].close()
				Buffer[0].close()
			""".trimIndent(),
                ag.getLogAsString()
        )
    }

	@Test
	fun testBounds() = syncTest {
		val image = views.image(tex)
		image.x = 100.0
		image.y = 100.0
		views.stage += image
		Assert.assertEquals(Rectangle(100, 100, 10, 10), image.getGlobalBounds())
	}

	@Test
	fun removeFromParent() = viewsTest {
		val s1 = views.container().apply { name = "s1" }
		val s2 = views.container().apply { name = "s2" }
		val s3 = views.container().apply { name = "s3" }
		views.stage += s1
		s1 += s2
		s1 += s3
		Assert.assertNotNull(s1["s2"])
		Assert.assertNotNull(s1["s3"])

		s1 -= s3
		Assert.assertNotNull(s1["s2"])
		Assert.assertNull(s1["s3"])
	}
}
