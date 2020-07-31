import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.particle.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korio.file.std.*

suspend fun particlesMain() = Korge(width = 600, height = 600) {
    val emitter = resourcesVfs["particle/demo2.pex"].readParticleEmitter()
    //val emitter = resourcesVfs["particle/particle.pex"].readParticleEmitter()
    //val emitter = resourcesVfs["particle/1/particle.pex"].readParticleEmitter()
    val particlesView = particleEmitter(emitter, time = 2.seconds).position(views.virtualWidth * 0.5, views.virtualHeight * 0.5)

    delay(4.seconds)

    println("RESTART")
    particlesView.restart()
}
