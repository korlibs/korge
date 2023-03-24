package samples

import korlibs.korge.scene.Scene
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.image.format.*
import korlibs.io.file.std.*

class MainExifTest : Scene() {
    override suspend fun SContainer.sceneMain() {
        //val file = localVfs("/tmp/Exif5-2x.avif")
        val file = resourcesVfs["Portrait_3.jpg"]
        //val info = resourcesVfs["IMG_5455.HEIC"].readImageInfo(HEICInfo)
        //val info = localVfs("/tmp/IMG_5455.HEIC").readImageInfo(HEICInfo, ImageDecodingProps(debug = true))
        //val info = localVfs("/tmp/Exif5-2x.avif").readImageInfo(HEICInfo, ImageDecodingProps(debug = true))
        val info = file.readBitmapInfo()
        image(file.readBitmapSliceWithOrientation())
            .scale(0.2)
            .filters(BlurFilter())
        //println(info)
    }
}