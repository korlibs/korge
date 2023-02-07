package samples

import com.soywiz.korge.scene.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.bitmap.ImageOrientation
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*

class MainImageOrientationTest : ScaledScene(512, 512) {
    override suspend fun SContainer.sceneMain() {
        val bmp = resourcesVfs["korge.png"].readBitmap()
        var orientation = ImageOrientation.ROTATE_0
        val image = image(bmp)
        fun updateOrientation(ori: ImageOrientation) {
            orientation = ori
            println("orientation=$orientation")
            image.bitmap = bmp.slice(orientation = ori)
        }

        uiVerticalStack {
            uiButton("FLIP HOR") { clicked { updateOrientation(orientation.flippedX()) } }
            uiButton("FLIP VER") { clicked { updateOrientation(orientation.flippedY()) } }
            uiButton("ROT LEFT") { clicked { updateOrientation(orientation.rotatedLeft()) } }
            uiButton("ROT RIGHT") { clicked { updateOrientation(orientation.rotatedRight()) } }
        }
    }
}
