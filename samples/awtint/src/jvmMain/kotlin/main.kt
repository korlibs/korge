import com.soywiz.kgl.*
import com.soywiz.korgw.awt.*
import com.soywiz.korgw.platform.*
import com.soywiz.korgw.x11.*
import com.sun.jna.*
import com.sun.jna.platform.*
import com.sun.jna.platform.unix.*
import com.sun.jna.ptr.*
import sun.java2d.opengl.*
import sun.java2d.x11.*
import java.awt.*
import javax.swing.*
import java.awt.event.*
import kotlin.system.*
//import sun.awt.X11ComponentPeer
//import sun.awt.AWTAccessor
//import sun.awt.AWTAccessor.ComponentAccessor

fun main() {
    val f = object : Frame("hello") {
        override fun update(g: Graphics) {
            paint(g)
        }

        override fun paintAll(p0: Graphics?) {
        }

        override fun paint(p0: Graphics) {
        }
    }

    var color = 0f
    val label1 = object : GLCanvas() {
        override fun render(gl: KmlGl) {
            gl.clearColor(0f, color, 0f, 1f)
            color += 0.01f
            gl.clear(X11KmlGl.COLOR_BUFFER_BIT)
        }
    }
    f.add(label1)

    Timer(100, object : ActionListener {
        override fun actionPerformed(e: ActionEvent?) {
            //println("FRAME!")
            label1.repaint()
        }
    }).start()

    f.setSize(300, 100)
    f.isVisible = true
    f.setLocationRelativeTo(null)
    f.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(event: WindowEvent) {
            exitProcess(0)
        }
    })
}
