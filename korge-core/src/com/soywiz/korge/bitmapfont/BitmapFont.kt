package com.soywiz.korge.bitmapfont

import com.soywiz.korag.AG
import com.soywiz.korge.render.BatchBuilder2D
import com.soywiz.korge.render.Texture
import com.soywiz.korge.render.readTexture
import com.soywiz.korge.resources.Path
import com.soywiz.korim.geom.Matrix2d
import com.soywiz.korio.inject.AsyncDependency
import com.soywiz.korio.serialization.xml.get
import com.soywiz.korio.serialization.xml.readXml
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.VfsFile

class BitmapFont(
        val glyphs: Map<Int, Glyph>
) {
    class Glyph(
            val id: Int,
            val texture: Texture,
            val xoffset: Int,
            val yoffset: Int,
            val xadvance: Int
    )

    operator fun get(charCode: Int) = glyphs[charCode] ?: glyphs[32] ?: glyphs.values.first()
    operator fun get(char: Char) = this[char.toInt()]

    fun drawText(batch: BatchBuilder2D, str: String, x: Int, y: Int, m: Matrix2d = Matrix2d()) {
        var dx = x
        for (c in str) {
            val glyph = this[c]
            val tex = glyph.texture
            batch.setStateFast(tex.base, smoothing = true)
            batch.addQuad(tex, dx.toFloat(), y.toFloat(), m = m)
            dx += glyph.xadvance
        }
    }
}

suspend fun VfsFile.readBitmapFont(ag: AG): BitmapFont {
    val fntFile = this
    val xml = fntFile.readXml()
    val textures = hashMapOf<Int, Texture>()
    for (page in xml["pages"]["page"]) {
        val id = page.int("id")
        val name = page.str("file")
        val texFile = fntFile.parent[name]
        val tex = texFile.readTexture(ag)
        textures[id] = tex
    }
    val texture = textures.values.first()
    val glyphs = hashMapOf<Int, BitmapFont.Glyph>()
    for (char in xml["chars"]["char"]) {
        val id = char.int("id")
        val x = char.int("x")
        val y = char.int("y")
        val width = char.int("width")
        val height = char.int("height")
        val xoffset = char.int("xoffset")
        val yoffset = char.int("yoffset")
        val xadvance = char.int("xadvance")

        glyphs[id] = BitmapFont.Glyph(
                id = id,
                texture = texture.slice(x, y, width, height),
                xoffset = xoffset,
                yoffset = yoffset,
                xadvance = xadvance
        )
    }

    return BitmapFont(glyphs)
    //println(xml)
}

class BitmapFontRef(
        private val path: Path,
        private val ag: AG
) : AsyncDependency {
    lateinit var font: BitmapFont; private set

    suspend override fun init() {
        font = ResourcesVfs[path.path].readBitmapFont(ag)
    }
}