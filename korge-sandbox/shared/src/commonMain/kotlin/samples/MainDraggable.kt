package samples
import korlibs.korge.input.DraggableCloseable
import korlibs.korge.input.draggableCloseable
import korlibs.korge.scene.Scene
import korlibs.korge.ui.uiCheckBox
import korlibs.korge.view.SContainer
import korlibs.korge.view.solidRect

class MainDraggable : Scene() {
    override suspend fun SContainer.sceneMain() {
        val draggableRect = solidRect(100, 100)

        var closeable: DraggableCloseable? = null

        uiCheckBox(text = "Toggle dragging") {
            onChange {
                if (it.checked) {
                    closeable = draggableRect.draggableCloseable()
                } else {
                    closeable?.close()
                }
            }
        }
    }
}
