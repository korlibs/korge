package korlibs.datastructure.event

import korlibs.datastructure.lock.*
import korlibs.datastructure.thread.*
import korlibs.time.*
import java.awt.*
import javax.swing.*
import kotlin.test.*

class JvmSyncEventLoopTest {

    @Test
    @Ignore
    fun test() {
        //val precise = false
        val precise = true
        val el = SyncEventLoop(precise = precise)
        val lock = Lock()
        var n = 0

        fun updateGame() {
            lock {
                n++
            }
        }
        fun drawGame(g: Graphics, y: Int) {
            lock {
                val g2d = (g as Graphics2D)

                val b = g.clipBounds
                g2d.color = Color(n and 0xFF, n and 0xFF, n and 0xFF, 0xFF)
                g2d.clearRect(0, 0, b.width, b.height)
                g2d.fillRect(b.x, b.y + y, 100, 100)
                //g.dispose()
            }
        }

        val frame = object : JFrame() {
            init {
                add(object : Container() {
                    override fun paint(g: Graphics) {
                        //super.paint(g)
                        drawGame(g, 0)
                    }
                })
            }
        }
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.preferredSize = Dimension(200, 200)
        frame.setLocationRelativeTo(null)
        //frame.createBufferStrategy(2)
        frame.pack()


        el.setInterval(60.hz.timeSpan) {
            //println("ITEM")
            updateGame()
        }
        el.start()
        nativeThread {
            var running = true
            while (running) {
                val fps = frame.graphicsDevice.displayMode.refreshRate.takeIf { it > 10 } ?: 10
                //println("fps=$fps")
                for (n in 0 until 10) {
                    if (!running) break
                    frame.repaint()
                    NativeThread.sleep(fps.hz.timeSpan, precise)
                }
            }
        }
        frame.isVisible = true
        frame.createBufferStrategy(2)

        /*
        val bc = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice()
            .getDefaultConfiguration()
            .getBufferCapabilities()
        println("BC=$bc")
        //val b = ExtendedBufferCapabilities(
        //    BufferCapabilities(ImageCapabilities(true), ImageCapabilities(true), FlipContents.PRIOR),
        //    ExtendedBufferCapabilities.VSyncType.VSYNC_ON
        //)
        //frame.createBufferStrategy(2, b)
        frame.createBufferStrategy(3)
        val strategy = frame.bufferStrategy

        nativeThread {
            while (true) {
                NativeThread.sleepExact(60.hz.timeSpan)
                val time = measureTime {
                do {
                        do {
                            val g = strategy.drawGraphics
                            // Your rendering code here
                        } while (strategy.contentsRestored())
                        strategy.show()
                } while (strategy.contentsLost())
                }
                println("time=$time")
            }
        }

         */

        NativeThread.sleep(100.seconds)
    }


    private val ge by lazy { GraphicsEnvironment.getLocalGraphicsEnvironment() }
    val JFrame.graphicsDevice: GraphicsDevice get() {
        var currentGraphicsDevice: GraphicsDevice? = null
        for (gd in ge.screenDevices) {
            if (gd.defaultConfiguration.bounds.intersects(this.bounds)) {
                currentGraphicsDevice = gd
                break
            }
        }
        return currentGraphicsDevice ?: ge.defaultScreenDevice
    }
}
