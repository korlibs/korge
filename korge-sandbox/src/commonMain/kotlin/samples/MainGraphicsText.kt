package samples

import com.soywiz.korge.KeepOnReload
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.addDebugExtraComponent
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.filter.DropshadowFilter
import com.soywiz.korge.view.filter.IdentityFilter
import com.soywiz.korge.view.filter.filters
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.image
import com.soywiz.korge.view.text
import com.soywiz.korge.view.xy
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.paint.Stroke
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.text.text
import com.soywiz.korio.async.runBlockingNoJs
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.rect
import kotlin.properties.Delegates

class MainGraphicsText : Scene() {
    /** Property delegate to trigger a refresh on change */
    protected fun <T> refreshable(initial: T, refresh: () -> Unit = { sceneContainer.changeToAsync(this::class) }) = Delegates.observable(initial) { prop, old, new -> refresh() }

    @KeepOnReload var align by refreshable(TextAlignment.MIDDLE_CENTER) { this@MainGraphicsText.sceneView.sceneMainSync() }

    override suspend fun SContainer.sceneMain() {
        addDebugExtraComponent("Debug") {
            uiEditableValue(::align, values = TextAlignment.ALL)
        }
        views.debugHightlightView(this)
        sceneMainSync()
    }

    fun SContainer.sceneMainSync() {
        removeChildren()

        graphics {
            fillStroke(Colors.WHITE, Stroke(Colors.RED, thickness = 5.0)) {
                //rect(0, 0, 100, 100)
                circle(0, 0, 50)
                //text("Hello World!", font = DefaultTtfFont, textSize = 128.0)
            }
            //drawText("hello", font = DefaultTtfFont, x = 50.0)
        }
            //.filters(IdentityFilter)
            .filters(DropshadowFilter(dropX = 200.0))
            .xy(200, 200)
        /*
        graphics { fill(Colors.RED) { circle(0, 0, 10) } }.xy(100, 100)
        //text("hello world!", font = DefaultTtfFont, textSize = 64.0).xy(100, 100)
        text("¡jhello\nA\nWorld", font = DefaultTtfFont, textSize = 64.0, alignment = align).xy(100, 100)
        graphics {
            fill(Colors.RED) {
                circle(0, 0, 10)
            }
            //this.drawText("¡hello!", 100.0, 200.0, font = DefaultTtfFont, fill = false)

            fill(Colors.WHITE) {
                this.text("¡jhello!\nA\nWorld", DefaultTtfFont, textSize = 64.0, align = align)
            }
            drawText("¡jhello!\nA\nWorld", font = DefaultTtfFont, x = 0.0, y = 0.0, size = 64.0, align = align, paint = Colors.WHITE)
        }.xy(100, 400)

         */
    }
}
