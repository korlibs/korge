package samples

import com.soywiz.korev.DropFileEvent
import com.soywiz.korge.ext.swf.SWFExportConfig
import com.soywiz.korge.ext.swf.readSWF
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.UI_DEFAULT_WIDTH
import com.soywiz.korge.ui.clicked
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.ui.uiComboBox
import com.soywiz.korge.view.GraphicsRenderer
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.container
import com.soywiz.korge.view.position
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.xy
import com.soywiz.korgw.FileFilter
import com.soywiz.korgw.onDragAndDropFileEvent
import com.soywiz.korgw.openFileDialog
import com.soywiz.korim.vector.ShapeRasterizerMethod
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.Vfs
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.korma.geom.applyScaleMode

class MainSWF : Scene() {
    //val rastMethod = ShapeRasterizerMethod.X4 // Fails on native
    val rastMethod = ShapeRasterizerMethod.NONE
    //val rastMethod = ShapeRasterizerMethod.X1
    val graphicsRenderer = GraphicsRenderer.SYSTEM
    //val graphicsRenderer = GraphicsRenderer.GPU

    val config = SWFExportConfig(
        rasterizerMethod = rastMethod,
        generateTextures = false,
        //generateTextures = true,
        graphicsRenderer = graphicsRenderer,
    )

    override suspend fun SContainer.sceneMain() {
        this += resourcesVfs["morph.swf"].readSWF(views, config, false).createMainTimeLine()
        this += resourcesVfs["dog.swf"].readSWF(views, config, false).createMainTimeLine()
        demo()
    }

    suspend fun SContainer.demo() {
        this += resourcesVfs["test1.swf"].readSWF(views, config, false).createMainTimeLine().position(400, 0)
        this += resourcesVfs["demo3.swf"].readSWF(views, config, false).createMainTimeLine()
        val extraSwfContainer = container {
        }

        fun loadSwf(files: List<VfsFile>) {
            if (files.isEmpty()) return
            launchImmediately {
                extraSwfContainer.removeChildren()
                for (file in files) {
                    val swf = file.readSWF(views, config, false)
                    val timeline = swf.createMainTimeLine()
                    extraSwfContainer += timeline
                    val realBounds = Rectangle(0, 0, swf.width, swf.height).applyScaleMode(this@demo.getLocalBounds(), ScaleMode.FIT, Anchor.CENTER)
                    //timeline.xy(realBounds.x, realBounds.y).scale(realBounds.width / swf.width, realBounds.height / swf.height)
                    //println("realBounds=$realBounds")
                    timeline.xy(realBounds.x, realBounds.y).setSizeScaled(realBounds.width, realBounds.height)
                    extraSwfContainer.uiComboBox(items = timeline.stateNames).also {
                        it.onSelectionUpdate {
                            it.selectedItem?.let { timeline.play(it) }
                        }
                    }
                    //println("swf=${swf.width}x${swf.height}, timeline.getLocalBounds()=${timeline.getLocalBounds()}, this@demo.getLocalBounds()=${this@demo.getLocalBounds()}, realBounds=$realBounds")
                }
            }
        }

        uiButton("Load or drag SWF...", width = UI_DEFAULT_WIDTH * 2)
            .xy(510, 0)
            .clicked {
                launchImmediately {
                    val files = gameWindow.openFileDialog(filter = FileFilter("SWF files" to listOf("*.swf")), write = false, multi = false)
                    loadSwf(files)
                }
            }

        this.addOnEvent<DropFileEvent> {
            println("DropFileEvent: $it")
            if (it.type == DropFileEvent.Type.DROP) {
                val files = it.files ?: return@addOnEvent
                loadSwf(files)
            }
        }
        //this += localVfs("/tmp/5.swf").readSWF(views, config, false).createMainTimeLine().scale(4.0)
    }
}
