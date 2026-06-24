package org.korge.application

import korlibs.image.awt.toAwtNativeImage
import korlibs.image.format.readBitmapNative
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.scene.ScaledScene
import korlibs.korge.view.SContainer
import korlibs.korge.view.ScalingOption
import korlibs.korge.view.align.alignLeftToRightOf
import korlibs.korge.view.image
import korlibs.korge.view.scaleWhileMaintainingAspect
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

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
