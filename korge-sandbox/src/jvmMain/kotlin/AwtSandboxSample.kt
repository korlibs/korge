import com.soywiz.kds.iterators.fastForEachWithIndex
import com.soywiz.korge.*
import com.soywiz.korge.input.onClick
import com.soywiz.korge.ui.*
import com.soywiz.korim.color.toRgba
import com.soywiz.korma.geom.*
import java.awt.Component
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.SwingUtilities

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
                            KorgeConfig(
                                bgcolor = frame.background.toRgba(),
                                virtualSize = Size(UIButton.DEFAULT_WIDTH, UIButton.DEFAULT_HEIGHT * 3).toInt(),
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
