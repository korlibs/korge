import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.bitmap.effect.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.text.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korui.*
import com.soywiz.korui.layout.*
import kotlin.reflect.*

val DEFAULT_BG = Colors["#2b2b2b"]

suspend fun main() {
    //GLOBAL_CHECK_GL = true
    Korge(width = 960, height = 720, bgcolor = DEFAULT_BG, clipBorders = false, scaleAnchor = Anchor.TOP_LEFT) {
        val font0 = resourcesVfs["clear_sans.fnt"].readFont()
        val font1 = debugBmpFont
        val font2 = DefaultTtfFont
        val font3 = BitmapFont(DefaultTtfFont, 64.0)
        val font4 = BitmapFont(DefaultTtfFont, 64.0, paint = Colors.BLUEVIOLET, effect = BitmapEffect(
            blurRadius = 0,
            dropShadowX = 2,
            dropShadowY = 2,
            dropShadowColor = Colors.GREEN,
            dropShadowRadius = 1,
            //borderSize = 1,
            //borderColor = Colors.RED,
        ))
        val font5 = resourcesVfs["Pacifico.ttf"].readFont()
        lateinit var text1: Text

        val textStrs = mapOf(
            "simple" to "01xXhjgÁEñÑ",
            "UPPER" to "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            "lower" to "abcdefghijklmnopqrstuvwxyz",
            "number" to "0123456789",
            "fox" to "The quick brown fox jumps over the lazy dog. 1234567890",
        )
        val fontSizes = listOf(8, 16, 32, 64, 128, 175)
        val verticalAlignments = VerticalAlign.values().toList()
        val horizontalAlignments = HorizontalAlign.values().toList()

        val fonts = mapOf(
            "DebugBMP" to font1,
            "BMPFile" to font0,
            "ExternalTTF" to font5,
            "DefaultTTF" to font2,
            "TTFtoBMP" to font3,
            "TTFtoBMPEffect" to font4,
        )

        container {
            xy(0, 500)
            val leftPadding = 50
            text1 = text(textStrs["simple"]!!, 175.0, Colors.WHITE, font2, alignment = TextAlignment.BASELINE_LEFT, autoScaling = true).xy(leftPadding, 0)
            val gbounds = graphics {}.xy(leftPadding, 0)

            val baseLineLine = solidRect(960 + 1200, 1, Colors.ORANGE)
            val baseAscent = solidRect(960 + 1200, 1, Colors.BLUE)
            val baseDescent = solidRect(960 + 1200, 1, Colors.PURPLE)

            var cachedBounds: Rectangle? = null
            fun updateBounds() {
                val currentBounds = text1.getLocalBounds()
                if (cachedBounds != currentBounds) {
                    cachedBounds = currentBounds
                    gbounds.clear()
                    gbounds.stroke(Colors.RED, StrokeInfo(2.0)) {
                        rect(text1.getLocalBounds())
                    }
                    gbounds.stroke(Colors.BLUE, StrokeInfo(2.0)) {
                        line(-5, 0, +5, 0)
                        line(0, -5, 0, +5)
                    }
                    val metrics = text1.font.getOrNull()!!.getFontMetrics(text1.fontSize)
                    baseLineLine.xy(0.0, -metrics.baseline)
                    baseAscent.xy(0.0, -metrics.ascent)
                    baseDescent.xy(0.0, -metrics.descent)
                }
            }

            addUpdater {
                updateBounds()
            }
            updateBounds()

        }

        data class SecInfo<T>(
            val name: String,
            val prop: KMutableProperty0<T>,
            val items: List<T>,
            val convert: (T) -> String = { it.toString().toLowerCase().capitalize() }
        )

        korui(width, 200) {
            for (info in listOf(
                SecInfo("Vertical", text1::verticalAlign, verticalAlignments),
                SecInfo("Horizontal", text1::horizontalAlign, horizontalAlignments),
                SecInfo("Size", text1::textSize, fontSizes.map { it.toDouble() }) { "${it.toInt()}" },
            )) {
                @Suppress("UNCHECKED_CAST") val rinfo = (info as SecInfo<Any>)
                horizontal {
                    label("${info.name}:")
                    val prop = ObservableProperty(info.prop)
                    @Suppress("UNCHECKED_CAST") val rprop = (prop as ObservableProperty<Any>)
                    for (item in info.items) {
                        toggleButton(rinfo.convert(item)) {
                            prop.observeStart { this.pressed = (it == item) }
                            onClick {
                                rprop.value = item
                            }
                        }
                    }
                }
            }
            val fontProp = ObservableProperty(text1.font.getOrNull()!!).observeStart { text1.font = it }
            horizontal {
                label("Font:")
                gameWindow.onDragAndDropFileEvent {
                    when (it.type) {
                        DropFileEvent.Type.START -> {
                            views.clearColor = DEFAULT_BG.interpolateWith(0.2, Colors.RED)
                        }
                        DropFileEvent.Type.END -> {
                            views.clearColor = DEFAULT_BG
                        }
                        DropFileEvent.Type.DROP -> {
                            try {
                                val file = it.files?.firstOrNull()?.jailParent()
                                val font = file?.readFont()
                                if (font != null) {
                                    fontProp.value = font
                                }
                            } catch (e: Throwable) {
                                gameWindow.alertError(e)
                                throw e
                            }
                        }
                    }
                }
                for ((key, value) in fonts) {
                    toggleButton(key) {
                        fontProp.observeStart { this.pressed = (it == value) }
                        onClick { fontProp.value = value }
                    }
                }
            }
            horizontal {
                label("Text:")
                val prop = ObservableProperty(textStrs.values.first()).observeStart { text1.text = it }
                for ((key, value) in textStrs) {
                    toggleButton(key) {
                        prop.observeStart { this.pressed = (it == value) }
                        onClick { prop.value = value }
                    }
                }
            }
            horizontal {
                checkBox("Autoscale") {
                    checked = text1.autoScaling
                    onChange { text1.autoScaling = it }
                }
                checkBox("Smooth") {
                    checked = text1.smoothing
                    onChange { text1.smoothing = it }
                }
                checkBox("Native Render") {
                    checked = text1.useNativeRendering
                    onChange { text1.useNativeRendering = it }
                }
            }
            horizontal {
                button("Select file...") {
                    onClick {
                        launchImmediately {
                            val file = gameWindow.openFileDialog().firstOrNull()
                            if (file != null) {
                                fontProp.value = file.readFont()
                            }
                        }
                    }
                }
            }
        }
    }
}
