package com.soywiz.korge.render

import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlProxyLogToString
import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.gl.AGOpengl
import com.soywiz.korag.gl.SimpleAGOpengl
import com.soywiz.korag.gl.fromGl
import com.soywiz.korag.log.LogAG
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.ForcedTexNativeImage
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RgbaArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AgBitmapTextureManagerTest {
	@Test
	fun test() {
		val ag = LogAG()
		val tm = AgBitmapTextureManager(ag)
		val bmp1 = Bitmap32(32, 32)
		val slice1 = bmp1.sliceWithSize(0, 0, 16, 16)
		val slice2 = bmp1.sliceWithSize(16, 0, 16, 16)
		val tex1a = tm.getTexture(slice1)
		val tex1b = tm.getTexture(slice1)
		val tex2a = tm.getTexture(slice2)
		val tex1c = tm.getTexture(slice1)
		val tex2b = tm.getTexture(slice2)
		assertSame(tex1a, tex1b)
		assertSame(tex1a, tex1c)
		assertSame(tex2a, tex2b)
		tm.gc()
		val tex1AfterGc = tm.getTexture(slice1)
		//assertNotSame(tex1a, tex1AfterGc) // @TODO: Check this!
	}

    @Test
    fun testNativeImage() {
        val gl = object : KmlGlProxyLogToString() {
            override fun getString(name: String, params: List<Any?>, result: Any?): String? {
                if (!name.contains("tex", ignoreCase = true)) return null
                return super.getString(name, params, result)
            }
        }
        val ag = SimpleAGOpengl(gl, checked = true)
        val tm = AgBitmapTextureManager(ag)

        class FixedNativeImage(
            override val forcedTexId: Int = 100,
            override val forcedTexTarget: Int = KmlGl.TEXTURE_EXTERNAL_OES,
        ) : ForcedTexNativeImage(100, 100, false) {
            override val name: String get() = "FixedNativeImage"
        }

        val image1 = Bitmap32(1, 1, Colors.RED)
        val fixedNativeImage = FixedNativeImage(100)
        val fixedNativeImage2 = FixedNativeImage(101)

        val tex0 = tm.getTextureBase(image1)
        val tex1 = tm.getTextureBase(fixedNativeImage)
        tex1.base?.forcedTexId = fixedNativeImage
        ag.commandsSync { list ->
            list.tempUBOs {
                val program = list.createProgram(DefaultShaders.PROGRAM_DEFAULT)
                list.useProgram(program)
                list.uboSet(it, AG.UniformValues(
                    DefaultShaders.u_Tex to AG.TextureUnit(tex1.base),
                    DefaultShaders.u_Tex2 to AG.TextureUnit(tex0.base),
                ))
                list.uboUse(it)
            }
        }
        val tex2 = tm.getTextureBase(image1)
        val tex3 = tm.getTextureBase(fixedNativeImage2)
        tex3.base?.forcedTexId = fixedNativeImage
        tm.afterRender()
        tm.gc()
        ag.commandsSync {  } // Ensure commands are executed

        val tex4 = tm.getTextureBase(image1)
        tm.afterRender()
        tm.gc()
        ag.commandsSync {  } // Ensure commands are executed

        tm.afterRender()
        tm.gc()
        ag.commandsSync {  } // Ensure commands are executed

        assertEquals(
            """
                genTextures(1, [6001])
                genTextures(1, [6002])
                activeTexture(33984)
                bindTexture(36197, 100)
                texParameteri(36197, 10242, 33071)
                texParameteri(36197, 10243, 33071)
                texParameteri(36197, 10241, 9729)
                texParameteri(36197, 10240, 9729)
                activeTexture(33985)
                bindTexture(3553, 6001)
                texImage2D(3553, 0, 6408, 1, 1, 0, 6408, 5121, FBuffer(size=4))
                texParameteri(3553, 10242, 33071)
                texParameteri(3553, 10243, 33071)
                texParameteri(3553, 10241, 9729)
                texParameteri(3553, 10240, 9729)
                genTextures(1, [6003])
                deleteTextures(1, [6002])
                deleteTextures(1, [6003])
                deleteTextures(1, [6001])
            """.trimIndent(),
            gl.log.joinToString("\n")
        )
    }
}
