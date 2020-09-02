package com.soywiz.korge3d

import com.soywiz.kds.get
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.iterators.fastForEachWithIndex
import com.soywiz.kmem.clamp
import com.soywiz.korag.AG
import com.soywiz.korge.view.BlendMode
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.BitmapFont
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Vector3D
import com.soywiz.korma.geom.invert

fun BitmapFont.drawText3D(
    ctx: RenderContext3D,
    v1: Vector3D,
    v2: Vector3D,
    v3: Vector3D,
    v4: Vector3D,
    textSize: Double,
    str: String,
    m: Matrix = Matrix(),
    colMul: RGBA = Colors.WHITE,
    colAdd: Int = 0x7f7f7f7f,
    blendMode: BlendMode = BlendMode.INHERIT,
    filtering: Boolean = true
) {
    val m2 = m.clone()
    val scale = textSize / fontSize.toDouble()
    m2.prescale(scale, scale)
    var dx = 0.0
    var dy = 0.0
    val meshBuilder = MeshBuilder3D()
    val dv1 = Vector3D(v1.x, v1.y, v1.z, v1.w)
    val dv2 = Vector3D(v2.x, v2.y, v3.z, v2.w)
    val dv3 = Vector3D(v3.x, v3.y, v3.z, v3.w)
    val dv4 = Vector3D(v4.x, v4.y, v4.z, v4.w)
    for (n in str.indices) {
        val c1 = str[n].toInt()
        if (c1 == '\n'.toInt()) {
            dx = 0.0
            dy += fontSize
            continue
        }
        val c2 = str.getOrElse(n + 1) { ' ' }.toInt()
        val glyph = this[c1]
        val tex = glyph.texture

        meshBuilder.faceRectangle(
            v1, v2, v3, v4
        )
        ctx.batch.drawQuad(
            ctx.rctx.getTex(tex),
            (dx + glyph.xoffset).toFloat(),
            (dy + glyph.yoffset).toFloat(),
            m = m2,
            colorMul = colMul,
            colorAdd = colAdd,
            blendFactors = blendMode.factors,
            filtering = filtering
        )
        val kerningOffset = kernings[BitmapFont.Kerning.buildKey(c1, c2)]?.amount ?: 0
        dx += glyph.xadvance + kerningOffset
    }
    val mesh = meshBuilder.build()
}

fun RenderContext3D.drawText(
    v1: Vector3D,
    v2: Vector3D,
    v3: Vector3D,
    v4: Vector3D,
    font: BitmapFont,
    textSize: Double,
    str: String,
    m: Matrix = Matrix(),
    colMul: RGBA = Colors.WHITE,
    colAdd: Int = 0x7f7f7f7f,
    blendMode: BlendMode = BlendMode.INHERIT,
    filtering: Boolean = true
) {
    font.drawText3D(
        this,
        v1, v2, v3, v4,
        textSize, str, m, colMul, colAdd, blendMode, filtering
    )
}
