
import com.soywiz.korge.input.DraggableCloseable
import com.soywiz.korge.input.draggableCloseable
import com.soywiz.korge.ui.uiCheckBox
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.solidRect

suspend fun Stage.mainDraggable() {
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
