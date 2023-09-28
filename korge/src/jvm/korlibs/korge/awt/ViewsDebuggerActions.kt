package korlibs.korge.awt

import korlibs.io.async.*
import korlibs.io.serialization.xml.*
import korlibs.korge.view.*

internal open class ViewsDebuggerActions(val views: Views) {
    var componentOrNull: ViewsDebuggerComponent? = null
    var component: ViewsDebuggerComponent
        get() = componentOrNull!!
        set(value) {
            componentOrNull = value
        }
    //var selectedView: View? = null
    val selectedView: View?
        get() = (componentOrNull?.tree?.selectionPath?.lastPathComponent as? ViewNode)?.view
    var pasteboard: Xml? = null
    val stage get() = views.stage
    val currentVfs get() = views.currentVfs
    var gridShowing = true
    var gridSnapping = true

    val root get() = stage
    //val camera = stage.cameraContainer(1280.toDouble(), 720.toDouble(), clip = false, contentBuilder = {
    //    KTreeRoot(it.width, it.height)
    //})
    //val root: KTreeRoot = camera.content as KTreeRoot
    //val grid get() = root.grid

    fun toggleGrid() {
        gridShowing = !gridShowing
    }

    var playing: Boolean
        get() = root.speed != 0.0
        set(value) {
            root.speed = if (value) 1.0 else 0.0
        }

    fun togglePlay() {
        playing = !playing
    }

    fun toggleReset() {
        playing = false
    }

    fun toggleGridSnapping() {
        gridSnapping = !gridSnapping
    }

    fun selectView(view: View?) {
        views.debugHightlightView(view)
        //selectedView = view
    }

    fun addSelection(view: View?) {
        views.debugHightlightView(view)
        //selectedView = view
    }

    fun cut() {
        val view = selectedView
        if (view != null) {
            //pasteboard = view.viewTreeToKTree(views, currentVfs)
            selectView(view.parent)
            view.removeFromParent()
            save("Cut", view)
        }
    }

    fun copyToXml(): Xml? {
        val view = selectedView
        if (view != null) {
            //return view.viewTreeToKTree(views, currentVfs)
            TODO()
        } else {
            return null
        }
    }

    fun copy() {
        val view = selectedView
        if (view != null) {
            pasteboard = copyToXml()
        }
    }

    suspend fun pasteFromXml(xml: Xml?, save: Boolean = true) {
        val view = selectedView ?: views.stage
        val container: Container = view.findFirstAscendant { it is ViewLeaf }?.parent
            ?: (view as? Container?)
            ?: view.parent
            ?: stage

        if (xml != null) {
            TODO()
            //val newView = xml.ktreeToViewTree(views, currentVfs)
            //container.addChild(newView)
            //selectView(newView)
        }
        if (save) {
            save("Paste", view)
        }
    }

    suspend fun paste(save: Boolean = true) {
        pasteFromXml(pasteboard, save)
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

    val finalSelectedView: View?
        get() {
            var selectedViewEx: View? = selectedView ?: root
            while (selectedViewEx != null && selectedViewEx !is Container) {
                selectedViewEx = selectedViewEx.parent
            }
            return selectedViewEx
        }

    fun attachNewView(newView: View?, parent: View? = finalSelectedView) {
        if (newView == null) return
        println("attachNewView.selectedView: $selectedView, selectedViewEx=$parent")
        (parent as Container?)?.addChild(newView)
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

    fun canDeleteCopyCut(): Boolean {
        return selectedView != null
    }

    fun canPaste(): Boolean {
        return true
    }

    open fun requestCopy() {
        copy()
    }

    open fun requestCut() {
        cut()
    }

    open fun requestPaste() {
        launchImmediately(views.coroutineContext) {
            paste()
        }
    }

    open fun requestDuplicate() {
        launchImmediately(views.coroutineContext) {
            duplicate()
        }
    }
}
