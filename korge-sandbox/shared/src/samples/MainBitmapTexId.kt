package samples

import korlibs.time.*
import korlibs.graphics.gl.*
import korlibs.korge.scene.Scene
import korlibs.korge.time.*
import korlibs.korge.view.*
import korlibs.image.bitmap.*
import korlibs.image.color.*

class MainBitmapTexId : Scene() {
    /*
    override suspend fun Container.sceneMain() {
        //val bitmap = resourcesVfs["korim.png"].readBitmap().toBMP32()
        val agGl = (views.ag as AGOpengl)
        val gl = agGl.gl
        val tex = agGl.Texture(gl, true).uploadAndBindEnsuring(Bitmap32(128, 128, Colors.RED))
        val tex2 = agGl.Texture(gl, true).uploadAndBindEnsuring(Bitmap32(128, 128, Colors.BLUE))

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
    ) : ForcedTexNativeImage(128, 128, false) {
        override var forcedTexTarget: Int = -1
    }
     */
    override suspend fun SContainer.sceneMain() {
        TODO("Not yet implemented")
    }
}
