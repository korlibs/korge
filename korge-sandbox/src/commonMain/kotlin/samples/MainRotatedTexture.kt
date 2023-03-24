package samples

import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.image
import korlibs.image.format.readBitmapSlice
import korlibs.io.file.std.resourcesVfs
import korlibs.math.geom.MMatrix
import korlibs.math.geom.degrees
import korlibs.math.geom.slice.*

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