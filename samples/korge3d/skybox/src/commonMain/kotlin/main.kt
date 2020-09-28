import com.soywiz.korev.*
import com.soywiz.korge.Korge
import com.soywiz.korge.input.keys
import com.soywiz.korge3d.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.times

@Korge3DExperimental
suspend fun main() = Korge {

    scene3D {
        val stage3D = this
        skyBox(resourcesVfs["skybox"].readCubeMap("jpg"))

        keys {
            val angleSpeed = 1.degrees
            downFrame(Key.UP) { stage3D.camera.pitchDown(angleSpeed * it.speed, 1f) }
            downFrame(Key.DOWN) { stage3D.camera.pitchUp(angleSpeed * it.speed, 1f) }
            downFrame(Key.RIGHT) { stage3D.camera.yawRight(angleSpeed * it.speed, 1f) }
            downFrame(Key.LEFT) { stage3D.camera.yawLeft(angleSpeed * it.speed, 1f) }
        }
    }

}

private val KeyEvent.speed: Double get() = if (shift) 5.0 else 1.0
