package korlibs.render.awt

import korlibs.math.*
import korlibs.math.geom.*
import korlibs.render.*
import java.awt.*
import java.awt.event.*
import javax.swing.*

class AwtGameWindowDebugger(val gameWindow: GameWindow, val mainFrame: JFrame) {

    val debugFrame = JFrame("Debug").apply {
        try {
            this.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            this.setSize(280, 256)
            this.type = Window.Type.UTILITY
            this.isAlwaysOnTop = true
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        val debugFrame = this
        gameWindow.onDebugChanged.add {
            EventQueue.invokeLater {
                debugFrame.isVisible = it
                synchronizeDebugFrameCoordinates()
                if (debugFrame.isVisible) {
                    //frame.isVisible = false
                    mainFrame.isVisible = true
                }
            }
        }
    }

    init {

        mainFrame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                debugFrame.isVisible = false
                debugFrame.dispose()
                gameWindow.close()
            }
        })
        mainFrame.addComponentListener(object : ComponentAdapter() {
            override fun componentMoved(e: ComponentEvent?) {
                synchronizeDebugFrameCoordinates()
            }

            override fun componentResized(e: ComponentEvent?) {
                synchronizeDebugFrameCoordinates()
            }
        })
    }


    private fun synchronizeDebugFrameCoordinates() {
        val displayMode = mainFrame.getScreenDevice().displayMode
        //println("frame.location=${frame.location}, frame.size=${frame.size}, debugFrame.width=${debugFrame.width}, displayMode=${displayMode.width}x${displayMode.height}")
        val frameBounds = RectangleInt(mainFrame.location.x, mainFrame.location.y, mainFrame.size.width, mainFrame.size.height)
        debugFrame.setLocation(frameBounds.right.clamp(0, (displayMode.width - debugFrame.width * 1.0).toInt()), frameBounds.top)
        debugFrame.setSize(debugFrame.width.coerceAtLeast(64), frameBounds.height)
        //debugFrame.pack()
        //debugFrame.doLayout()
        //debugFrame.repaint()
    }
}
