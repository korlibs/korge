import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.format.*
import com.soywiz.korio.file.std.*

suspend fun main() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "SVG") {
	val svg = SVG(resourcesVfs["tiger.svg"].readString())
//	image(svg.render(native = false))
	image(svg.render(native = true))
}
