import com.soywiz.korge.*
import com.soywiz.korge.box2d.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korgw.*
import com.soywiz.korio.file.std.*

suspend fun main() = Korge(width = 920, height = 720, quality = GameWindow.Quality.PERFORMANCE, title = "My Awesome Box2D Game!") {
    registerBox2dSupportOnce()
    addChild(resourcesVfs["restitution.ktree"].readKTree(views))
}
