import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*


const val WIDTH = 640
const val HEIGHT = 480

const val SHIP_SIZE = 24


val Stage.assets by Extra.PropertyThis<Stage, Assets> { Assets(SHIP_SIZE) }


suspend fun main() = Korge(
	width = WIDTH, height = HEIGHT,
	virtualWidth = WIDTH, virtualHeight = HEIGHT,
	bgcolor = Colors["#222"],
	clipBorders = false
) {
	views.gameWindow.icon = assets.shipBitmap

    val gameHolder = GameHolder(this)

    addEventListener<GameRestartEvent> {
        gameHolder.restart()
    }
}


class GameRestartEvent : Event()
