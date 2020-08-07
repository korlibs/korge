package com.soywiz.korge.debug

import com.soywiz.korui.*

class UiCollapsableSection(app: UiApplication, val name: String, val componentChildren: List<UiComponent>) : UiContainer(app) {
    private lateinit var mycontainer: UiContainer

    init {
        button(name) {
            onClick {
                mycontainer.visible = !mycontainer.visible
            }
        }
        mycontainer = container {
            for (child in componentChildren) {
                addChild(child)
            }
        }
    }
}
