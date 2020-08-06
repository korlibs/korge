package com.soywiz.korge.awt

import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korio.serialization.xml.*
import javax.swing.tree.*

class ViewsDebuggerActions(val views: Views, val component: ViewsDebuggerComponent) {
    var selectedView: View? = null
    var pasteboard: Xml? = null
    val stage get() = views.stage
    val currentVfs get() = views.currentVfs

    fun highlight(view: View?) {
        component.update()
        val treeNode = view?.treeNode ?: return
        val path = TreePath((component.tree.model as DefaultTreeModel).getPathToRoot(treeNode))
        component.tree.expandPath(path)
        component.tree.clearSelection()
        component.tree.addSelectionPath(path)
    }

    fun selectView(view: View?) {
        views.renderContext.debugAnnotateView = view
        views.debugHightlightView(view)
        selectedView = view
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
        (selectedView as Container?)?.addChild(newView)
        highlight(newView)
        save(newView)
    }

    fun save(newView: View? = selectedView) {
        views.stage.views.debugSaveView(newView)
    }

    fun removeCurrentNode() {
        val parent = selectedView?.parent
        selectedView?.removeFromParent()
        highlight(parent)
        save(parent)
    }

    fun sendToBack() {
        selectedView?.let { it.parent?.sendChildToBack(it) }
    }

    fun sendToFront() {
        selectedView?.let { it.parent?.sendChildToFront(it) }
    }

}
