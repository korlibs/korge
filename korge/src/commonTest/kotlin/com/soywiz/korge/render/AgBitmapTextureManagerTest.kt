package com.soywiz.korge.render

import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlProxyLogToString
import com.soywiz.korag.AG
import com.soywiz.korag.gl.AGOpengl
import com.soywiz.korag.gl.SimpleAGOpengl
import com.soywiz.korag.gl.fromGl
import com.soywiz.korag.log.LogAG
import com.soywiz.korim.bitmap.Bitmap32
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
        val gl = KmlGlProxyLogToString()
        val tm = AgBitmapTextureManager(SimpleAGOpengl(gl, checked = true))

        class FixedNativeImage(
            override val forcedTexId: Int = 100,
            override val forcedTexTarget: Int = KmlGl.TEXTURE_EXTERNAL_OES,
        ) : NativeImage(100, 100, null, false) {
            override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
                TODO("Not yet implemented")
            }

            override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
                TODO("Not yet implemented")
            }
        }

        val image1 = Bitmap32(1, 1, Colors.RED)
        val fixedNativeImage = FixedNativeImage(100)
        val fixedNativeImage2 = FixedNativeImage(101)

        val tex0 = tm.getTextureBase(image1)
        val tex1 = tm.getTextureBase(fixedNativeImage)
        tex1.base?.implForcedTexId = fixedNativeImage.forcedTexId
        tex1.base?.implForcedTexTarget = AG.TextureTargetKind.fromGl(fixedNativeImage.forcedTexTarget)
        val tex2 = tm.getTextureBase(image1)
        val tex3 = tm.getTextureBase(fixedNativeImage2)
        tex3.base?.implForcedTexId = fixedNativeImage.forcedTexId
        tex3.base?.implForcedTexTarget = AG.TextureTargetKind.fromGl(fixedNativeImage.forcedTexTarget)
        tm.afterRender()
        tm.gc()

        val tex4 = tm.getTextureBase(image1)
        tm.afterRender()
        tm.gc()

        tm.afterRender()
        tm.gc()

        assertEquals(
            """
                genTextures(1, [6001])
                genTextures(1, [6002])
                genTextures(1, [6003])
                deleteTextures(1, [6002])
                deleteTextures(1, [6003])
                deleteTextures(1, [6001])
            """.trimIndent(),
            gl.log.joinToString("\n")
        )
    }
}
