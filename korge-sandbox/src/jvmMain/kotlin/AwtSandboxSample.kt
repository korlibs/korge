import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.iterators.fastForEachWithIndex
import com.soywiz.korge.GLCanvasKorge
import com.soywiz.korge.GLCanvasWithKorge
import com.soywiz.korge.Korge
import com.soywiz.korge.input.onClick
import com.soywiz.korge.jvmEnsureAddOpens
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.descendantsWith
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.toRgba
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.SizeInt
import java.awt.Component
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.coroutines.EmptyCoroutineContext

object AwtSandboxSample {
    @JvmStatic
    fun main(args: Array<String>) {
        jvmEnsureAddOpens()

        val frame = JFrame()
        frame.isVisible = false
        frame.ignoreRepaint = true
        //background = Color.black
        frame.setBounds(0, 0, 640, 480)
        frame.setLocationRelativeTo(null)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isVisible = true
        frame.title = "KorGE Awt Sandbox Sample"

        val components = arrayListOf<Component>()

        for (n in 0 until 9) {
            if (n == 4 || n == 8) {
            //if (n == 4) {
                components.add(
                    frame.add(
                        GLCanvasWithKorge(
                            Korge.Config(
                                bgcolor = frame.background.toRgba(),
                                virtualSize = SizeInt(UIButton.DEFAULT_WIDTH.toInt(), UIButton.DEFAULT_HEIGHT.toInt() * 3),
                                scaleAnchor = Anchor.CENTER
                            )
                        ) {
                            uiVerticalStack {
                                uiButton("${views.devicePixelRatio}")
                                uiButton("${n}HELLO") {
                                    name = "helloButton"
                                    onClick {
                                        SwingUtilities.invokeLater {
                                            components.fastForEachWithIndex { index, component ->
                                                if (component is JButton) {
                                                    component.text = "K$index"
                                                }
                                            }
                                        }
                                    }
                                }
                                uiButton("WORLD")
                            }
                        })
                )
            } else {
                components.add(frame.add(JButton("$n").also {
                    it.addActionListener {
                        components.fastForEachWithIndex { index, component ->
                            if (component is JButton) {
                                SwingUtilities.invokeLater {
                                    component.text = "J$index"
                                }
                            }
                            if (component is GLCanvasWithKorge) {
                                component.korge.launchInContext {
                                    (stage.findViewByName("helloButton") as UIButton).text = "$n/$index YAY!"
                                }
                            }
                        }
                    }
                }))
            }
        }
        frame.layout = GridLayout(3, 3, 0, 0)
        frame.validate()
    }
}
