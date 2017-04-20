import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container

object MainTest {
	@JvmStatic fun main(args: Array<String>) = Korge(object : Module() {
		override val mainScene = TestMainScene::class.java
	})
}

class TestMainScene : Scene() {
	suspend override fun sceneInit(sceneView: Container) {
	}
}
