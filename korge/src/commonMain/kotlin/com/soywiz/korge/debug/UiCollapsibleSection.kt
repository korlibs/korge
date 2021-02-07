package com.soywiz.korge.debug

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

fun UiContainer.uiCollapsibleSection(name: String?, block: UiContainer.() -> Unit): UiCollapsibleSection {
    return UiCollapsibleSection(app, name, block).also { addChild(it) }
}

@Deprecated(
    message = "An older name of `uiCollapsibleSection`",
    replaceWith = ReplaceWith("uiCollapsibleSection(name, block)"),
    level = DeprecationLevel.WARNING
)
fun UiContainer.uiCollapsableSection(name: String?, block: UiContainer.() -> Unit): UiCollapsibleSection {
    return UiCollapsibleSection(app, name, block).also { addChild(it) }
}

class UiCollapsibleSection(app: UiApplication, val name: String?, val componentChildren: List<UiComponent>) : UiContainer(app) {
    companion object {
        operator fun invoke(app: UiApplication, name: String?, block: UiContainer.() -> Unit): UiCollapsibleSection =
            UiCollapsibleSection(app, name, listOf()).also { block(it.mycontainer) }

        private fun createIcon(angle: Angle): NativeImage {
            return NativeImage(ICON_SIZE, ICON_SIZE).context2d {
                val s = ICON_SIZE.toDouble()
                fill(Colors.DIMGREY) {
                    if (angle == 0.degrees) {
                        translate(s * 0.5, s * 0.25)
                    } else {
                        translate(s * 0.25, s * 0.5)
                    }
                    scale(ICON_SIZE.toDouble())
                    rotate(angle)
                    moveTo(-0.5, 0.0)
                    lineTo(+0.5, 0.0)
                    lineTo(0.0, 0.5)
                    close()
                }
            }
        }

        val ICON_SIZE = 16
        val ICON_OPEN = createIcon(0.degrees)
        val ICON_CLOSE = createIcon((-90).degrees)
    }

    private lateinit var mycontainer: UiContainer

    init {
        button(name ?: "Unknown") {
            this.icon = ICON_OPEN
            onClick {
                mycontainer.visible = !mycontainer.visible
                mycontainer.root?.relayout()
                this.icon = if (mycontainer.visible) ICON_OPEN else ICON_CLOSE
            }
        }
        mycontainer = container {
            for (child in componentChildren) {
                addChild(child)
            }
        }
    }
}

@Deprecated(
    message = "An older spelling of UiCollapsableSection",
    replaceWith = ReplaceWith("UiCollapsibleSection")
)
typealias UiCollapsableSection = UiCollapsibleSection
