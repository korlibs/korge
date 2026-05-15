import java.awt.image.*
import javax.imageio.*
import korlibs.image.awt.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*

class MainRenderImagesJvmNative : ScaledScene(512, 512) {
  override suspend fun SContainer.sceneMain() {

    val bytes = resourcesVfs["img1_grayscale.jpg"].readBytes()
    val inputStream = bytes.inputStream()
    val img: BufferedImage = ImageIO.read(inputStream)
    val bmp1 = img.toAwtNativeImage()
    val img1 =
        image(bmp1) {
          scaleWhileMaintainingAspect(ScalingOption.ByWidthAndHeight(512.0 / 2, 512.0))
        }

    val bitmap = resourcesVfs["img1_grayscale.jpg"].readBitmapNative()
    image(bitmap) {
      scaleWhileMaintainingAspect(ScalingOption.ByWidthAndHeight(512.0 / 2, 512.0))
      alignLeftToRightOf(img1)
    }
  }
}
