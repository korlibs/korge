package samples

import korlibs.time.*
import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.scene.Scene
import korlibs.korge.view.*
import korlibs.image.color.*

class MainCustomSolidRectShader : Scene() {
    object TimeUB : UniformBlock(fixedLocation = 6) {
        val timeUniform by float()
    }

    override suspend fun SContainer.sceneMain() {
        val solidRect = solidRect(200, 200, Colors.RED).xy(100, 100)

        var time = 0.fastSeconds
        solidRect.updateProgramUniforms = {
            it[TimeUB].push {
                it[timeUniform] = time.seconds.toFloat()
            }
        }
        addFastUpdater {
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
