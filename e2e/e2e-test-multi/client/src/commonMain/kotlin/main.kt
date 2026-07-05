import korlibs.image.color.Colors
import korlibs.korge.Korge
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.korge.view.SContainer
import korlibs.korge.view.text
import korlibs.math.geom.Size
import korlibs.samples.clientserver.mySharedString

suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]) {
    sceneContainer().changeTo { MyMainScene() }
}

class MyMainScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        text("Text: $mySharedString")
        gameWindow.close()
    }
}
