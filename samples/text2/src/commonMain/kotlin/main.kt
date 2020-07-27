import com.soywiz.korge.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.vector.paint.*
import com.soywiz.korma.geom.*

@OptIn(KorgeExperimental::class)
suspend fun main() = Korge(quality = GameWindow.Quality.PERFORMANCE, width = 512, height = 160, title = "Korge's Text2!", bgcolor = Colors["#112"]) {
	val font = BitmapFont(
		DefaultTtfFont, 64.0,
		paint = LinearGradientPaint(0, 0, 0, 50).add(0.0, Colors.CADETBLUE).add(1.0, Colors.PURPLE)
	)

	var offset = 0.degrees
	addUpdater { offset += 10.degrees }
	text2("Hello World!", font = font, renderer = CreateStringTextRenderer { text, n, c, c1, g, advance ->
		transform.identity()
		val sin = sin(offset + (n * 360 / text.length).degrees)
		transform.rotate(15.degrees)
		transform.translate(0.0, sin * 16)
		transform.scale(1.0, 1.0 + sin * 0.1)
		put(c)
		advance(advance)
	}).position(100, 100)
}
