package samples

import com.soywiz.korge.particle.*
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.*
import com.soywiz.korio.file.std.*

class MainParticles : Scene() {
    override suspend fun SContainer.sceneMain() {
        //val emitter = resourcesVfs["particle/demo2.pex"].readParticleEmitter()
        container {
            //scale = 0.05
            scale = 0.5
            val emitter = resourcesVfs["particle/particle.pex"].readParticleEmitter()
            //val emitter = resourcesVfs["particle/particle.pex"].readParticleEmitter()
            //val emitter = resourcesVfs["particle/1/particle.pex"].readParticleEmitter()
            //val particlesView = particleEmitter(emitter, time = 2.seconds).position(views.virtualWidth * 0.5, views.virtualHeight * 0.5)
            val particlesView = particleEmitter(emitter, localCoords = false)//.position(views.virtualWidth * 0.5, views.virtualHeight * 0.5)
            //val particlesView = particleEmitter(emitter).position(0.0, 0.0)

            addUpdater {
                val localMouse = localMouseXY(views)
                val stageMouse = stage!!.mouseXY.copy()
                //println("localMouse=$localMouse, stageMouse=$stageMouse")
                particlesView.emitterPos = localMouseXY(views) * scale
                //particlesView.setGlobalXY(stage!!.mouseXY)
                //println(stage!!.mouseXY)
                //particlesView.x = stage.mouseX
                //particlesView.y = stage.mouseY
            }

            //delay(4.seconds)

            println("RESTART")
            particlesView.restart()
        }
    }
}
