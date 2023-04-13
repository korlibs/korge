package samples

import korlibs.image.font.*
import korlibs.io.async.*
import korlibs.io.async.delay
import korlibs.io.file.std.*
import korlibs.korge.scene.Scene
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import korlibs.korge.view.filter.*
import korlibs.time.*

class MainBmpFont : Scene() {
    override suspend fun SContainer.sceneMain() {
        val font1 = resourcesVfs["font1.fnt"].readBitmapFont()
        val segment7 = resourcesVfs["segment7.fnt"].readBitmapFont() // mono spaced
        val text1 = text("Hello World!", textSize = 96f, font = font1)
        val text2 = text("Hello : World! jg", textSize = 96f, font = font1) {
            smoothing = false
            alignTopToBottomOf(text1)
        }
        val text3 = text("Hello World!", textSize = 96f, font = font1) {
            filter = Convolute3Filter(Convolute3Filter.KERNEL_GAUSSIAN_BLUR)
            alignTopToBottomOf(text2)
        }
        text("Hello World 2!", textSize = 32f, font = font1) {
            val text = this
            launchImmediately {
                var n = 0
                while (true) {
                    text.text = "Hello World! ${n++}"
                    centerOn(root)
                    delay(1.milliseconds)
                }
            }
        }
        text("42:10", textSize = 64f, font = segment7) {
            val text = this
            alignX(root, 0.5, true)
            alignY(root, 0.75, true)
            launchImmediately {
                var count = 10
                while (true) {
                    text.text = "42:${count++}"
                    delay(1.seconds)
                }
            }
        }
    }
}
