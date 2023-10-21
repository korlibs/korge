package korlibs.korge.awt

import korlibs.image.color.*
import korlibs.korge.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.render.awt.*
import kotlinx.coroutines.*
import java.awt.*
import javax.swing.*

object AwtSample {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val frame = JFrame()
            frame.preferredSize = Dimension(200, 200)
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.layout = GridLayout(5, 1)
            frame.add(JButton("[1]"))
            frame.add(AwtAGOpenglCanvas().apply { gameWindow.configureKorge(KorgeConfig(virtualSize = Size(512, 512), displayMode = KorgeDisplayMode.NO_SCALE)) {
                views.clearColor = Colors.RED
                solidRect(100, 100, Colors.YELLOW)
            } })
            frame.add(JButton("[2]"))
            /*
            frame.add(GLCanvasWithKorge(Korge.Config(virtualSize = SizeInt(512, 512), clipBorders = true)) {
                views.clearColor = Colors.BLUE
                solidRect(256, 256, Colors.YELLOWGREEN)
            })

             */
            frame.add(JButton("[3]"))
            frame.pack()
            frame.setLocationRelativeTo(null)
            frame.isVisible = true
        }
    }
}
