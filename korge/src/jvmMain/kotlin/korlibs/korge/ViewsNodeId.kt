package korlibs.korge

import korlibs.korge.view.*

open class ViewsNodeId(val views: Views) {
    var lastId: Long = 1L

    var View.nodeId by extraViewProp { lastId++ }

    init {
        views.stage.nodeId // stage has ID = 1
    }

    fun getId(view: View?): Long {
        return view?.nodeId ?: 0L
    }

    fun findById(id: Long): View? {
        views.root.foreachDescendantInline {
            if (it.nodeId == id) {
                return it
            }
        }
        return null
    }
}
