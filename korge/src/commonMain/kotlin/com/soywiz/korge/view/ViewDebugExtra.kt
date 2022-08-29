package com.soywiz.korge.view

import com.soywiz.kds.Extra
import com.soywiz.kds.FastArrayList
import com.soywiz.korge.debug.uiCollapsibleSection
import com.soywiz.korio.lang.Closeable
import com.soywiz.korui.UiContainer

class DebugExtraComponents(val view: View) {
    val sections = LinkedHashMap<String, FastArrayList<UiContainer.(Views) -> Unit>>()
    val oldExtraDebugComponents = view.extraBuildDebugComponent

    init {
        view.extraBuildDebugComponent = { views, view, container ->
            oldExtraDebugComponents?.invoke(views, view, container)
            for ((section, callbacks) in sections) {
                container.uiCollapsibleSection(section) {
                    for (callback in callbacks) callback(views)
                }
            }
        }
    }

    fun add(section: String, block: UiContainer.(Views) -> Unit): Closeable {
        val list = sections.getOrPut(section) { FastArrayList() }
        list.add(block)
        return Closeable { list.remove(block) }
    }
}

val View.debugExtraComponents: DebugExtraComponents by Extra.PropertyThis { DebugExtraComponents(this) }

fun View.addDebugExtraComponent(section: String, block: UiContainer.(views: Views) -> Unit): Closeable {
    return debugExtraComponents.add(section, block)
}
