package com.soywiz.korge.render

import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlProxyLogToString
import com.soywiz.korag.gl.*
import com.soywiz.korag.log.AGLog
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.ForcedTexNativeImage
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korim.color.Colors
import kotlin.test.*

class AgBitmapTextureManagerTest {
    val ag = AGLog()
    val tm = AgBitmapTextureManager(ag)

	@Test
	fun test() {
		val bmp1 = Bitmap32(32, 32, premultiplied = true)
		val slice1 = bmp1.sliceWithSize(0, 0, 16, 16)
		val slice2 = bmp1.sliceWithSize(16, 0, 16, 16)
		val tex1a = tm.getTexture(slice1)
		val tex1b = tm.getTexture(slice1)
		val tex2a = tm.getTexture(slice2)
		val tex1c = tm.getTexture(slice1)
		val tex2b = tm.getTexture(slice2)
        assertEquals(1, tm.getBitmapsWithTextureInfoCopy().size)

		assertSame(tex1a, tex1b)
		assertSame(tex1a, tex1c)
		assertSame(tex2a, tex2b)
        assertEquals(4096L, tm.managedTextureMemory)
		tm.gc()
        assertEquals(1, tm.getBitmapsWithTextureInfoCopy().size)
        tm.gc()
        assertEquals(0, tm.getBitmapsWithTextureInfoCopy().size)

        assertEquals(0L, tm.managedTextureMemory)

        val tex1AfterGc = tm.getTexture(slice1)
		assertNotSame(tex1a, tex1AfterGc)
    }

    @Test
    fun testMaxMemoryKeepsTextureForLater() {
        tm.maxCachedMemory = 4096L
        val bmp1 = Bitmap32(32, 32, premultiplied = true)
        val slice1a = bmp1.sliceWithSize(0, 0, 16, 16)
        val slice1b = bmp1.sliceWithSize(16, 0, 16, 16)
        val tex1a = tm.getTexture(slice1a)
        val tex1b = tm.getTexture(slice1a)
        val tex2a = tm.getTexture(slice1b)
        val tex1c = tm.getTexture(slice1a)
        val tex2b = tm.getTexture(slice1b)
        assertEquals(1, tm.getBitmapsWithTextureInfoCopy().size)
        assertEquals(4096L, tm.managedTextureMemory)
        tm.gc()
        tm.gc()
        assertEquals(1, tm.getBitmapsWithTextureInfoCopy().size)
        val bmp2 = Bitmap32(32, 32, premultiplied = true)
        val slice2a = bmp2.sliceWithSize(0, 0, 16, 16)
        val tex22a = tm.getTexture(slice2a)
        assertEquals(8192L, tm.managedTextureMemory)
        assertEquals(2, tm.getBitmapsWithTextureInfoCopy().size)
        tm.gc()
        tm.gc()
        assertEquals(4096L, tm.managedTextureMemory)
        assertEquals(1, tm.getBitmapsWithTextureInfoCopy().size)
        tm.close()
        assertEquals(0, tm.getBitmapsWithTextureInfoCopy().size)
    }

    @Test
    @Ignore
    fun testNativeImage() {
        val gl = object : KmlGlProxyLogToString() {
            override fun getString(name: String, params: List<Any?>, result: Any?): String? {
                if (!name.contains("tex", ignoreCase = true)) return null
                return super.getString(name, params, result)
            }
        }
        val ag = AGOpengl(gl)
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
        //ag.commands { list ->
        //    val program = list.createProgram(DefaultShaders.PROGRAM_DEFAULT)
        //    list.useProgram(program)
        //    list.uniformsSet(AGUniformValues {
        //        it[DefaultShaders.u_Tex] = AGTextureUnit(0, tex1.base)
        //        it[DefaultShaders.u_Tex2] = AGTextureUnit(1, tex0.base)
        //    })
        //}
        ag.sync()
        val tex2 = tm.getTextureBase(image1)
        val tex3 = tm.getTextureBase(fixedNativeImage2)
        tex3.base?.forcedTexId = fixedNativeImage
        tm.afterRender()
        tm.gc()
        ag.sync() // Ensure commands are executed

        val tex4 = tm.getTextureBase(image1)
        tm.afterRender()
        tm.gc()
        ag.sync() // Ensure commands are executed

        tm.afterRender()
        tm.gc()
        ag.sync() // Ensure commands are executed

        assertEquals(
            """
                activeTexture(33984)
                genTextures(1, [6001])
                bindTexture(3553, 6001)
                texImage2D(3553, 0, 6408, 6408, 5121, FixedNativeImage(100, 100))
                texParameteri(36197, 10242, 33071)
                texParameteri(36197, 10243, 33071)
                texParameteri(36197, 32882, 33071)
                texParameteri(36197, 10241, 9729)
                texParameteri(36197, 10240, 9729)
                activeTexture(33985)
                genTextures(1, [6002])
                bindTexture(3553, 6002)
                texImage2D(3553, 0, 6408, 1, 1, 0, 6408, 5121, Buffer(size=4))
                texParameteri(3553, 10242, 33071)
                texParameteri(3553, 10243, 33071)
                texParameteri(3553, 10241, 9729)
                texParameteri(3553, 10240, 9729)
            """.trimIndent(),
            gl.log.joinToString("\n")
        )
    }
}
