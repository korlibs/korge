package com.soywiz.korge.bitmapfont

import com.soywiz.korag.log.LogAG
import com.soywiz.korge.render.RenderContext
import com.soywiz.korio.async.EventLoopTest
import com.soywiz.korio.async.sync
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Assert
import org.junit.Test

class BitmapFontTest {
    val ag = LogAG()
    val ctx = RenderContext(ag)

    @Test
    fun simple() = sync(EventLoopTest()) {
        val font = ResourcesVfs["font/font.fnt"].readBitmapFont(ag)
        Assert.assertEquals(81, font.glyphs.size)
        val glyph = font[64]
        Assert.assertEquals(69, glyph.texture.width)
        Assert.assertEquals(70, glyph.texture.height)
        Assert.assertEquals(4, glyph.xoffset)
        Assert.assertEquals(4, glyph.yoffset)
        Assert.assertEquals(73, glyph.xadvance)

        font.drawText(ctx.batch, 72.0 / 4.0, "ABF,", 0, 0)
        ctx.flush()

        Assert.assertEquals(
                """
                    createTexture():0
                    Texture[0].uploadBitmap32(Bitmap32(361, 512))
                    Texture[0].createMipmaps()
                    createBuffer(VERTEX):0
                    Buffer[0].afterSetMem(mem[320])
                    createBuffer(INDEX):1
                    Buffer[1].afterSetMem(mem[48])
                    draw(vertices=Buffer[0], indices=Buffer[1], program=Program(name=BatchBuilder2D.Tinted, attributes=[a_Tex, a_Col, a_Pos], uniforms=[u_ProjMat, u_Tex]), type=TRIANGLES, vertexLayout=VertexLayout[a_Pos, a_Tex, a_Col], vertexCount=24, offset=0, blending=BlendFactors(srcRGB=SOURCE_ALPHA, dstRGB=ONE_MINUS_SOURCE_ALPHA, srcA=ONE, dstA=ONE_MINUS_SOURCE_ALPHA), uniforms={Uniform(u_ProjMat)=Matrix4([0.003125, 0.0, 0.0, 0.0, 0.0, -0.004166667, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, -1.0, 1.0, -0.0, 1.0]), Uniform(u_Tex)=TextureUnit(texture=Texture[0], linear=true)})
                    ::draw.indices=[0, 1, 2, 3, 0, 2, 4, 5, 6, 7, 4, 6, 8, 9, 10, 11, 8, 10, 12, 13, 14, 15, 12, 14]
                    ::draw.vertex[0]: a_Pos[vec2(0.0,1.25)], a_Tex[vec2(0.21052632,0.19726562)], a_Col[byte4(-1)]
                    ::draw.vertex[1]: a_Pos[vec2(12.75,1.25)], a_Tex[vec2(0.35180056,0.19726562)], a_Col[byte4(-1)]
                    ::draw.vertex[2]: a_Pos[vec2(12.75,14.75)], a_Tex[vec2(0.35180056,0.30273438)], a_Col[byte4(-1)]
                    ::draw.vertex[3]: a_Pos[vec2(0.0,14.75)], a_Tex[vec2(0.21052632,0.30273438)], a_Col[byte4(-1)]
                    ::draw.vertex[4]: a_Pos[vec2(13.25,1.25)], a_Tex[vec2(0.33240998,0.0859375)], a_Col[byte4(-1)]
                    ::draw.vertex[5]: a_Pos[vec2(23.5,1.25)], a_Tex[vec2(0.44598338,0.0859375)], a_Col[byte4(-1)]
                    ::draw.vertex[6]: a_Pos[vec2(23.5,14.75)], a_Tex[vec2(0.44598338,0.19140625)], a_Col[byte4(-1)]
                    ::draw.vertex[7]: a_Pos[vec2(13.25,14.75)], a_Tex[vec2(0.33240998,0.19140625)], a_Col[byte4(-1)]
                    ::draw.vertex[8]: a_Pos[vec2(25.5,1.25)], a_Tex[vec2(0.35734072,0.1953125)], a_Col[byte4(-1)]
                    ::draw.vertex[9]: a_Pos[vec2(34.75,1.25)], a_Tex[vec2(0.4598338,0.1953125)], a_Col[byte4(-1)]
                    ::draw.vertex[10]: a_Pos[vec2(34.75,14.75)], a_Tex[vec2(0.4598338,0.30078125)], a_Col[byte4(-1)]
                    ::draw.vertex[11]: a_Pos[vec2(25.5,14.75)], a_Tex[vec2(0.35734072,0.30078125)], a_Col[byte4(-1)]
                    ::draw.vertex[12]: a_Pos[vec2(34.5,12.25)], a_Tex[vec2(0.5734072,0.69921875)], a_Col[byte4(-1)]
                    ::draw.vertex[13]: a_Pos[vec2(37.0,12.25)], a_Tex[vec2(0.601108,0.69921875)], a_Col[byte4(-1)]
                    ::draw.vertex[14]: a_Pos[vec2(37.0,17.25)], a_Tex[vec2(0.601108,0.73828125)], a_Col[byte4(-1)]
                    ::draw.vertex[15]: a_Pos[vec2(34.5,17.25)], a_Tex[vec2(0.5734072,0.73828125)], a_Col[byte4(-1)]
                    Buffer[1].close()
                    Buffer[0].close()
                """.trimIndent(),
                ag.getLogAsString()
        )

    }
}
