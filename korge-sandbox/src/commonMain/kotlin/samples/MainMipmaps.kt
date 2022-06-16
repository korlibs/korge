package samples

import com.soywiz.klock.*
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*

class MainMipmaps : Scene() {
    override suspend fun Container.sceneMain() {
        val image = image(resourcesVfs["korge.png"].readBitmap().mipmaps())
        while (true) {
            tween(image::scale[0.01], time = 3.seconds)
            tween(image::scale[0.2], time = 1.seconds)
        }
    }
}
