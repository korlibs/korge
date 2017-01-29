package com.soywiz.korge.bitmapfont

import com.soywiz.korag.log.LogAG
import com.soywiz.korge.render.RenderContext
import com.soywiz.korio.async.EventLoopTest
import com.soywiz.korio.async.sync
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class BitmapFontTest {
    val ag = LogAG()
    val ctx = RenderContext(ag)

    @Test
    fun name() = sync(EventLoopTest()) {
        val font = ResourcesVfs["font/font.fnt"].readBitmapFont(ag)
        Assert.assertEquals(81, font.glyphs.size)
        //<char id="64" x="253" y="350" width="69" height="70" xoffset="4" yoffset="4" xadvance="73" page="0" chnl="15"/>
        val glyph = font[64]
        Assert.assertEquals(69, glyph.texture.width)
        Assert.assertEquals(70, glyph.texture.height)
        Assert.assertEquals(4, glyph.xoffset)
        Assert.assertEquals(4, glyph.yoffset)
        Assert.assertEquals(73, glyph.xadvance)

        font.drawText(ctx.batch, "AB", 0, 0)
        ctx.flush()

        Assert.assertEquals(
                """
                    createTexture():0
                    Texture[0].uploadBitmap32(Bitmap32(361, 512))
                    Texture[0].createMipmaps()
                    createBuffer(VERTEX):0
                    Buffer[0].afterSetMem(mem[160])
                    createBuffer(INDEX):1
                    Buffer[1].afterSetMem(mem[24])
                    draw(vertices=Buffer[0], indices=Buffer[1], program=Program(name=BatchBuilder2D.Tinted, attributes=[a_Tex, a_Col, a_Pos], uniforms=[u_ProjMat, u_Tex]), type=TRIANGLES, vertexLayout=VertexLayout[a_Pos, a_Tex, a_Col], vertexCount=12, offset=0, blending=OVERLAY, uniforms={Uniform(u_ProjMat)=Matrix4([0.003125, 0.0, 0.0, 0.0, 0.0, -0.004166667, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, -1.0, 1.0, -0.0, 1.0]), Uniform(u_Tex)=TextureUnit(texture=Texture[0], linear=true)})
                    ::draw.indices=[0, 1, 2, 3, 0, 2, 4, 5, 6, 7, 4, 6]
                    ::draw.vertex[0]: a_Pos[vec2(0.0,0.0)], a_Tex[vec2(0.21052632,0.2797784)], a_Col[byte4(-1)]
                    ::draw.vertex[1]: a_Pos[vec2(51.0,0.0)], a_Tex[vec2(0.35180056,0.2797784)], a_Col[byte4(-1)]
                    ::draw.vertex[2]: a_Pos[vec2(51.0,54.0)], a_Tex[vec2(0.35180056,0.4293629)], a_Col[byte4(-1)]
                    ::draw.vertex[3]: a_Pos[vec2(0.0,54.0)], a_Tex[vec2(0.21052632,0.4293629)], a_Col[byte4(-1)]
                    ::draw.vertex[4]: a_Pos[vec2(48.0,0.0)], a_Tex[vec2(0.33240998,0.12188365)], a_Col[byte4(-1)]
                    ::draw.vertex[5]: a_Pos[vec2(89.0,0.0)], a_Tex[vec2(0.44598338,0.12188365)], a_Col[byte4(-1)]
                    ::draw.vertex[6]: a_Pos[vec2(89.0,54.0)], a_Tex[vec2(0.44598338,0.27146813)], a_Col[byte4(-1)]
                    ::draw.vertex[7]: a_Pos[vec2(48.0,54.0)], a_Tex[vec2(0.33240998,0.27146813)], a_Col[byte4(-1)]
                    Buffer[1].close()
                    Buffer[0].close()
                """.trimIndent(),
                ag.getLogAsString()
        )

    }
}