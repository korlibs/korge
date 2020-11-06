package com.soywiz.korge.intellij.components

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.*
import com.intellij.openapi.actionSystem.impl.*
import com.intellij.ui.components.*
import com.soywiz.korge.awt.*
import com.soywiz.korim.color.*
import java.awt.*
import java.awt.event.*
import java.awt.font.*
import javax.swing.*


class MyToolbarButton(text: String) : JButton() {
    var down = false
    var over = false
    init {
        this.text = text
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                down = true
            }

            override fun mouseReleased(e: MouseEvent?) {
                down = false
            }

            override fun mouseEntered(e: MouseEvent) {
                over = true
                repaint()
            }

            override fun mouseExited(e: MouseEvent) {
                over = false
                repaint()
            }
        })
    }

    override fun paintBorder(g: Graphics) {
    }

    override fun paintComponent(g: Graphics) {
        val look = ActionButtonLook.SYSTEM_LOOK

        look.paintBackground(g, this, when {
            down -> ActionButtonComponent.PUSHED
            this.isSelected -> ActionButtonComponent.SELECTED
            over -> ActionButtonComponent.POPPED
            else -> ActionButtonComponent.NORMAL
        })
        val g = g as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val bounds = g.font.getStringBounds(this.text, g.fontRenderContext)
        //println(bounds)
        g.drawString(this.text, ((width / 2 - bounds.width / 2) -bounds.x).toInt(), ((height / 2 - bounds.height / 2) -bounds.y).toInt())

        /*
        //val backgroundColor = JBUI.CurrentTheme.ActionButton.pressedBackground()
        val color = JToggleButton().also { it.isSelected = true }.background
        //g.paint = Colors.RED.toAwt()
        g.paint = color
        g.fillRoundRect(1, 1, width - 2, height - 2, 8, 8)
        //g.fillRect(0, 0, width, height)
         */
    }
}

fun Styled<out Container>.toolbarButton(text: String, tooltip: String? = null, block: @UIDslMarker Styled<AbstractButton>.() -> Unit = {}): Styled<AbstractButton> {
    /*
    val fg1: Color? = JPanel().foreground
    //val fg2 = Colors.WHITE.toAwt()
    val fg2: Color? = null
    val bg1: Color? = JPanel().background
    val bg2: Color? = null

    // com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI
    component.add(JButton(text)
        .also { it.border = BorderFactory.createEmptyBorder() }
        .also { it.isOpaque = false }
        .also { it.toolTipText = tooltip }
        .also {
            it.insets.set(0, 0, 0, 0)
            //it.putClientProperty("JButton.buttonType", "help")
            it.putClientProperty("ActionToolbar.smallVariant", true)
            it.putClientProperty("JButton.textColor", fg1)
            it.putClientProperty("JButton.backgroundColor", bg1)
            it.putClientProperty("JButton.focusedBackgroundColor", bg1)
            it.putClientProperty("Button.paintShadow", false)
            it.putClientProperty("Button.shadowWidth", 0)
            //it.background = JPanel().background
        }
        .also {
            it.addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    it.putClientProperty("JButton.backgroundColor", bg2)
                    it.putClientProperty("JButton.textColor", fg2)
                }

                override fun mouseExited(e: MouseEvent) {
                    it.putClientProperty("JButton.backgroundColor", bg1)
                    it.putClientProperty("JButton.textColor", fg1)
                }
            })
        }
        .also { block(it.styled) })
     */
    val button = MyToolbarButton(text)
        .also { it.toolTipText = tooltip }
        .also { block(it.styled) }
    component.add(button)
    return button.styled
}
