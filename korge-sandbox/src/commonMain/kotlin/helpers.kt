@file:OptIn(ExperimentalStdlibApi::class)

import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import korlibs.math.geom.*

class Demo(val sceneBuilder: () -> Scene, val name: String = sceneBuilder()::class.portableSimpleName.removePrefix("Main")) {
    override fun toString(): String = name
}
suspend fun Stage.demoSelector(default: Demo, all: List<Demo>) {
    val container = sceneContainer(size = Size(width, height - 48f)) { }.xy(0, 48)
    val containerFocus = container.makeFocusable()

    lateinit var comboBox: UIComboBox<Demo>

    suspend fun setDemo(demo: Demo?) {
        //container.removeChildren()
        if (demo != null) {
            comboBox.selectedItem = demo
            views.clearColor = DEFAULT_KORGE_BG_COLOR
            container.changeTo {
                containerFocus.focus()
                demo.sceneBuilder().also { it.init(this) }
            }
        }
    }

    uiHorizontalStack(padding = 8f) {
        alignLeftToLeftOf(this@demoSelector.stage, padding = 8.0).alignTopToTopOf(this@demoSelector.stage, padding = 8.0)
        comboBox = uiComboBox<Demo>(size = UI_DEFAULT_SIZE.copy(width = 200f), items = (listOf(default) + all).distinctBy { it.name }.sortedBy { it.name }) {
            this.viewportHeight = 600
            this.onSelectionUpdate.add {
                //println(it)
                launchImmediately { setDemo(it.selectedItem!!) }
            }
        }
        uiCheckBox(size = UI_DEFAULT_SIZE.copy(width = 200f), text = "forceRenderEveryFrame", checked = views.forceRenderEveryFrame) {
            onChange { views.forceRenderEveryFrame = it.checked }
        }
        uiCheckBox(size = UI_DEFAULT_SIZE.copy(width = 150f), text = "toggleDebug", checked = views.debugViews) {
            onChange { views.debugViews = it.checked }
        }
    }
    comboBox.selectedItem = default
    comboBox.focusNoOpen()
    setDemo(default)
}
