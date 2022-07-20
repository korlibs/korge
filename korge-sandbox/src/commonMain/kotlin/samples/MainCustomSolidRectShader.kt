package samples

import com.soywiz.klock.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*

class MainCustomSolidRectShader : Scene() {
    override suspend fun SContainer.sceneMain() {
        val solidRect = solidRect(200, 200, Colors.RED).xy(100, 100)
        val timeUniform = Uniform("u_time", VarType.Float1)

        var time = 0.seconds
        addUpdater {
            solidRect.programUniforms[timeUniform] = time.seconds.toFloat()
            time += it
        }

        solidRect.program = views.getDefaultProgram()
            .replacingFragment("color") {
                DefaultShaders {
                    SET(out, vec4(1f.lit, v_Tex.x, v_Tex.y, 1f.lit))
                }
            }
            .appendingVertex("moving") {
                SET(out.x, out.x + (sin(timeUniform * 2f.lit) * .1f.lit))
            }
    }
}
