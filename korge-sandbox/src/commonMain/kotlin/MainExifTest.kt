import com.soywiz.korge.view.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*

suspend fun Stage.mainExifTest() {
    //val info = resourcesVfs["IMG_5455.HEIC"].readImageInfo(HEICInfo)
    //val info = localVfs("/tmp/IMG_5455.HEIC").readImageInfo(HEICInfo, ImageDecodingProps(debug = true))
    val info = localVfs("/tmp/Exif5-2x.avif").readImageInfo(HEICInfo, ImageDecodingProps(debug = true))
    println(info)
}

