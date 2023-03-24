package samples

import korlibs.korge.component.docking.dockedTo
import korlibs.korge.input.onClick
import korlibs.korge.scene.Scene
import korlibs.korge.ui.UIButtonToggleableGroup
import korlibs.korge.ui.group
import korlibs.korge.ui.tooltip
import korlibs.korge.ui.uiButton
import korlibs.korge.ui.uiGridFill
import korlibs.korge.ui.uiHorizontalStack
import korlibs.korge.ui.uiImage
import korlibs.korge.ui.uiTooltipContainer
import korlibs.korge.ui.uiVerticalStack
import korlibs.korge.view.*
import korlibs.image.bitmap.asumePremultiplied
import korlibs.image.bitmap.slice
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.std.resourcesVfs
import korlibs.math.geom.Anchor
import korlibs.math.geom.ScaleMode

class MainUIImageTester : Scene() {
    override suspend fun SContainer.sceneMain() {
        //val imageData = resourcesVfs["vampire.ase"].readBitmapImageData(ASE.toProps())
        ////val imageData = resourcesVfs["vampire_slices_fixed.ase"].readImageDataContainer(ASE.toProps())
        //image(imageData.mainBitmap).scale(8.0)
        //return

        solidRect(10, 10, Colors.RED).anchor(Anchor.TOP_LEFT).dockedTo(Anchor.TOP_LEFT)
        solidRect(10, 10, Colors.GREEN).anchor(Anchor.TOP_RIGHT).dockedTo(Anchor.TOP_RIGHT)
        solidRect(10, 10, Colors.BLUE).anchor(Anchor.BOTTOM_RIGHT).dockedTo(Anchor.BOTTOM_RIGHT)
        solidRect(10, 10, Colors.PURPLE).anchor(Anchor.BOTTOM_LEFT).dockedTo(Anchor.BOTTOM_LEFT)

        val korimPng = resourcesVfs["korim.png"].readBitmapSlice()
        val bunnysPng = resourcesVfs["bunnys.png"].readBitmapSlice()
        //val vampireAse = resourcesVfs["vampire.ase"].readBitmap(ASE).toBMP32().premultipliedIfRequired().slice()
        val vampireAse = resourcesVfs["vampire.ase"].readBitmap(ASE.toProps(ImageDecodingProps.DEFAULT_PREMULT)).slice()

        //println("vampireAse.premultiplied=${vampireAse.premultiplied}")

        val image = uiImage(300, 170, korimPng, scaleMode = ScaleMode.COVER, contentAnchor = Anchor.MIDDLE_CENTER).xy(200, 200)
        image.bgcolor = Colors["#17334f"]

        uiTooltipContainer { tooltips ->
            uiGridFill(100.0, 100.0, cols = 3, rows = 3) {
                val group = UIButtonToggleableGroup()
                for (y in 0 until 3) {
                    for (x in 0 until 3) {
                        uiButton("X") {
                            val anchor = Anchor(x * 0.5, y * 0.5)
                            tooltip(tooltips, anchor.toNamedString())
                            this.group(group, pressed = x == 1 && y == 1)
                            onClick { image.contentAnchor = anchor }
                        }
                    }
                }
            }
            uiVerticalStack {
                xy(200.0, 0.0)
                uiHorizontalStack {
                    val group = UIButtonToggleableGroup()
                    uiButton(label = "COVER").group(group, pressed = true).onClick { image.scaleMode = ScaleMode.COVER }
                    uiButton(label = "FIT").group(group).onClick { image.scaleMode = ScaleMode.FIT }
                    uiButton(label = "EXACT").group(group).onClick { image.scaleMode = ScaleMode.EXACT }
                    uiButton(label = "NO_SCALE").group(group).onClick { image.scaleMode = ScaleMode.NO_SCALE }
                }
                uiHorizontalStack {
                    val group = UIButtonToggleableGroup()
                    uiButton(label = "SQUARE").group(group).onClick { image.size(300, 300) }
                    uiButton(label = "HRECT").group(group, pressed = true).onClick { image.size(300, 170) }
                    uiButton(label = "VRECT").group(group).onClick { image.size(170, 300) }
                }
                uiHorizontalStack {
                    val group = UIButtonToggleableGroup()
                    uiButton(label = "korim.png").group(group, pressed = true).onClick { image.bitmap = korimPng }
                    uiButton(label = "bunnys.png").group(group).onClick { image.bitmap = bunnysPng }
                    uiButton(label = "vampire.ase").group(group).onClick { image.bitmap = vampireAse }
                }
            }
        }
    }
}