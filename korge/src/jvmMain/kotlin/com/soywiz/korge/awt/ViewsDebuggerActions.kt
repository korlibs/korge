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

    suspend fun cut() {
        val view = selectedView
        if (view != null) {
            pasteboard = view.viewTreeToKTree(views, currentVfs)
            selectView(view.parent)
            view.removeFromParent()
        }
    }

    suspend fun copy() {
        val view = selectedView
        if (view != null) {
            pasteboard = view.viewTreeToKTree(views, currentVfs)
        }
    }

    suspend fun paste() {
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
    }

    suspend fun duplicate() {
        copy()
        //selectView(selectedView?.parent)
        paste()
    }

    fun moveView(dx: Int, dy: Int, shift: Boolean) {
        val view = selectedView
        val increment = if (shift) 10.0 else 1.0
        if (view != null) {
            view.x += dx * increment
            view.y += dy * increment
        }
    }

    fun attachNewView(newView: View?) {
        if (newView == null) return
        println("attachNewView.selectedView: $selectedView")
        (selectedView as Container?)?.addChild(newView)
        selectView(newView)
        save(newView)
    }

    fun save(newView: View? = selectedView) {
        views.stage.views.debugSaveView(newView)
    }

    fun removeCurrentNode() {
        val parent = selectedView?.parent
        selectedView?.removeFromParent()
        selectView(parent)
        save(parent)
    }

    fun sendToBack() {
        selectedView?.let { it.parent?.sendChildToBack(it) }
    }

    fun sendToFront() {
        selectedView?.let { it.parent?.sendChildToFront(it) }
    }

}
