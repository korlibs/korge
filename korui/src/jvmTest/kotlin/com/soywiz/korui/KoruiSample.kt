package com.soywiz.korui

import com.soywiz.korev.*
import com.soywiz.korui.geom.len.*
import com.soywiz.korui.style.*
import com.soywiz.korui.ui.*

object KoruiSample {
	@JvmStatic
	fun main(args: Array<String>) = Korui {
		val app = Application()
		app.apply {
			frame("HELLO") {
				vertical {
                    val vertical = this@vertical
					button("HI!") {
                        onClick {
                            println("ON CICK!")
                            vertical.addBlock {
                                button("Demo")
                            }
                            //vertical.recreate()
                            vertical.relayout()
                            vertical.repaint()
                        }
                    }
					comboBox(1, 2, 3)
					progress(50, 100)
					slider(50, 100)
                    horizontal {
                        label("Hello") {
                            //this.padding = Padding(left = 80.pt)
                            width = 200.pt
                            maxWidth = 200.pt
                        }
                        label("World")
                    }
				}
			}
		}
	}
}
