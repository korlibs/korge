import com.soywiz.kmem.*
import com.soywiz.korge.*
import com.soywiz.korge.render.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korim.font.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.Rectangle

suspend fun main() = Korge(scaleMode = ScaleMode.NO_SCALE, scaleAnchor = Anchor.TOP_LEFT, clipBorders = false) {
    val font = DefaultTtfFont.toBitmapFont(16.0)
    uiSkin = UISkin {
        this.textFont = font
    }

    /*
    uiBreadCrumbArray("hello", "world") {
        onClickPath {
            println(it)
            gameWindow.showContextMenu {
                item("hello", action = {})
                separator()
                item("world", action = {})
            }
        }
    }

    //val component = injector.get<ViewsDebuggerComponent>()
    //ktreeEditorKorge(stage, component.actions, views, BaseKorgeFileToEdit(MemoryVfsMix(mapOf("test.ktree" to "<ktree></ktree>"))["test.ktree"]), { })

    val solidRect = solidRect(100, 100, Colors.RED).position(300, 300).anchor(Anchor.CENTER)
    //val grid = OrthographicGrid(20, 20)
    //renderableView() { grid.draw(ctx, 500.0, 500.0, globalMatrix) }



    uiWindow("Properties", 300.0, 100.0) {
        it.isCloseable = false
        it.container.mobileBehaviour = false
        it.container.overflowRate = 0.0
        uiVerticalStack(300.0) {
            uiText("Properties") { textColor = Colors.RED }
            uiPropertyNumberRow("Alpha", *UIEditableNumberPropsList(solidRect::alpha))
            uiPropertyNumberRow("Position", *UIEditableNumberPropsList(solidRect::x, solidRect::y, min = -1024.0, max = +1024.0, clamped = false))
            uiPropertyNumberRow("Size", *UIEditableNumberPropsList(solidRect::width, solidRect::height, min = -1024.0, max = +1024.0, clamped = false))
            uiPropertyNumberRow("Scale", *UIEditableNumberPropsList(solidRect::scaleX, solidRect::scaleY, min = -1.0, max = +1.0, clamped = false))
            uiPropertyNumberRow("Rotation", *UIEditableNumberPropsList(solidRect::rotationDeg, min = -360.0, max = +360.0, clamped = true))
            val skewProp = uiPropertyNumberRow("Skew", *UIEditableNumberPropsList(solidRect::skewXDeg, solidRect::skewYDeg, min = -360.0, max = +360.0, clamped = true))
            append(UIPropertyRow("Visible")) {
                this.container.append(uiCheckBox(checked = solidRect.visible, text = "").also {
                    it.onChange {
                        solidRect.visible = it.checked
                    }
                })
            }

            println(skewProp.getVisibleGlobalArea())

        }
    }
    */

    uiScrollable {
        uiVerticalList(object : UIVerticalList.Provider {
            override val numItems: Int = 1000
            override val fixedHeight: Double = 20.0
            override fun getItemHeight(index: Int): Double = fixedHeight
            override fun getItemView(index: Int): View = UIText("HELLO WORLD $index")
        })
    }
}

private var View.rotationDeg: Double
    get() = rotation.degrees
    set(value) { rotation = value.degrees }

private var View.skewXDeg: Double
    get() = skewX.degrees
    set(value) { skewX = value.degrees }

private var View.skewYDeg: Double
    get() = skewY.degrees
    set(value) { skewY = value.degrees }
