package samples

import com.soywiz.korag.FragmentShaderDefault
import com.soywiz.korag.ProgramBuilderDefault
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.Uniform
import com.soywiz.korag.shader.VarType
import com.soywiz.korge.render.BatchBuilder2D
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.RectBase
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.skew
import com.soywiz.korge.view.vector.GpuShapeViewPrograms
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.degrees

class MainSDF : Scene() {
    override suspend fun SContainer.sceneMain() {
        addChild(
            CircleSDFView(width = 400.0, height = 400.0)
                .skew(45.degrees, 0.degrees)
                .also {
                    //it.radius = 0.3
                })
    }
}

open class CircleSDFView(width: Double = 100.0, height: Double = 100.0) : ShadedView(PROGRAM, width, height) {
    var radius = 0.49
    var feather = 0.005
    var center = Point(0.5, 0.5)

    override fun renderInternal(ctx: RenderContext) {
        this.programUniforms[u_Center] = center
        this.programUniforms[u_Radius] = radius
        this.programUniforms[u_Feather] = feather

        super.renderInternal(ctx)
    }

    companion object {
        val u_Center = Uniform("u_Center", VarType.Float2)
        val u_Radius = Uniform("u_Radius", VarType.Float1)
        val u_Feather = Uniform("u_Feather", VarType.Float1)
        val PROGRAM = buildShader {
            val dist = t_Temp0.x
            SET(dist, smoothstep(u_Radius, u_Radius + u_Feather, length(v_Tex - u_Center)))
            SET(out, vec4(v_Col["rgb"], 1f.lit - dist))
        }
    }
}

open class ShadedView(program: Program, width: Double = 100.0, height: Double = 100.0) : RectBase(0.0, 0.0) {
    override var width: Double = width; set(v) { field = v; dirtyVertices = true }
    override var height: Double = height; set(v) { field = v; dirtyVertices = true }

    override val bwidth: Double get() = width
    override val bheight: Double get() = height

    init {
        this.program = program
    }

    companion object {
        inline fun buildShader(callback: ProgramBuilderDefault.() -> Unit): Program {
            return BatchBuilder2D.PROGRAM.copy(fragment = FragmentShaderDefault {
                callback()
                BatchBuilder2D.DO_OUTPUT_FROM(this, out)
            })
        }
    }
}
