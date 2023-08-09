@file:OptIn(ExperimentalStdlibApi::class)

package samples
import korlibs.korge.input.DraggableAutoCloseable
import korlibs.korge.input.draggableCloseable
import korlibs.korge.scene.Scene
import korlibs.korge.ui.uiCheckBox
import korlibs.korge.view.SContainer
import korlibs.korge.view.solidRect

class MainDraggable : Scene() {
    override suspend fun SContainer.sceneMain() {
        val draggableRect = solidRect(100, 100)

        var closeable: DraggableAutoCloseable? = null

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
