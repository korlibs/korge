package samples

import com.soywiz.korge.input.mouse
import com.soywiz.korge.resources.resourceBitmap
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.view.Circle
import com.soywiz.korge.view.MaskedView
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.centered
import com.soywiz.korge.view.image
import com.soywiz.korge.view.position
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.text
import com.soywiz.korim.bitmap.effect.BitmapEffect
import com.soywiz.korim.bitmap.effect.applyEffect
import com.soywiz.korim.color.Colors
import com.soywiz.korio.resources.ResourcesContainer

class MainOldMask : ScaledScene(512, 512) {
    val ResourcesContainer.korgePng by resourceBitmap("korge.png")

    override suspend fun SContainer.sceneMain() {
        val bitmap = korgePng.get().extract().toBMP32()
        val bitmap2 = bitmap.applyEffect(BitmapEffect(dropShadowColor = Colors.BLUE, dropShadowX = 10, dropShadowY = 0, dropShadowRadius = 6))
        val maskedView = MaskedView()
        addChild(maskedView)
        maskedView.text("HELLO WORLD!", textSize = 64.0)
        val img = maskedView.image(bitmap2).scale(3, 3)
        val mask = Circle(256.0).centered
        maskedView.mask = mask
        maskedView.mask!!.position(0, 0)
        mouse {
            onMoveAnywhere {
                maskedView.mask!!.position(localMouseX(views).coerceIn(0.0, 512.0), localMouseY(views).coerceIn(0.0, 512.0))
            }
        }
    }
}
