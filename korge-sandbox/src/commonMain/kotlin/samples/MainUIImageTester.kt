package samples

import com.soywiz.korge.component.docking.dockedTo
import com.soywiz.korge.input.onClick
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.UIButtonToggleableGroup
import com.soywiz.korge.ui.group
import com.soywiz.korge.ui.tooltip
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.ui.uiGridFill
import com.soywiz.korge.ui.uiHorizontalStack
import com.soywiz.korge.ui.uiImage
import com.soywiz.korge.ui.uiTooltipContainer
import com.soywiz.korge.ui.uiVerticalStack
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.size
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.xy
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.ASE
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.format.readBitmapSlice
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ScaleMode

class MainUIImageTester : Scene() {
    override suspend fun SContainer.sceneMain() {
        solidRect(10, 10, Colors.RED).anchor(Anchor.TOP_LEFT).dockedTo(Anchor.TOP_LEFT)
        solidRect(10, 10, Colors.GREEN).anchor(Anchor.TOP_RIGHT).dockedTo(Anchor.TOP_RIGHT)
        solidRect(10, 10, Colors.BLUE).anchor(Anchor.BOTTOM_RIGHT).dockedTo(Anchor.BOTTOM_RIGHT)
        solidRect(10, 10, Colors.PURPLE).anchor(Anchor.BOTTOM_LEFT).dockedTo(Anchor.BOTTOM_LEFT)

        val korimPng = resourcesVfs["korim.png"].readBitmapSlice()
        val bunnysPng = resourcesVfs["bunnys.png"].readBitmapSlice()
        val vampireAse = resourcesVfs["vampire.ase"].readBitmap(ASE).slice()

        val image = uiImage(300, 170, korimPng, scaleMode = ScaleMode.COVER, contentAnchor = Anchor.MIDDLE_CENTER).xy(200, 200)
        image.bgcolor = Colors["#17334f"]

        uiTooltipContainer { tooltips ->
            uiGridFill(100.0, 100.0, cols = 3, rows = 3) {
                val group = UIButtonToggleableGroup()
                for (y in 0 until 3) {
                    for (x in 0 until 3) {
                        uiButton(text = "X") {
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
                    uiButton(text = "COVER").group(group, pressed = true).onClick { image.scaleMode = ScaleMode.COVER }
                    uiButton(text = "FIT").group(group).onClick { image.scaleMode = ScaleMode.FIT }
                    uiButton(text = "EXACT").group(group).onClick { image.scaleMode = ScaleMode.EXACT }
                    uiButton(text = "NO_SCALE").group(group).onClick { image.scaleMode = ScaleMode.NO_SCALE }
                }
                uiHorizontalStack {
                    val group = UIButtonToggleableGroup()
                    uiButton(text = "SQUARE").group(group).onClick { image.size(300, 300) }
                    uiButton(text = "HRECT").group(group, pressed = true).onClick { image.size(300, 170) }
                    uiButton(text = "VRECT").group(group).onClick { image.size(170, 300) }
                }
                uiHorizontalStack {
                    val group = UIButtonToggleableGroup()
                    uiButton(text = "korim.png").group(group, pressed = true).onClick { image.bitmap = korimPng }
                    uiButton(text = "bunnys.png").group(group).onClick { image.bitmap = bunnysPng }
                    uiButton(text = "vampire.ase").group(group).onClick { image.bitmap = vampireAse }
                }
            }
        }
    }
}
