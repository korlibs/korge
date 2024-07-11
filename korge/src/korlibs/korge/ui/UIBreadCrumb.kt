package korlibs.korge.ui

import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.korge.component.*
import korlibs.korge.input.*
import korlibs.korge.view.*

inline fun <T> Container.uiBreadCrumb(
    path: UIBreadCrumb.Path<T>,
    block: @ViewDslMarker UIBreadCrumb<T>.() -> Unit = {}
) = UIBreadCrumb<T>(path).addTo(this).apply(block)

inline fun <T> Container.uiBreadCrumb(
    path: Iterable<T>,
    block: @ViewDslMarker UIBreadCrumb<T>.() -> Unit = {}
) = UIBreadCrumb<T>(UIBreadCrumb.Path(path.toList())).addTo(this).apply(block)

inline fun <T> Container.uiBreadCrumbArray(
    vararg path: T,
    block: @ViewDslMarker UIBreadCrumb<T>.() -> Unit = {}
) = UIBreadCrumb<T>(UIBreadCrumb.Path(path.toList())).addTo(this).apply(block)

class UIBreadCrumb<T>(path: Path<T>) : UIView() {
    constructor(path: Iterable<T>) : this(Path(path.toList()))
    constructor(vararg path: T) : this(Path(path.toList()))

    data class Path<T>(val paths: List<T>) {
        constructor(vararg paths: T) : this(paths.toList())
    }

    data class PathPosition<T>(val breadCrumb: UIBreadCrumb<T>, val index: Int, val path: Path<T>) {
        val entry: T get() = path.paths[index]
    }

    val onClickPath = Signal<PathPosition<T>>()

    var path: Path<T> = path
        set(value) {
            field = value
            removeChildren()
            var mx = 0.0
            for ((index, path) in field.paths.withIndex()) {
                if (index != 0) {
                    val text = text(">").autoSize(true).position(mx, 0.0)
                    mx += text.width + 4.0
                }
                val text = text(path.toString()).autoSize(true).position(mx, 0.0)
                text.decorateOutOver { view, over ->
                    text.colorMul = if (over) Colors.WHITE else Colors.DARKGREY
                }
                text.onClick {
                    onClickPath(PathPosition(this, index, value))
                }
                mx += text.width + 4.0
            }
        }

    init {
        this.path = path
    }
}
