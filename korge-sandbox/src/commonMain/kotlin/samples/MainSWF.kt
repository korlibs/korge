package samples

import com.soywiz.korge.ext.swf.SWFExportConfig
import com.soywiz.korge.ext.swf.readSWF
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.position
import com.soywiz.korim.vector.ShapeRasterizerMethod
import com.soywiz.korio.file.std.resourcesVfs

class MainSWF : Scene() {
    override suspend fun Container.sceneMain() {
        this += resourcesVfs["dog.swf"].readSWF(views, SWFExportConfig(rasterizerMethod = ShapeRasterizerMethod.X4), false).createMainTimeLine()
        this += resourcesVfs["test1.swf"].readSWF(views, SWFExportConfig(rasterizerMethod = ShapeRasterizerMethod.X4), false).createMainTimeLine().position(400, 0)
    }
}
