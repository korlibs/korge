package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.image
import com.soywiz.korim.bitmap.flippedX
import com.soywiz.korim.bitmap.rotatedRight
import com.soywiz.korim.bitmap.transformed
import com.soywiz.korim.format.readBitmapSlice
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.degrees

class MainRotatedTexture : Scene() {
    override suspend fun Container.sceneMain() {
        //val tex = resourcesVfs["korim.png"].readBitmapSlice().rotateRight()
        //val tex = resourcesVfs["korim.png"].readBitmapSlice().flipY()
        //val tex = resourcesVfs["korim.png"].readBitmapSlice().transformed(Matrix().scale(.5f, .5f)).sliceWithSize(0, 0, 10, 10)
        //val tex = resourcesVfs["korim.png"].readBitmapSlice().transformed(Matrix().scale(.5f, .5f))
        val tex = resourcesVfs["korim.png"].readBitmapSlice().transformed(Matrix().skew(30.degrees, 0.degrees)).flippedX().rotatedRight()
        println("tex=$tex")
        println("size=${tex.width},${tex.height}")
        image(tex)
    }
}
