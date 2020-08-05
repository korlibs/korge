import com.soywiz.korge.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.input.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.awt.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import kotlinx.coroutines.*
import java.awt.*
import java.awt.Graphics
import javax.swing.*
import java.awt.event.*
import kotlin.system.*
import java.awt.Dimension
import javax.swing.plaf.*
import javax.swing.UIManager

/*
fun main() {
    UIManager.put("Tree.collapsedIcon", IconUIResource(NodeIcon('+')));
    UIManager.put("Tree.expandedIcon", IconUIResource(NodeIcon('-')));

    val f = JFrame("hello")

    val canvas = GLCanvas()
    val tree = ViewsDebuggerComponent(null)
    f.contentPane.layout = GridLayout(1, 2)
    f.add(canvas)
    f.add(tree)

    f.size = Dimension(512, 512)
    f.isVisible = true
    f.setLocationRelativeTo(null)
    f.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(event: WindowEvent) {
            exitProcess(0)
        }
    })


    //launchImmediately(Dispatchers.Unconfined) {
    runBlocking {
        println("[1]")
        val korge = GLCanvasKorge(canvas, 512, 512)
        println("[2]")
        launchImmediately {
            korge.executeInContext {
                tree.setRootView(stage, views.coroutineContext, views)
                val rect = solidRect(50, 50, Colors.BLUE)
                rect.name = "MyRectangle"
                rect.mouse {
                    click {
                        println("CLICKED!")
                    }
                }
                tree.update()
                animate {  }
                tween(rect::x[400])
                //views.gameWindow.exit()
            }
        }
        println("[3]")
        //delay(0.5.seconds)
        //korge.close()
    }

    //Timer(100) { canvas.repaint() }.start()
}

class NodeIcon(private val type: Char) : Icon {
    override fun getIconWidth(): Int = SIZE
    override fun getIconHeight(): Int = SIZE

    override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
        g.color = UIManager.getColor("Tree.background")
        g.fillRect(x, y, SIZE - 1, SIZE - 1)

        g.color = UIManager.getColor("Tree.hash").darker()
        g.drawRect(x, y, SIZE - 1, SIZE - 1)

        g.color = UIManager.getColor("Tree.foreground")
        g.drawLine(x + 2, y + SIZE / 2, x + SIZE - 3, y + SIZE / 2)
        if (type == '+') {
            g.drawLine(x + SIZE / 2, y + 2, x + SIZE / 2, y + SIZE - 3)
        }
    }

    companion object {
        private val SIZE = 9
    }
}
*/
