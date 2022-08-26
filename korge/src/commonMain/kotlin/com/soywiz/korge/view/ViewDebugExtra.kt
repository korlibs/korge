package com.soywiz.korge.view

import com.soywiz.kds.Extra
import com.soywiz.kds.FastArrayList
import com.soywiz.korge.debug.uiCollapsibleSection
import com.soywiz.korui.UiContainer

class DebugExtraComponents(val view: View) {
    val sections = LinkedHashMap<String, FastArrayList<UiContainer.() -> Unit>>()

    init {
        view.extraBuildDebugComponent = { views, view, container ->
            for ((section, callbacks) in sections) {
                container.uiCollapsibleSection(section) {
                    for (callback in callbacks) callback()
                }
            }
        }
    }

    fun add(section: String, block: UiContainer.() -> Unit) {
        sections.getOrPut(section) { FastArrayList() }.add(block)
    }
}

val View.debugExtraComponents: DebugExtraComponents by Extra.PropertyThis { DebugExtraComponents(this) }

fun View.addDebugExtraComponent(section: String, block: UiContainer.() -> Unit) {
    debugExtraComponents.add(section, block)
}
