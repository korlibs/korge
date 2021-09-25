import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.fast.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import kotlinx.coroutines.*

suspend fun main() = Korge(width = 512, height = 512, bgcolor = Colors["#2b2b2b"]) {
	terminalEmulatorMain()
}

suspend fun Stage.terminalEmulatorMain() {
	container {
		scale(1)

		val glyphs = resourcesVfs["cp437.png"]
			.readBitmap()
			.toBMP32()
			.apply { updateColors { if (it == Colors.BLACK) Colors.TRANSPARENT_BLACK else it } }
			.slice()
			.split(16, 16)
			.toTypedArray()

		val terminalEmulatorView = TerminalEmulatorView(200, 100, glyphs).xy(32, 128)
		addChild(terminalEmulatorView)
		val colors =
			arrayOf(Colors.BLACK, Colors.BLUE, Colors.GREEN, Colors.CYAN, Colors.PURPLE, Colors.YELLOW, Colors.WHITE)
		val flips = arrayOf(false, true)
		val chars = CharArray(256) { it.toChar() }

		addUpdater {
			for (row in 0 until terminalEmulatorView.rows) {
				for (col in 0 until terminalEmulatorView.columns) {
					terminalEmulatorView.setGlyph(col, row, chars.random(), colors.random(), colors.random(), flips.random(), flips.random())
				}
			}
			terminalEmulatorView.setString(0, 0, "Hello WORLD", Colors.WHITE, Colors.RED, true, true)
			terminalEmulatorView.setString(0, 1, "Hello WORLD", Colors.WHITE, Colors.GREEN, false, true)
			terminalEmulatorView.setString(0, 2, "Hello WORLD", Colors.WHITE, Colors.BLUE, true, false)
			terminalEmulatorView.setString(0, 3, "Hello WORLD", Colors.WHITE, Colors.PURPLE, false, false)
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
		for (n in 0 until string.length) setGlyph(col + n, row, string[n], fgcolor, bgcolor, flipX, flipY)
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
