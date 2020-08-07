package com.soywiz.korge.debug

import com.soywiz.korui.*

fun UiContainer.uiCollapsableSection(name: String, block: UiContainer.() -> Unit): UiCollapsableSection {
    return UiCollapsableSection(app, name, block).also { addChild(it) }
}

class UiCollapsableSection(app: UiApplication, val name: String, val componentChildren: List<UiComponent>) : UiContainer(app) {
    companion object {
        operator fun invoke(app: UiApplication, name: String, block: UiContainer.() -> Unit): UiCollapsableSection =
            UiCollapsableSection(app, name, listOf()).also { block(it.mycontainer) }
    }

    private lateinit var mycontainer: UiContainer

    init {
        button(name) {
            mycontainer.visible = !mycontainer.visible
            mycontainer.root?.relayout()
        }
        mycontainer = container {
            for (child in componentChildren) {
                addChild(child)
            }
        }
    }
}
