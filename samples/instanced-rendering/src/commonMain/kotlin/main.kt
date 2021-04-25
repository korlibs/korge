import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
//import com.soywiz.korge.component.length.bindLength
import com.soywiz.korge.resources.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.bitmap.effect.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.resources.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import kotlin.random.*
import com.soywiz.klock.*

// @TODO: We could autogenerate this via gradle
val ResourcesContainer.korge_png by resourceBitmap("korge.png")

class BunnyContainer(maxSize: Int) : FSprites(maxSize) {
    val speeds = FBuffer(maxSize * Float.SIZE_BYTES * 2).f32
    var FSprite.speedXf: Float get() = speeds[index * 2 + 0] ; set(value) { speeds[index * 2 + 0] = value }
    var FSprite.speedYf: Float get() = speeds[index * 2 + 1] ; set(value) { speeds[index * 2 + 1] = value }
    //var FSprite.tex: BmpSlice
}

/*
class Bunny(tex: BmpSlice) : FastSprite(tex) {
    var speedXf: Float = 0f
    var speedYf: Float = 0f
}
*/

// bunnymark ported from PIXI.js
// https://www.goodboydigital.com/pixijs/bunnymark/
// https://www.goodboydigital.com/pixijs/bunnymark/js/bunnyBenchMark.js
suspend fun main() = Korge(width = 800, height = 600, bgcolor = Colors["#2b2b9b"], batchMaxQuads = BatchBuilder2D.MAX_BATCH_QUADS) {
    println("currentThreadId=$currentThreadId")
    delay(1.milliseconds)
    println("currentThreadId=$currentThreadId")
    println("ag.graphicExtensions=${ag.graphicExtensions}")
    println("ag.isFloatTextureSupported=${ag.isFloatTextureSupported}")
    println("ag.isInstancedSupported=${ag.isInstancedSupported}")
//suspend fun main() = Korge(width = 800, height = 600, bgcolor = Colors["#2b2b9b"]) {
    val wabbitTexture = resourcesVfs["bunnys.png"].readBitmap()

    val bunny1 = wabbitTexture.sliceWithSize(2, 47, 26, 37)
    val bunny2 = wabbitTexture.sliceWithSize(2, 86, 26, 37)
    val bunny3 = wabbitTexture.sliceWithSize(2, 125, 26, 37)
    val bunny4 = wabbitTexture.sliceWithSize(2, 164, 26, 37)
    val bunny5 = wabbitTexture.sliceWithSize(2, 2, 26, 37)

    val startBunnyCount = 2
    //val startBunnyCount = 1_000_000
    // val startBunnyCount = 200_000
    val bunnyTextures = listOf(bunny1, bunny2, bunny3, bunny4, bunny5)
    var currentTexture = bunny1

    val bunnys = BunnyContainer(800_000)
    addChild(bunnys.createView(wabbitTexture))

    val font = DefaultTtfFont.toBitmapFont(fontSize = 16.0, effect = BitmapEffect(dropShadowX = 1, dropShadowY = 1, dropShadowRadius = 1))
    val bunnyCountText = text("", font = font, textSize = 16.0, alignment = com.soywiz.korim.text.TextAlignment.TOP_LEFT).position(16.0, 16.0)


    val random = Random(0)

    fun addBunny(count: Int = 1) {
        for (n in 0 until count) {
            bunnys.apply {
                val bunny = alloc()
                bunny.speedXf = random.nextFloat() * 1
                bunny.speedYf = (random.nextFloat() * 1) - 5
                bunny.setAnchor(.5f, 1f)
                //bunny.width = 10f
                //bunny.height = 20f
                //bunny.alpha = 0.3f + random.nextFloat() * 0.7f
                bunny.setTex(currentTexture)
                bunny.scale(0.5f + random.nextFloat() * 0.5f)
                bunny.radiansf = (random.nextFloat() - 0.5f)
            }
        }
        bunnyCountText.text = "(WIP) KorGE Bunnymark. Bunnies: ${bunnys.size}"
    }

    addBunny(startBunnyCount)

    val maxX = width.toFloat()
    val minX = 0f
    val maxY = height.toFloat()
    val minY = 0f
    val gravity = 0.5f // 1.5f

    mouse {
        up {
            currentTexture = bunnyTextures.random(random)
        }
    }

    addUpdater {
        if (views.input.mouseButtons != 0) {
            if (bunnys.size < 200_000) {
                addBunny(500)
            } else if (bunnys.size < bunnys.maxSize - 1000) {
                addBunny(1000)
            }
        }
        bunnys.fastForEach { bunny ->
            bunny.x += bunny.speedXf
            bunny.y += bunny.speedYf
            bunny.speedYf += gravity

            if (bunny.x > maxX) {
                bunny.speedXf *= -1
                bunny.x = maxX
            } else if (bunny.x < minX) {
                bunny.speedXf *= -1
                bunny.x = minX
            }

            if (bunny.y > maxY) {
                bunny.speedYf *= -0.85f
                bunny.y = maxY
                bunny.radiansf = (random.nextFloat() - 0.5f) * 0.2f
                if (random.nextFloat() > 0.5) {
                    bunny.speedYf -= random.nextFloat() * 6
                }
            } else if (bunny.y < minY) {
                bunny.speedYf = 0f
                bunny.y = minY
            }
        }
    }
}


/*
suspend fun main() {
    //GLOBAL_CHECK_GL = true
    Korge(width = 512, height = 512, bgcolor = Colors["#2b2b2b"], clipBorders = false) {
        gameWindow.icon = korge_png.get().bmp.toBMP32().scaled(32, 32)
        val minDegrees = (-16).degrees
        val maxDegrees = (+16).degrees
        val image = image(korge_png) {
            //val image = image(resourcesVfs["korge.png"].readbitmapslice) {
            rotation = maxDegrees
            anchor(.5, .5)
            scale(.8)
            position(256, 256)
        }
        addChild(MyView())
        //bindLength(image::scaledWidth) { 100.vw }
        //bindLength(image::scaledHeight) { 100.vh }
        while (true) {
            image.tween(image::rotation[minDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
            image.tween(image::rotation[maxDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
        }
    }
}
*/


open class FSprites(val maxSize: Int) {
    var size = 0
    val data = FBuffer(maxSize * FSprites.STRIDE * 4)
    private val i32 = data.i32
    private val f32 = data.f32
    fun uploadVertices(ctx: RenderContext) {
        ctx.fastSpriteBuffer.buffer.upload(data, 0, size * STRIDE * 4)
    }

    fun unloadVertices(ctx: RenderContext) {
        ctx.fastSpriteBuffer.buffer.upload(data, 0, 0)
    }

    fun alloc() = FSprite(size++ * STRIDE)

    var FSprite.x: Float get() = f32[offset + 0] ; set(value) { f32[offset + 0] = value }
    var FSprite.y: Float get() = f32[offset + 1] ; set(value) { f32[offset + 1] = value }
    var FSprite.scaleX: Float get() = f32[offset + 2] ; set(value) { f32[offset + 2] = value }
    var FSprite.scaleY: Float get() = f32[offset + 3] ; set(value) { f32[offset + 3] = value }
    var FSprite.radiansf: Float get() = f32[offset + 4] ; set(value) { f32[offset + 4] = value }
    var FSprite.anchorRaw: Int get() = i32[offset + 5] ; set(value) { i32[offset + 5] = value }
    var FSprite.tex0Raw: Int get() = i32[offset + 6] ; set(value) { i32[offset + 6] = value }
    var FSprite.tex1Raw: Int get() = i32[offset + 7] ; set(value) { i32[offset + 7] = value }

    var FSprite.angle: Angle get() = radiansf.radians ; set(value) { radiansf = value.radians.toFloat() }

    fun FSprite.setAnchor(x: Float, y: Float) {
        anchorRaw = packAnchor(x, y)
    }

    fun FSprite.scale(sx: Float, sy: Float = sx) {
        this.scaleX = sx
        this.scaleY = sy
    }

    fun FSprite.setTex(tex: BmpSlice) {
        tex0Raw = tex.left or (tex.top shl 16)
        tex1Raw = tex.right or (tex.bottom shl 16)
    }

    fun createView(tex: Bitmap) = FView(this, tex)

    class FView(val sprites: FSprites, var tex: Bitmap) : View() {
        var smoothing: Boolean = true
        private val xyData = floatArrayOf(0f, 0f, /**/ 1f, 0f, /**/ 1f, 1f, /**/ 0f, 1f)
        private val u_i_texSizeData = FloatArray(2)

        override fun renderInternal(ctx: RenderContext) {
            ctx.flush()
            val ttex = ctx.agBitmapTextureManager.getTextureBase(tex)
            u_i_texSizeData[0] = 1f / ttex.width.toFloat()
            u_i_texSizeData[1] = 1f / ttex.height.toFloat()
            ctx.batch.setTemporalUniform(u_i_texSize, u_i_texSizeData) {
                ctx.batch.updateStandardUniforms()
                ctx.batch.setViewMatrixTemp(globalMatrix) {
                    ctx.batch.textureUnit.texture = ttex.base
                    ctx.batch.textureUnit.linear = smoothing
                    sprites.uploadVertices(ctx)
                    ctx.xyBuffer.buffer.upload(xyData)
                    ctx.ag.drawV2(
                        vertexData = ctx.buffers,
                        program = vprogram,
                        type = AG.DrawType.TRIANGLE_FAN,
                        vertexCount = 4,
                        instances = sprites.size,
                        uniforms = ctx.batch.uniforms
                    )
                    sprites.unloadVertices(ctx)

                }
            }
            ctx.batch.onInstanceCount(sprites.size)
        }
    }

    companion object {
        const val STRIDE = 8

        val u_i_texSize = Uniform("u_texSize", VarType.Float2)
        val v_color = Varying("v_color", VarType.Float4)
        val a_xy = Attribute("a_xy", VarType.Float2, false)

        val a_pos = Attribute("a_rxy", VarType.Float2, false).withDivisor(1)
        val a_scale = Attribute("a_scale", VarType.Float2, true).withDivisor(1)
        val a_angle = Attribute("a_rangle", VarType.Float1, false).withDivisor(1)
        val a_anchor = Attribute("a_axy", VarType.SShort2, true).withDivisor(1)
        val a_uv0 = Attribute("a_uv0", VarType.UShort2, false).withDivisor(1)
        val a_uv1 = Attribute("a_uv1", VarType.UShort2, false).withDivisor(1)

        val RenderContext.xyBuffer by Extra.PropertyThis<RenderContext, AG.VertexData> {
            ag.createVertexData(a_xy)
        }
        val RenderContext.fastSpriteBuffer by Extra.PropertyThis<RenderContext, AG.VertexData> {
            ag.createVertexData(a_pos, a_scale, a_angle, a_anchor, a_uv0, a_uv1)
        }
        val RenderContext.buffers by Extra.PropertyThis<RenderContext, List<AG.VertexData>> {
            listOf(xyBuffer, fastSpriteBuffer)
        }

        val vprogram = Program(VertexShader {
            DefaultShaders.apply {
                //SET(out, (u_ProjMat * u_ViewMat) * vec4(vec2(a_x, a_y), 0f.lit, 1f.lit))
                //SET(v_color, texture2D(u_Tex, vec2(vec1(id) / 4f.lit, 0f.lit)))
                val baseSize = t_Temp1["xy"]
                SET(baseSize, a_uv1 - a_uv0)

                SET(v_Tex, vec2(
                    mix(a_uv0.x, a_uv1.x, a_xy.x),
                    mix(a_uv0.y, a_uv1.y, a_xy.y),
                ) * u_i_texSize)
                val cos = t_Temp0["x"]
                val sin = t_Temp0["y"]
                SET(cos, cos(a_angle))
                SET(sin, sin(a_angle))
                SET(t_TempMat2, mat2(
                    cos, -sin,
                    sin, cos,
                ))
                val size = t_Temp0["zw"]
                val localPos = t_Temp0["xy"]

                //SET(size, (a_scale * ((a_uv1 - a_uv0) / u_texSize) * 10f.lit))
                SET(size, baseSize)
                SET(localPos, t_TempMat2 * (a_xy - a_anchor) * size)
                SET(out, (u_ProjMat * u_ViewMat) * vec4(localPos + vec2(a_pos.x, a_pos.y), 0f.lit, 1f.lit))
            }
        }, FragmentShader {
            DefaultShaders.apply {
                SET(out, texture2D(u_Tex, v_Tex["xy"]))
                //SET(out, vec4(1f.lit, 0f.lit, 1f.lit, .5f.lit))
                IF(out["a"] le 0f.lit) { DISCARD() }
                SET(out["rgb"], out["rgb"] / out["a"])
            }
        })

        private fun packAnchorComponent(v: Float): Int {
            return (((v + 1f) * .5f) * 0xFFFF).toInt() and 0xFFFF
        }

        fun packAnchor(x: Float, y: Float): Int {
            return (packAnchorComponent(x) and 0xFFFF) or (packAnchorComponent(y) shl 16)
        }
    }
}

inline class FSprite(val id: Int) {
    val offset get() = id
    val index get() = offset / FSprites.STRIDE
    //val offset get() = index * STRIDE
}

inline fun <T : FSprites> T.fastForEach(callback: T.(sprite: FSprite) -> Unit) {
    var m = 0
    for (n in 0 until size) {
        callback(FSprite(m))
        m += FSprites.STRIDE
    }
}
