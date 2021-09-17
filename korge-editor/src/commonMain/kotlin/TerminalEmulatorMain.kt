import com.soywiz.kds.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.fast.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*

suspend fun Stage.terminalEmulatorMain() {
    container {
        scale(0.5)

        val glyphs = resourcesVfs["cp437.png"]
            .readBitmap()
            .toBMP32()
            .apply { updateColors { if (it == Colors.BLACK) Colors.TRANSPARENT_BLACK else it } }
            .slice()
            .split(16, 16)
            .toTypedArray()

        val terminalEmulatorView = TerminalEmulatorView(128, 64, glyphs).xy(32, 128)
        addChild(terminalEmulatorView)
        val colors =
            arrayOf(Colors.BLACK, Colors.BLUE, Colors.GREEN, Colors.CYAN, Colors.PURPLE, Colors.YELLOW, Colors.WHITE)
        val chars = CharArray(256) { it.toChar() }
        addUpdater {
            for (row in 0 until terminalEmulatorView.rows) {
                for (col in 0 until terminalEmulatorView.columns) {
                    terminalEmulatorView.setGlyph(col, row, chars.random(), colors.random(), colors.random())
                }
            }
        }
    }
}

class TerminalEmulatorView(val columns: Int, val rows: Int, val glyphs: Array<out BmpSlice>) : Container() {
    private val bgBitmap = Bitmap32(16, 16, Colors.WHITE).slice()

    private val bgFSprites = FSprites(columns * rows)
    private val bgView = bgFSprites.createView(bgBitmap.bmp).addTo(this)
    private val bgMat = IntArray2(columns, rows) { bgFSprites.alloc().id }

    private val fgFSprites = FSprites(columns * rows)
    private val fgView = fgFSprites.createView(glyphs.first().bmp).addTo(this)
    private val fgMat = IntArray2(columns, rows) { fgFSprites.alloc().id }

    fun setGlyph(col: Int, row: Int, char: Char, fgcolor: RGBA, bgcolor: RGBA) {
        bgFSprites.apply {
            FSprite(bgMat[col, row]).colorMul = bgcolor
        }
        fgFSprites.apply {
            val fsprite = FSprite(fgMat[col, row])
            fsprite.colorMul = fgcolor
            fsprite.setTex(glyphs[char.code])
        }
    }

    init {
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                fgFSprites.apply {
                    val fsprite = FSprite(fgMat[col, row])
                    fsprite.x = col * 16f
                    fsprite.y = row * 16f
                }
                bgFSprites.apply {
                    val fsprite = FSprite(bgMat[col, row])
                    fsprite.x = col * 16f
                    fsprite.y = row * 16f
                    fsprite.colorMul = Colors.BLACK
                    fsprite.setTex(bgBitmap)
                }
            }
        }
    }
}
