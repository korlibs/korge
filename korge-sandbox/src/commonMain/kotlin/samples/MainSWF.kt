package samples

import com.soywiz.korge.ext.swf.SWFExportConfig
import com.soywiz.korge.ext.swf.readSWF
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.position
import com.soywiz.korim.vector.ShapeRasterizerMethod
import com.soywiz.korio.file.std.resourcesVfs

class MainSWF : Scene() {
    override suspend fun SContainer.sceneMain() {
        //val rastMethod = ShapeRasterizerMethod.X4 // Fails on native
        val rastMethod = ShapeRasterizerMethod.NONE
        //val rastMethod = ShapeRasterizerMethod.X1
        //val native = false
        val native = true
        this += resourcesVfs["dog.swf"].readSWF(views, SWFExportConfig(rasterizerMethod = rastMethod, native = native), false).createMainTimeLine()
        this += resourcesVfs["test1.swf"].readSWF(views, SWFExportConfig(rasterizerMethod = rastMethod, native = native), false).createMainTimeLine().position(400, 0)
    }
}
