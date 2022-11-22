package samples

import com.soywiz.korge.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.text.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.vector.*
import kotlin.properties.*

class MainGraphicsText : Scene() {
    /** Property delegate to trigger a refresh on change */
    protected fun <T> refreshable(initial: T, refresh: () -> Unit = { sceneContainer.changeToAsync(this::class) }) = Delegates.observable(initial) { prop, old, new -> refresh() }

    @ViewProperty
    @KeepOnReload var vfont: TtfFont by refreshable(DefaultTtfFont) { launchImmediately { this@MainGraphicsText.sceneView.sceneMainSync() } }
    @ViewProperty
    @KeepOnReload var align by refreshable(TextAlignment.MIDDLE_CENTER) { launchImmediately { this@MainGraphicsText.sceneView.sceneMainSync() } }

    override suspend fun SContainer.sceneMain() {
        val helvetica = resourcesVfs["helvetica.otf"].readTtfFont()
        views.debugHightlightView(this)
        vfont = helvetica
        sceneMainSync()
    }

    suspend fun SContainer.sceneMainSync() {
        removeChildren()

        //val font = resourcesVfs["Radicalis.ttf"].readTtfFont()

        graphics {
            fillStroke(Colors.WHITE, Stroke(Colors.RED, thickness = 5.0)) {
                //rect(0, 0, 100, 100)
                text("Hello World!", font = this@MainGraphicsText.vfont, textSize = 128.0, align = this@MainGraphicsText.align)
            }
            fill(Colors.GREEN) {
                circle(0, 0, 10)
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
