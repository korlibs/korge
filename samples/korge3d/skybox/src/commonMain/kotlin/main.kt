import com.soywiz.korev.Key
import com.soywiz.korge.Korge
import com.soywiz.korge.input.keys
import com.soywiz.korge3d.Korge3DExperimental
import com.soywiz.korge3d.cubeMapFromResourceDirectory
import com.soywiz.korge3d.scene3D
import com.soywiz.korge3d.skyBox
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.times

@Korge3DExperimental
suspend fun main() = Korge {

    scene3D {
        val stage3D = this
        skyBox(cubeMapFromResourceDirectory("skybox", "jpg"))

        keys {
            val angleSpeed = 1.degrees
            val fast = 5
            down(Key.UP) {
                val mul = when {
                    it.shift -> fast
                    else -> 1
                }
                stage3D.camera.pitchDown(angleSpeed.times(mul), 1f)
            }
            down(Key.DOWN) {
                val mul = when {
                    it.shift -> fast
                    else -> 1
                }
                stage3D.camera.pitchUp(angleSpeed.times(mul), 1f)
            }
            down(Key.RIGHT) {
                val mul = when {
                    it.shift -> fast
                    else -> 1
                }
                stage3D.camera.yawRight(angleSpeed.times(mul), 1f)
            }
            down(Key.LEFT) {
                val mul = when {
                    it.shift -> fast
                    else -> 1
                }
                stage3D.camera.yawLeft(angleSpeed.times(mul), 1f)
            }
        }
    }

}
