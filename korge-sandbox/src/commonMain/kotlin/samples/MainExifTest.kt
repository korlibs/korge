package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*

class MainExifTest : Scene() {
    override suspend fun Container.sceneMain() {
        //val file = localVfs("/tmp/Exif5-2x.avif")
        val file = resourcesVfs["Portrait_3.jpg"]
        //val info = resourcesVfs["IMG_5455.HEIC"].readImageInfo(HEICInfo)
        //val info = localVfs("/tmp/IMG_5455.HEIC").readImageInfo(HEICInfo, ImageDecodingProps(debug = true))
        //val info = localVfs("/tmp/Exif5-2x.avif").readImageInfo(HEICInfo, ImageDecodingProps(debug = true))
        val info = file.readBitmapInfo()
        image(file.readBitmapSliceWithOrientation())
            .scale(0.2)
            .filters(OldBlurFilter())
        //println(info)
    }
}
