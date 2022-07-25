package samples

import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.image
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs

class MainBlending : ScaledScene(1024, 512) {
    override suspend fun SContainer.sceneMain() {
        views.clearColor = Colors.WHITE
        val bmp = resourcesVfs["7f7f7f.png"].readBitmap(PNG)
        //val bmp = resourcesVfs["7f7f7f.png"].readBitmapNoNative(PNG)
        println("premultiplied=${bmp.premultiplied}")
        println("col1=${bmp.getRgbaRaw(0, 0)}")
        println("col2=${bmp.getRgbaRaw(256, 0)}")

        image(bmp).xy(0, 256)
    }
}
