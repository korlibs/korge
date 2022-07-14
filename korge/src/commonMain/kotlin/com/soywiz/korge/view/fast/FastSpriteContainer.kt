package com.soywiz.korge.view.fast

import com.soywiz.kds.FastArrayList
import com.soywiz.korge.render.BatchBuilder2D
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.addTo
import kotlin.math.min

inline fun Container.fastSpriteContainer(
    useRotation: Boolean = false,
    smoothing: Boolean = true,
    callback: @ViewDslMarker FastSpriteContainer.() -> Unit = {}
): FastSpriteContainer = FastSpriteContainer(useRotation, smoothing).addTo(this, callback)

class FastSpriteContainer(val useRotation: Boolean = false, var smoothing: Boolean = true) : View() {
    private val sprites = FastArrayList<FastSprite>()

    val numChildren get() = sprites.size

    fun addChild(sprite: FastSprite) {
        if(sprite.useRotation != useRotation) {
            sprite.useRotation = useRotation
            // force update the sprite just in case the FastSprite properties were updated before being
            // added to the container
            sprite.forceUpdate()
        }
        sprite.container = this
        this.sprites.add(sprite)
    }

    // alias for addChild
    fun alloc(sprite: FastSprite) = addChild(sprite)

    fun removeChild(sprite: FastSprite) {
        this.sprites.remove(sprite)
        sprite.container = null
    }

    // alias for removeChild
    fun delete(sprite: FastSprite) = removeChild(sprite)

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
        ctx.useBatcher { bb ->
            val fsprite = sprites.first()
            val bmp = fsprite.tex.bmpBase

            bb.setViewMatrixTemp(globalMatrix) {
                ////////////////////////////

                bb.setStateFast(bmp, smoothing, blendMode, null, icount = 0, vcount = 0)

                ////////////////////////////

                val batchSize = min(sprites.size, bb.maxQuads)
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
    }

    private fun addQuadIndices(bb: BatchBuilder2D, batchSize: Int) {
        bb.addQuadIndicesBatch(batchSize)
    }

    private fun renderInternalBatch(bb: BatchBuilder2D, m: Int, batchSize: Int, colorMul: Int, colorAdd: Int) {
        val sprites = this.sprites
        var vp = bb.vertexPos
        val vd = bb.verticesFast32
        val mMax = min(sprites.size, m + batchSize)
        var count = mMax - m
        for (n in m until mMax) {
            //spriteCount++
            val sprite = sprites[n]
            if (!sprite.visible) {
                count--
                continue
            }

            if (useRotation) {
                vp = bb._addQuadVerticesFastNormal(
                    vp, vd,
                    sprite.x0, sprite.y0, sprite.x1, sprite.y1,
                    sprite.x2, sprite.y2, sprite.x3, sprite.y3,
                    sprite.tx0, sprite.ty0, sprite.tx1, sprite.ty1,
                    sprite.color.value, colorAdd
                )
            } else {
                vp = bb._addQuadVerticesFastNormalNonRotated(
                    vp, vd,
                    sprite.x0, sprite.y0, sprite.x1, sprite.y1,
                    sprite.tx0, sprite.ty0, sprite.tx1, sprite.ty1,
                    sprite.color.value, colorAdd
                )
            }
        }
        bb.vertexPos = vp
        bb.vertexCount = count * 4
        bb.indexPos = count * 6
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
