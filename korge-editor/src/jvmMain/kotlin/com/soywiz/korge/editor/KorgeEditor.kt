import com.soywiz.korge.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.Container
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korma.geom.*
import java.awt.*

suspend fun main() = Korge(scaleMode = ScaleMode.NO_SCALE, scaleAnchor = Anchor.TOP_LEFT, clipBorders = false) {
    //val component = injector.get<ViewsDebuggerComponent>()
    //ktreeEditorKorge(stage, component.actions, views, BaseKorgeFileToEdit(MemoryVfsMix(mapOf("test.ktree" to "<ktree></ktree>"))["test.ktree"]), { })

    val font = DefaultTtfFont.toBitmapFont(16.0)

    val solidRect = solidRect(100, 100, Colors.RED).position(300, 300)

    uiSkin = UISkin {
        this.textFont = font
    }
    uiNewScrollable(300.0, 100.0) {
        it.mobileBehaviour = false
        it.overflowRate = 0.0
        uiVerticalStack(300.0) {
            uiText("Properties") { textColor = Colors.RED }
            uiPropertyNumberRow("Alpha", *UIEditableNumberPropsList(solidRect::alpha))
            uiPropertyNumberRow("Position", *UIEditableNumberPropsList(solidRect::x, solidRect::y, min = -1024.0, max = +1024.0, clamped = false))
            uiPropertyNumberRow("Size", *UIEditableNumberPropsList(solidRect::width, solidRect::height, min = -1024.0, max = +1024.0, clamped = false))
            uiPropertyNumberRow("Scale", *UIEditableNumberPropsList(solidRect::scaleX, solidRect::scaleY, min = -1.0, max = +1.0, clamped = false))
            uiPropertyNumberRow("Rotation", *UIEditableNumberPropsList(solidRect::rotationDeg, min = -360.0, max = +360.0, clamped = true))
        }
    }
}

private var View.rotationDeg: Double
    get() = rotation.degrees
    set(value) { rotation = value.degrees }
