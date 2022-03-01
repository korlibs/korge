import com.soywiz.klock.*
import com.soywiz.korag.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*

suspend fun Stage.mainBitmapTexId() {
    //val bitmap = resourcesVfs["korim.png"].readBitmap().toBMP32()
    val agGl = (views.ag as AGOpengl)
    val gl = agGl.gl
    val tex = agGl.GlTexture(gl, true).uploadAndBindEnsuring(Bitmap32(128, 128, Colors.RED))
    val tex2 = agGl.GlTexture(gl, true).uploadAndBindEnsuring(Bitmap32(128, 128, Colors.BLUE))

    timers.timeout(0.1.seconds) {
        val image = MyNativeImage(tex.nativeTexId)
        val img = image(image)
        timers.timeout(2.seconds) {
            image.lock {
                image.forcedTexId = tex2.nativeTexId
            }
            //img.bitmap = MyNativeImage(tex2.nativeTexId).slice()
            println("CHANGED")
            //image.forcedTexId = tex2.nativeTexId
        }
    }
}

class MyNativeImage(
    override var forcedTexId: Int = -1
) : NativeImage(128, 128, null, false) {
    override var forcedTexTarget: Int = -1

    override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        TODO("Not yet implemented")
    }

}
