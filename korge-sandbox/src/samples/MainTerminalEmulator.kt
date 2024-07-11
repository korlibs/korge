package samples

import korlibs.datastructure.*
import korlibs.datastructure.random.*
import korlibs.korge.scene.Scene
import korlibs.korge.view.*
import korlibs.korge.view.fast.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.math.geom.slice.*

/*
class MainTerminalEmulator : Scene() {
    override suspend fun SContainer.sceneMain() {
        container {
            scale(1)

            val glyphs = resourcesVfs["cp437.png"]
                .readBitmap()
                .toBMP32()
                .premultipliedIfRequired()
                .apply { updateColors { if (it == Colors.BLACK) Colors.TRANSPARENT else it } }
                .slice()
                .splitInRows(16, 16)
                .toTypedArray()

            val terminalEmulatorView = TerminalEmulatorView(128, 64, glyphs).xy(32, 32)
            addChild(terminalEmulatorView)
            val colors =
                arrayOf(Colors.BLACK, Colors.BLUE, Colors.GREEN, Colors.CYAN, Colors.PURPLE, Colors.YELLOW, Colors.WHITE)
            val flips = arrayOf(false, true)
            val chars = CharArray(256) { it.toChar() }

            addUpdater {
                for (row in 0 until terminalEmulatorView.rows) {
                    for (col in 0 until terminalEmulatorView.columns) {
                        terminalEmulatorView.setGlyph(col, row, chars.fastRandom(), colors.fastRandom(), colors.fastRandom(), flips.fastRandom(), flips.fastRandom())
                    }
                }
                terminalEmulatorView.setString(0, 0, "Hello WORLD", Colors.WHITE, Colors.RED, true, true)
                terminalEmulatorView.setString(0, 1, "Hello WORLD", Colors.WHITE, Colors.GREEN, false, true)
                terminalEmulatorView.setString(0, 2, "Hello WORLD", Colors.WHITE, Colors.BLUE, true, false)
                terminalEmulatorView.setString(0, 3, "Hello WORLD", Colors.WHITE, Colors.PURPLE, false, false)

                invalidate()
            }
        }
    }

    class TerminalEmulatorView(val columns: Int, val rows: Int, val glyphs: Array<out BmpSlice>) : Container() {
        private val bgBitmap = Bitmap32(16, 16, Colors.WHITE.premultiplied).slice()

        private val bgFSprites = FSprites(columns * rows)
        private val bgView = bgFSprites.createView(bgBitmap.bmp).addTo(this)
        private val bgMat = IntArray2(columns, rows) { bgFSprites.alloc().id }

        private val fgFSprites = FSprites(columns * rows)
        private val fgView = fgFSprites.createView(glyphs.first().bmp).addTo(this)
        private val fgMat = IntArray2(columns, rows) { fgFSprites.alloc().id }

        fun setGlyph(col: Int, row: Int, char: Char, fgcolor: RGBA, bgcolor: RGBA = Colors.BLACK, flipX: Boolean = false, flipY: Boolean = false) {
            if (col !in 0 until columns || row !in 0 until rows) return
            bgFSprites.apply {
                FSprite(bgMat[col, row]).colorMul = bgcolor
            }
            fgFSprites.apply {
                val fsprite = FSprite(fgMat[col, row])
                fsprite.colorMul = fgcolor
                fsprite.setTex(glyphs[char.code])
                fsprite.scale(if (flipX) -1f else 1f, if (flipY) -1f else 1f)
            }
        }

        fun setString(col: Int, row: Int, string: String, fgcolor: RGBA, bgcolor: RGBA = Colors.BLACK, flipX: Boolean = false, flipY: Boolean = false) {
            for (n in string.indices) setGlyph(col + n, row, string[n], fgcolor, bgcolor, flipX, flipY)
        }

        init {
            for (row in 0 until rows) {
                for (col in 0 until columns) {
                    fgFSprites.apply {
                        val fsprite = FSprite(fgMat[col, row])
                        fsprite.x = col * 16f + 8f
                        fsprite.y = row * 16f + 8f
                        fsprite.setAnchor(.5f, .5f)
                    }
                    bgFSprites.apply {
                        val fsprite = FSprite(bgMat[col, row])
                        fsprite.x = col * 16f + 8f
                        fsprite.y = row * 16f + 8f
                        fsprite.setAnchor(.5f, .5f)
                        fsprite.colorMul = Colors.BLACK
                        fsprite.setTex(bgBitmap)
                    }
                }
            }
        }
    }

}
*/
