package com.soywiz.korge.awt

import com.soywiz.korge.GLCanvasWithKorge
import com.soywiz.korge.Korge
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.korma.geom.SizeInt
import kotlinx.coroutines.runBlocking
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JFrame

object AwtSample {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val frame = JFrame()
            frame.preferredSize = Dimension(200, 200)
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.layout = GridLayout(5, 1)
            frame.add(JButton("[1]"))
            frame.add(GLCanvasWithKorge(Korge.Config(virtualSize = SizeInt(512, 512), scaleMode = ScaleMode.NO_SCALE, scaleAnchor = Anchor.TOP_LEFT)) {
                views.clearColor = Colors.RED
                solidRect(100, 100, Colors.YELLOW)
            })
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
