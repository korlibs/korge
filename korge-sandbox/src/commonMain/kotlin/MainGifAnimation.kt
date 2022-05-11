import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.animation.imageAnimationView
import com.soywiz.korim.format.readImageData
import com.soywiz.korio.file.std.resourcesVfs

suspend fun Stage.mainGifAnimation() {
    val imageData = com.soywiz.korio.file.std.resourcesVfs["200.gif"].readImageData(com.soywiz.korim.format.GIF)
    imageAnimationView(imageData.defaultAnimation)
}
