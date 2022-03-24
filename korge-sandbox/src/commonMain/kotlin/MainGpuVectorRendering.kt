import com.soywiz.kds.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.BlendMode
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.text.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*

suspend fun Stage.mainGpuVectorRendering() {
    Console.log("[1]")
    val korgeBitmap = resourcesVfs["korge.png"].readBitmap()
    Console.log("[2]")
    val tigerSvg = resourcesVfs["Ghostscript_Tiger.svg"].readSVG()
    Console.log("[3]")
    //AudioData(44100, AudioSamples(1, 1024)).toSound().play()

    fun Context2d.buildGraphics() {
        keep {
            draw(tigerSvg)
            translate(100, 200)
            fill(Colors.BLUE) {
                rect(-10, -10, 120, 120)
                rectHole(40, 40, 80, 80)
            }
            fill(Colors.YELLOW) {
                this.circle(100, 100, 40)
                //rect(-100, -100, 500, 500)
                //rectHole(40, 40, 320, 320)
            }
            fill(Colors.RED) {
                regularPolygon(6, 30.0, x = 100.0, y = 100.0)
                //rect(-100, -100, 500, 500)
                //rectHole(40, 40, 320, 320)
            }
        }
        keep {
            translate(100, 20)
            scale(2.0)
            globalAlpha = 0.75
            fillStyle = BitmapPaint(
                korgeBitmap,
                Matrix().translate(50, 50).scale(0.125),
                cycleX = CycleMethod.REPEAT,
                cycleY = CycleMethod.REPEAT
            )
            fillRect(0.0, 0.0, 100.0, 100.0)
            fillStyle =
                createLinearGradient(0.0, 0.0, 200.0, 200.0, transform = Matrix().scale(0.5).pretranslate(130, 30))
                    .addColorStop(0.0, Colors.RED)
                    .addColorStop(1.0, Colors.BLUE)
            fillRect(100.0, 0.0, 100.0, 100.0)
        }
        keep {
            font = DefaultTtfFont
            fontSize = 16.0
            fillStyle = Colors.WHITE
            alignment = TextAlignment.TOP_LEFT
            fillText("HELLO WORLD", 0.0, 16.0)
        }
    }

    gpuShapeView { buildGraphics() }.xy(0, 0)//.rotation(45.degrees)
    image(NativeImage(512, 512).context2d { buildGraphics() }).xy(700, 0)
    image(Bitmap32(512, 512).context2d { buildGraphics() }).xy(700, 370)
}

inline fun Container.gpuShapeView(buildContext2d: Context2d.() -> Unit) =
    GpuShapeView(buildShape { buildContext2d() }).addTo(this)

inline fun Container.gpuShapeView(shape: Shape, callback: @ViewDslMarker GpuShapeView.() -> Unit = {}) =
    GpuShapeView(shape).addTo(this, callback)

class GpuShapeView(shape: Shape) : View() {
    private val pointCache = FastIdentityMap<VectorPath, PointArrayList>()

    var shape: Shape = shape
        set(value) {
            field = value
            pointCache.clear()
        }

    companion object {
        val u_Color = Uniform("u_Color", VarType.Float4)
        val u_Transform = Uniform("u_Transform", VarType.Mat4)
        val LAYOUT = VertexLayout(DefaultShaders.a_Pos)
        val LAYOUT_FILL = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex)
        val VERTEX_FILL = VertexShaderDefault {
            SET(out, u_ProjMat * u_ViewMat * vec4(a_Pos, 0f.lit, 1f.lit))
            SET(v_Tex, DefaultShaders.a_Tex)
        }
        val PROGRAM_STENCIL = Program(
            vertex = VertexShaderDefault { SET(out, u_ProjMat * u_ViewMat * vec4(a_Pos, 0f.lit, 1f.lit)) },
            fragment = FragmentShaderDefault { SET(out, vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit)) },
        )
        val PROGRAM_COLOR = Program(
            vertex = VERTEX_FILL,
            fragment = FragmentShaderDefault {
                SET(out, u_Color)
            },
        )
        val PROGRAM_BITMAP = Program(
            vertex = VERTEX_FILL,
            fragment = FragmentShaderDefault {
                // @TODO: we should convert 0..1 to texture slice coordinates
                SET(out, texture2D(u_Tex, fract(vec2((vec4(v_Tex, 0f.lit, 1f.lit) * u_Transform)["xy"]))))
            },
        )
        val PROGRAM_LINEAR_GRADIENT = Program(
            vertex = VERTEX_FILL,
            fragment = FragmentShaderDefault {
                SET(out, texture2D(u_Tex, (vec4(v_Tex.x, v_Tex.y, 0f.lit, 1f.lit) * u_Transform)["xy"]))
            },
        )
    }

    private val bb = BoundsBuilder()
    override fun getLocalBoundsInternal(out: Rectangle) {
        shape.getBounds(out, bb)
    }

    var msaaSamples: Int = 4

    override fun renderInternal(ctx: RenderContext) {
        ctx.flush()
        val currentRenderBuffer = ctx.ag.currentRenderBufferOrMain
        ctx.renderToTexture(currentRenderBuffer.width, currentRenderBuffer.height, {
            renderShape(ctx, shape)
        }, hasStencil = true, msamples = msaaSamples) { texture ->
            ctx.useBatcher {
                it.drawQuad(texture, x = 0f, y = 0f)
            }
        }
    }

    private fun renderShape(ctx: RenderContext, shape: Shape) {
        when (shape) {
            EmptyShape -> Unit
            is FillShape -> renderShape(ctx, shape)
            is CompoundShape -> for (v in shape.components) renderShape(ctx, v)
            is PolylineShape -> {
                //println("TODO: PolylineShape not implemented. Convert into fills")
            }
            is TextShape -> renderShape(ctx, shape.primitiveShapes)
            else -> TODO("shape=$shape")
        }
    }

    private fun renderShape(ctx: RenderContext, shape: FillShape) {
        val path = shape.path
        val m = globalMatrix

        val points = pointCache.getOrPut(path) {
            val points = PointArrayList()
            path.emitPoints2 { x, y, move ->
                points.add(x, y)
            }
            points
        }

        val bb = BoundsBuilder()
        bb.reset()

        val data = FloatArray(points.size * 2 + 4)
        for (n in 0 until points.size + 1) {
            val x = points.getX(n % points.size).toFloat()
            val y = points.getY(n % points.size).toFloat()
            val tx = m.transformXf(x, y)
            val ty = m.transformYf(x, y)
            data[(n + 1) * 2 + 0] = tx
            data[(n + 1) * 2 + 1] = ty
            bb.add(tx, ty)
        }
        data[0] = ((bb.xmax + bb.xmin) / 2).toFloat()
        data[1] = ((bb.ymax + bb.ymin) / 2).toFloat()

        if (shape.path.winding != Winding.EVEN_ODD) {
            error("Currently only supported EVEN_ODD winding")
        }

        val bounds = bb.getBounds()

        ctx.dynamicVertexBufferPool { vertices ->
            vertices.upload(data)
            ctx.batch.updateStandardUniforms()

            ctx.batch.simulateBatchStats(points.size + 2)

            val scissor: AG.Scissor? = AG.Scissor().setTo(
                //bounds
                Rectangle.fromBounds(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toIntCeil(), bounds.bottom.toIntCeil())
            )
            //val scissor: AG.Scissor? = null

            ctx.ag.clearStencil(0, scissor = scissor)
            //ctx.ag.clearStencil(0, scissor = null)
            ctx.ag.draw(
                vertices = vertices,
                program = PROGRAM_STENCIL,
                type = AG.DrawType.TRIANGLE_FAN,
                vertexLayout = LAYOUT,
                vertexCount = points.size + 2,
                uniforms = ctx.batch.uniforms,
                stencil = AG.StencilState(
                    enabled = true,
                    readMask = 0xFF,
                    compareMode = AG.CompareMode.ALWAYS,
                    referenceValue = 0xFF,
                    writeMask = 0xFF,
                    actionOnDepthFail = AG.StencilOp.KEEP,
                    actionOnDepthPassStencilFail = AG.StencilOp.KEEP,
                    actionOnBothPass = AG.StencilOp.INVERT,
                ),
                blending = BlendMode.NONE.factors,
                colorMask = AG.ColorMaskState(false, false, false, false),
                scissor = scissor,
            )
        }
        renderFill(ctx, shape.paint, shape.transform, bounds)
    }

    private val colorUniforms = AG.UniformValues()
    private val bitmapUniforms = AG.UniformValues()
    private val gradientUniforms = AG.UniformValues()
    private val gradientBitmap = Bitmap32(256, 1)

    private val colorF = FloatArray(4)

    private fun renderFill(ctx: RenderContext, paint: Paint, transform: Matrix, bounds: Rectangle) {
        if (paint is NonePaint) return

        ctx.dynamicVertexBufferPool { vertices ->
            val data = FloatArray(4 * 4)
            var n = 0
            val x0 = bounds.left.toFloat()
            val y0 = bounds.top.toFloat()
            val x1 = bounds.right.toFloat()
            val y1 = bounds.bottom.toFloat()
            val w = x1 - x0
            val h = y1 - y0
            data[n++] = x0; data[n++] = y0; data[n++] = 0f; data[n++] = 0f
            data[n++] = x1; data[n++] = y0; data[n++] = w; data[n++] = 0f
            data[n++] = x1; data[n++] = y1; data[n++] = w; data[n++] = h
            data[n++] = x0; data[n++] = y1; data[n++] = 0f; data[n++] = h

            vertices.upload(data)
            ctx.useBatcher { batch ->
                batch.updateStandardUniforms()
                var uniforms: AG.UniformValues = colorUniforms
                var program: Program = PROGRAM_COLOR
                when (paint) {
                    is ColorPaint -> {
                        val color = paint
                        color.writeFloat(colorF)
                        colorUniforms[u_Color] = colorF
                        program = PROGRAM_COLOR
                        uniforms = colorUniforms
                    }
                    is BitmapPaint -> {
                        bitmapUniforms[DefaultShaders.u_Tex] = AG.TextureUnit(ctx.getTex(paint.bitmap).base)
                        val mat = (paint.transform * transform)
                        mat.scale(1.0 / paint.bitmap.width, 1.0 / paint.bitmap.height)
                        bitmapUniforms[u_Transform] = mat.toMatrix3D()
                        program = PROGRAM_BITMAP
                        uniforms = bitmapUniforms
                    }
                    is GradientPaint -> {
                        paint.fillColors(gradientBitmap.dataPremult)
                        gradientUniforms[DefaultShaders.u_Tex] = AG.TextureUnit(ctx.getTex(gradientBitmap).base)
                        val mat = paint.transform * transform * paint.gradientMatrixInv
                        gradientUniforms[u_Transform] = mat.toMatrix3D()
                        program = PROGRAM_LINEAR_GRADIENT
                        uniforms = gradientUniforms
                    }
                    else -> {
                        TODO("paint=$paint")
                    }
                }
                batch.setTemporalUniforms(uniforms) {
                    ctx.batch.simulateBatchStats(4)
                    ctx.ag.draw(
                        vertices = vertices,
                        program = program,
                        type = AG.DrawType.TRIANGLE_FAN,
                        vertexLayout = LAYOUT_FILL,
                        vertexCount = 4,
                        uniforms = ctx.batch.uniforms,
                        stencil = AG.StencilState(
                            enabled = true,
                            compareMode = AG.CompareMode.NOT_EQUAL,
                            writeMask = 0,
                        ),
                        blending = BlendMode.NORMAL.factors,
                        colorMask = AG.ColorMaskState(true, true, true, true),
                    )
                }
            }
        }
    }
}
