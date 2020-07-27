import com.soywiz.korge.*
import com.soywiz.korge.particle.*
import com.soywiz.korge.view.*
import com.soywiz.korio.file.std.*

suspend fun main() = Korge(width = 300, height = 300) {
	val emitter = resourcesVfs["particle/particle.pex"].readParticle()
	particleEmitter(emitter).position(views.virtualWidth * 0.5, views.virtualHeight * 0.75)
}
