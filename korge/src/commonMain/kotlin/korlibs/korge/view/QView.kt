package korlibs.korge.view

import korlibs.datastructure.iterators.fastForEach
import korlibs.korge.input.EventsDslMarker
import korlibs.korge.input.MouseEvents
import korlibs.korge.input.onClick
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.math.geom.Angle
import korlibs.math.geom.MPoint
import korlibs.math.geom.degrees
import kotlin.reflect.KMutableProperty1

class QView(val views: List<View>) : List<View> by views, BView {
    val firstOrNull: View? = views.firstOrNull()
    val first: View by lazy { firstOrNull ?: DummyView() }
    override val bview: View get() = first
    override val bviewAll: List<View> get() = views

    constructor(view: View?) : this(if (view != null) listOf(view) else emptyList())

    operator fun get(name: String): QView = QView(views.mapNotNull { it.firstDescendantWith { it.name == name } })

    fun <T> setProperty(prop: KMutableProperty1<View, T>, value: T) {
        views.fastForEach { prop.set(it, value) }
    }

    inline fun fastForEach(callback: (View) -> Unit) {
        views.fastForEach { callback(it) }
    }

    var visible: Boolean
        get() = firstOrNull?.visible ?: false
        set(value) = fastForEach { it.visible = value }

    var alpha: Float
        get() = firstOrNull?.alphaF ?: 1.0f
        set(value) = fastForEach { it.alphaF = value }

    var scale: Double
        get() = firstOrNull?.scale ?: 1.0
        set(value) = fastForEach { it.scale = value }

    var scaleX: Double
        get() = firstOrNull?.scaleX ?: 1.0
        set(value) = fastForEach { it.scaleX = value }

    var scaleY: Double
        get() = firstOrNull?.scaleY ?: 1.0
        set(value) = fastForEach { it.scaleY = value }

    var x: Double
        get() = firstOrNull?.x ?: 0.0
        set(value) = fastForEach { it.x = value }

    var y: Double
        get() = firstOrNull?.y ?: 0.0
        set(value) = fastForEach { it.y = value }

    var rotation: Angle
        get() = firstOrNull?.rotation ?: 0.degrees
        set(value) = fastForEach { it.rotation = value }

    var skewX: Angle
        get() = firstOrNull?.skewX ?: 0.degrees
        set(value) = fastForEach { it.skewX = value }

    var skewY: Angle
        get() = firstOrNull?.skewY ?: 0.degrees
        set(value) = fastForEach { it.skewY = value }

    var colorMul: RGBA
        get() = firstOrNull?.colorMul ?: Colors.WHITE
        set(value) = fastForEach { it.colorMul = value }
}

fun QView.visible(value: Boolean) { visible = value }
fun QView.alpha(value: Float) { alpha = value }
fun QView.onClick(handler: @EventsDslMarker suspend (MouseEvents) -> Unit) = fastForEach { it.onClick(handler) }
inline fun <reified T : View> QView.castTo(): T? = firstOrNull as? T?

/** Indexer that allows to get a descendant marked with the name [name]. */
operator fun View?.get(name: String): QView = QView(this)[name]
