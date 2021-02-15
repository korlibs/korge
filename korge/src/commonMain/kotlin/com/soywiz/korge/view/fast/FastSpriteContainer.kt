package com.soywiz.korge.view.fast

import com.soywiz.kds.*
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.FBuffer
import com.soywiz.korag.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.internal.min2
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import kotlin.math.min

inline fun Container.fastSpriteContainer(
    callback: @ViewDslMarker FastSpriteContainer.() -> Unit = {}
): FastSpriteContainer = FastSpriteContainer().addTo(this, callback)

class FastSpriteContainer : View() {
    private val sprites = FastArrayList<FastSprite>()

    val numChildren get() = sprites.size

    fun addChild(sprite: FastSprite) {
        this.sprites.add(sprite)
    }

    // 65535 is max vertex index in the index buffer (see ParticleRenderer)
    // so max number of particles is 65536 / 4 = 16384
    // and max number of element in the index buffer is 16384 * 6 = 98304
    // Creating a full index buffer, overhead is 98304 * 2 = 196Ko
    // let numIndices = 98304;

    override fun renderInternal(ctx: RenderContext) {
        val colorMul = this.renderColorMul.value
        val colorAdd = this.renderColorAdd.value
        val sprites = this.sprites
        if (sprites.isEmpty()) return
        ctx.flush()
        val bb = ctx.batch
        val fsprite = sprites.first()
        val bmp = fsprite.tex.bmpBase

        bb.setViewMatrixTemp(globalMatrix) {
            ////////////////////////////

            bb.setStateFast(bmp, true, blendMode.factors, null)

            ////////////////////////////

            val batchSize = min2(sprites.size, bb.maxQuads)
            addQuadIndices(bb, batchSize)
            bb.vertexCount = 0
            bb.uploadIndices()
            val realIndexPos = bb.indexPos

            ////////////////////////////

            //var batchCount = 0
            //var spriteCount = 0
            //for (m in 0 until sprites.size step bb.maxQuads) { // @TODO: Not optimized on Kotlin/JS
            for (m2 in 0 until (sprites.size divCeil bb.maxQuads)) {
                val m = m2 * bb.maxQuads
                //batchCount++
                bb.indexPos = realIndexPos
                renderInternalBatch(bb, m, batchSize, colorMul, colorAdd)
                flush(bb)
            }

            //println("batchCount: $batchCount, spriteCount: $spriteCount")
        }
    }

    private fun addQuadIndices(bb: BatchBuilder2D, batchSize: Int) {
        for (n in 0 until batchSize) {
            bb.addQuadIndices()
            bb.vertexCount += 4
        }
    }

    private fun renderInternalBatch(bb: BatchBuilder2D, m: Int, batchSize: Int, colorMul: Int, colorAdd: Int) {
        val sprites = this.sprites
        var vp = bb.vertexPos
        val vd = bb.verticesFast32
        val mMax = min2(sprites.size, m + batchSize)
        val count = mMax - m
        for (n in m until mMax) {
            //spriteCount++
            val sprite = sprites[n]
            vp = bb._addQuadVerticesFastNormalNonRotated(
                vp, vd,
                sprite.x0, sprite.y0, sprite.x1, sprite.y1,
                sprite.tx0, sprite.ty0, sprite.tx1, sprite.ty1,
                colorMul, colorAdd
            )
        }
        bb.vertexPos = vp
        bb.vertexCount += count * 4
    }

    private fun flush(bb: BatchBuilder2D) {
        bb.flush(uploadVertices = true, uploadIndices = false)
    }
}

private infix fun Int.divCeil(other: Int): Int {
    val res = this / other
    if (this % other != 0) return res + 1
    return res
}
