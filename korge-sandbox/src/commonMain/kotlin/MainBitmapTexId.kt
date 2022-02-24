import com.soywiz.korag.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*

suspend fun Stage.mainBitmapTexId() {
    val bitmap = resourcesVfs["korim.png"].readBitmap().toBMP32()

    /*
    val agGl = (views.ag as AGOpengl)
    val tex = agGl.GlTexture(agGl.gl, true)
    tex.upload(bitmap)
    val image = MyNativeImage()
    image.forcedTexId = tex.texId
    image(image)
    */
    image(bitmap)
        .filters(BlurFilter())
        .filters(ColorMatrixFilter(ColorMatrixFilter.SEPIA_MATRIX))
}

class MyNativeImage : NativeImage(128, 128, null, false) {
    override var forcedTexId: Int = -1
    override var forcedTexTarget: Int = -1

    override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        TODO("Not yet implemented")
    }

}
