package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.image
import com.soywiz.korim.format.readBitmapSlice
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.MMatrix
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.slice.*

class MainRotatedTexture : Scene() {
    override suspend fun SContainer.sceneMain() {
        //val tex = resourcesVfs["korim.png"].readBitmapSlice().rotateRight()
        //val tex = resourcesVfs["korim.png"].readBitmapSlice().flipY()
        //val tex = resourcesVfs["korim.png"].readBitmapSlice().transformed(Matrix().scale(.5f, .5f)).sliceWithSize(0, 0, 10, 10)
        //val tex = resourcesVfs["korim.png"].readBitmapSlice().transformed(Matrix().scale(.5f, .5f))
        val tex = resourcesVfs["korim.png"].readBitmapSlice().transformed(MMatrix().skew(30.degrees, 0.degrees)).flippedX().rotatedRight()
        println("tex=$tex")
        println("size=${tex.width},${tex.height}")
        image(tex)
    }
}
