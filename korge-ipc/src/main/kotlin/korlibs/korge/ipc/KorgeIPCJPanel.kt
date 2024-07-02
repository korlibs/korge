package korlibs.korge.ipc

import java.awt.*
import java.awt.event.*
import java.awt.image.*
import javax.swing.*

class KorgeIPCJPanel(val ipc: KorgeIPC = KorgeIPC(isServer = false)) : JPanel(), MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    var image: BufferedImage? = null

    init {
        Timer(16) {
            readFrame()
            //println(events.availableRead)
        }.also { it.isRepeats = true }.start()
    }

    override fun paint(g: Graphics) {
        //g.color = Color.RED
        //g.fillRect(0, 0, 100, 100)
        if (image != null) {
            g.drawImage(image, 0, 0, width, height, null)
        }
    }

    fun rgbaToBgra(v: Int): Int = ((v shl 16) and 0x00FF0000) or ((v shr 16) and 0x000000FF) or (v and 0xFF00FF00.toInt())

    var currentFrameId = -1

    fun readFrame() {
        val frameId = ipc.getFrameId()
        if (frameId == currentFrameId) return // Do not update
        val frame = ipc.getFrame()
        if (frame.width == 0 || frame.height == 0) return // Empty frame
        currentFrameId = frame.id
        val image = BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_ARGB)
        this.image = image
        val imgPixels = (image.raster.dataBuffer as DataBufferInt).data
        System.arraycopy(frame.pixels, 0, imgPixels, 0, frame.width * frame.height)
        for (n in imgPixels.indices) imgPixels[n] = rgbaToBgra(imgPixels[n])
        repaint()
    }

    private fun sendEv(type: Int, e: KeyEvent) = ipc.writeEvent(IPCPacket.keyPacket(type = type, keyCode = e.keyCode, char = e.keyChar.code))
    private fun sendEv(type: Int, e: MouseEvent) = ipc.writeEvent(IPCPacket.mousePacket(type = type, x = e.x, y = e.y, button = e.button))
    override fun keyTyped(e: KeyEvent) = sendEv(IPCPacket.KEY_TYPE, e)
    override fun keyPressed(e: KeyEvent) = sendEv(IPCPacket.KEY_DOWN, e)
    override fun keyReleased(e: KeyEvent) = sendEv(IPCPacket.KEY_UP, e)
    override fun mouseMoved(e: MouseEvent) = sendEv(IPCPacket.MOUSE_MOVE, e)
    override fun mouseDragged(e: MouseEvent) = sendEv(IPCPacket.MOUSE_MOVE, e)
    override fun mouseWheelMoved(e: MouseWheelEvent) = sendEv(IPCPacket.MOUSE_MOVE, e)
    override fun mouseExited(e: MouseEvent) = sendEv(IPCPacket.MOUSE_MOVE, e)
    override fun mouseEntered(e: MouseEvent) = sendEv(IPCPacket.MOUSE_MOVE, e)
    override fun mouseReleased(e: MouseEvent) = sendEv(IPCPacket.MOUSE_UP, e)
    override fun mousePressed(e: MouseEvent)  = sendEv(IPCPacket.MOUSE_DOWN, e)
    override fun mouseClicked(e: MouseEvent) = sendEv(IPCPacket.MOUSE_CLICK, e)

    init {
        addKeyListener(this)
        addMouseListener(this)
        addMouseMotionListener(this)
        addMouseWheelListener(this)
    }

    companion object {
        @JvmStatic
        fun main() {
            val frame = JFrame()
            val frameHolder = korlibs.korge.ipc.KorgeIPCJPanel()
            frame.add(frameHolder)
            frame.addKeyListener(frameHolder)

            frame.preferredSize = Dimension(640, 480)
            frame.pack()
            frame.setLocationRelativeTo(null)

            frame.isVisible = true

        }
    }
}
