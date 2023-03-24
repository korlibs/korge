package samples

import korlibs.korge.scene.ScaledScene
import korlibs.korge.view.SContainer
import korlibs.korge.view.image
import korlibs.korge.view.xy
import korlibs.image.color.Colors
import korlibs.image.format.PNG
import korlibs.image.format.readBitmap
import korlibs.io.file.std.resourcesVfs

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