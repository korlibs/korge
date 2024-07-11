package korlibs.korge.render

import korlibs.datastructure.*
import korlibs.korge.view.*

val RenderContext.views: Views? get() = bp as? Views?

private var RenderContext._debugAnnotateView: View? by extraProperty { null }

var RenderContext.debugAnnotateView: View?
    get() = _debugAnnotateView
    set(value) {
        views?.invalidatedView(_debugAnnotateView)
        _debugAnnotateView = value
        views?.invalidatedView(_debugAnnotateView)
    }
