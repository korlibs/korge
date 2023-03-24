package samples

import korlibs.korge.particle.*
import korlibs.korge.scene.Scene
import korlibs.korge.view.*
import korlibs.io.file.std.*
import korlibs.math.geom.*

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
                particlesView.emitterPos = localMousePos(views).mutable
            }

            //delay(4.seconds)

            println("RESTART")
            particlesView.restart()
        }
    }
}