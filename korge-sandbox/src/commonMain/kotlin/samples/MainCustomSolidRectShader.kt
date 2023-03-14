package samples

import com.soywiz.klock.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*

class MainCustomSolidRectShader : Scene() {
    object TimeUB : UniformBlock(fixedLocation = 6) {
        val timeUniform by float()
    }

    override suspend fun SContainer.sceneMain() {
        val solidRect = solidRect(200, 200, Colors.RED).xy(100, 100)

        var time = 0.seconds
        solidRect.updateProgramUniforms = {
            it[TimeUB].push {
                it[timeUniform] = time.seconds.toFloat()
            }
        }
        addUpdater {
            time += it
            invalidateRender()
        }

        solidRect.program = BatchBuilder2D.PROGRAM
            .replacingFragment("color") {
                DefaultShaders {
                    SET(out, vec4(1f.lit, v_Tex.x, v_Tex.y, 1f.lit))
                }
            }
            .appendingVertex("moving") {
                SET(out.x, out.x + (sin(TimeUB.timeUniform * 2f.lit) * .1f.lit))
            }
    }
}
