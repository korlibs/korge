package com.soywiz.korge.view

import com.soywiz.korag.log.LogAG
import com.soywiz.korge.input.Input
import com.soywiz.korge.render.Texture
import com.soywiz.korio.async.EventLoopTest
import com.soywiz.korio.async.sync
import com.soywiz.korio.inject.AsyncInjector
import org.junit.Assert
import org.junit.Test

class ViewsTest {
    val ag = LogAG()
    val views = Views(ag, AsyncInjector(), Input())
    val tex = Texture(views.ag.createTexture(), 10, 10)

    @Test
    fun name() = sync(EventLoopTest()) {
        views.root += views.container().apply {
            this += views.image(tex)
        }
        Assert.assertEquals(
                """
				|Container(0)
				| Container(1)
				|  Image(2)
			""".trimMargin(),
                views.root.dumpToString()
        )
        views.render()
        Assert.assertEquals(
                """
				createTexture():0
				createBuffer(VERTEX):0
				Buffer[0].afterSetMem(mem[64])
				createBuffer(INDEX):1
				Buffer[1].afterSetMem(mem[12])
				draw(vertices=Buffer[0], indices=Buffer[1], program=Program(name=BatchBuilder2D, attributes=[a_Tex, a_Pos], uniforms=[u_ProjMat, u_Tex]), type=TRIANGLES, vertexLayout=VertexLayout[a_Pos, a_Tex], vertexCount=6, offset=0, blending=OVERLAY, uniforms={Uniform(u_ProjMat)=Matrix4([0.003125, 0.0, 0.0, 0.0, 0.0, -0.004166667, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, -1.0, 1.0, -0.0, 1.0]), Uniform(u_Tex)=TextureUnit(texture=Texture[0], linear=true)})
				::draw.indices=[0, 1, 2, 3, 0, 2]
				::draw.vertex[0]: a_Pos[vec2(0.0,0.0)], a_Tex[vec2(0.0,0.0)]
				::draw.vertex[1]: a_Pos[vec2(10.0,0.0)], a_Tex[vec2(1.0,0.0)]
				::draw.vertex[2]: a_Pos[vec2(10.0,10.0)], a_Tex[vec2(1.0,1.0)]
				::draw.vertex[3]: a_Pos[vec2(0.0,10.0)], a_Tex[vec2(0.0,1.0)]
				Buffer[1].close()
				Buffer[0].close()
			""".trimIndent(),
                ag.getLogAsString()
        )
    }
}