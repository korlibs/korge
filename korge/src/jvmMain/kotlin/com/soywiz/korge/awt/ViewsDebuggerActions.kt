package com.soywiz.korge.awt

import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korio.serialization.xml.*
import javax.swing.tree.*

class ViewsDebuggerActions(val views: Views, val component: ViewsDebuggerComponent) {
    //var selectedView: View? = null
    val selectedView: View? get() = (component.tree.selectionPath?.lastPathComponent as? ViewNode)?.view
    var pasteboard: Xml? = null
    val stage get() = views.stage
    val currentVfs get() = views.currentVfs

    fun selectView(view: View?) {
        views.debugHightlightView(view)
        //selectedView = view
    }

    fun addSelection(view: View?) {
        views.debugHightlightView(view)
        //selectedView = view
    }

    suspend fun cut() {
        val view = selectedView
        if (view != null) {
            pasteboard = view.viewTreeToKTree(views, currentVfs)
            selectView(view.parent)
            view.removeFromParent()
            save("Cut", view)
        }
    }

    suspend fun copy() {
        val view = selectedView
        if (view != null) {
            pasteboard = view.viewTreeToKTree(views, currentVfs)
        }
    }

    suspend fun paste(save: Boolean = true) {
        val view = selectedView
        if (view != null) {
            val pasteboard = pasteboard
            val container: Container = view.findFirstAscendant { it is ViewLeaf }?.parent
                ?: (view as? Container?)
                ?: view.parent
                ?: stage

            if (pasteboard != null) {
                val newView = pasteboard.ktreeToViewTree(views, currentVfs)
                container.addChild(newView)
                selectView(newView)
            }
        }
        if (save) {
            save("Paste", view)
        }
    }

    suspend fun duplicate() {
        copy()
        //selectView(selectedView?.parent)
        paste(save = false)
        save("Duplicate", selectedView)
    }

    fun moveView(dx: Int, dy: Int, shift: Boolean) {
        val view = selectedView
        val increment = if (shift) 10.0 else 1.0
        if (view != null) {
            view.x += dx * increment
            view.y += dy * increment
        }
        save("Move", view)
    }

    fun attachNewView(newView: View?) {
        if (newView == null) return
        println("attachNewView.selectedView: $selectedView")
        (selectedView as Container?)?.addChild(newView)
        selectView(newView)
        save("Create", newView)
    }

    fun save(action: String, newView: View? = selectedView) {
        views.debugSaveView(action, newView)
    }

    fun removeCurrentNode() {
        val parent = selectedView?.parent
        selectedView?.removeFromParent()
        selectView(parent)
        save("Remove", parent)
    }

    fun sendToBack() {
        selectedView?.let { it.parent?.sendChildToBack(it) }
        save("Send to back", selectedView)
    }

    fun sendToFront() {
        selectedView?.let { it.parent?.sendChildToFront(it) }
        save("Send to front", selectedView)
    }

}
