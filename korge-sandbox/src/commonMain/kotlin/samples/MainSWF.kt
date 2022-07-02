package samples

import com.soywiz.korge.ext.swf.SWFExportConfig
import com.soywiz.korge.ext.swf.readSWF
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.GraphicsRenderer
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.position
import com.soywiz.korim.vector.ShapeRasterizerMethod
import com.soywiz.korio.file.std.resourcesVfs

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
    }
}
