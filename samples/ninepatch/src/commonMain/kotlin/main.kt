import com.soywiz.kmem.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.std.*

suspend fun main() = Korge(width = 512, height = 512, bgcolor = Colors["#2b2b2b"]) {
	val ninePath = resourcesVfs["image.9.png"].readNinePatch()

	val np = ninePatch(ninePath, 320.0, 32.0) {
		position(100, 100)
	}
	np.mouse {
		moveAnywhere {
			np.width = it.currentPosLocal.x.clamp(16.0, 1000.0)
			np.height = it.currentPosLocal.y.clamp(16.0, 1000.0)
		}
	}
}
