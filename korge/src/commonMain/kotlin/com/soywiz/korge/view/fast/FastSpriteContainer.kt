package com.soywiz.korge.view.fast

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.FBuffer
import com.soywiz.korag.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import kotlin.math.min

inline fun Container.fastSpriteContainer(
    callback: @ViewDslMarker FastSpriteContainer.() -> Unit = {}
): FastSpriteContainer = FastSpriteContainer().addTo(this, callback)

class FastSpriteContainer : View() {
    private val sprites = arrayListOf<FastSprite>()

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
        val sprites = this.sprites
        if (sprites.isEmpty()) return
        ctx.flush()
        val bb = ctx.batch
        val fsprite = sprites.first()
        val bmp = fsprite.tex.bmp
        val colorMul = this.renderColorMul.value
        val colorAdd = this.renderColorAdd

        bb.setViewMatrixTemp(globalMatrix) {
            ////////////////////////////

            bb.setStateFast(bmp, true, blendMode.factors, null)

            ////////////////////////////

            val batchSize = min(sprites.size, bb.maxQuads)
            for (n in 0 until batchSize) {
                bb.addQuadIndices()
                bb.vertexCount += 4
            }
            bb.vertexCount = 0
            bb.uploadIndices()
            val realIndexPos = bb.indexPos

            ////////////////////////////

            //var batchCount = 0
            //var spriteCount = 0
            for (m in 0 until sprites.size step bb.maxQuads) {
                //batchCount++
                bb.indexPos = realIndexPos
                for (n in m until min(sprites.size, m + batchSize)) {
                    //spriteCount++
                    val sprite = sprites[n]

                    val w = sprite.width * sprite.scalef
                    val h = sprite.height * sprite.scalef

                    val ax = w * sprite.anchorXf
                    val ay = h * sprite.anchorYf

                    val x0 = sprite.xf - ax
                    val y0 = sprite.yf - ay

                    val x1 = x0 + w
                    val y1 = y0 + h

                    bb.addQuadVerticesFastNormal(
                        x0, y0, x1, y0, x1, y1, x0, y1,
                        sprite.tx0, sprite.ty0, sprite.tx1, sprite.ty1,
                        colorMul, colorAdd
                    )
                }
                bb.flush(uploadVertices = true, uploadIndices = false)
            }

            //println("batchCount: $batchCount, spriteCount: $spriteCount")

            ////////////////////////////
        }
    }
}
