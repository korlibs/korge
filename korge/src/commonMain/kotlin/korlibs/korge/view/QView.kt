package korlibs.korge.view

import korlibs.datastructure.iterators.*
import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.math.geom.*
import kotlin.reflect.*

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

    var visible: Boolean get() = firstOrNull?.visible ?: false ; set(value) = fastForEach { it.visible = value }
    var alpha: Double get() = firstOrNull?.alpha ?: 1.0 ; set(value) = fastForEach { it.alpha = value }
    var scale: Scale get() = firstOrNull?.scale ?: Scale(1, 1) ; set(value) = fastForEach { it.scale = value }
    var scaleAvg: Double get() = firstOrNull?.scaleAvg ?: 1.0 ; set(value) = fastForEach { it.scaleAvg = value }
    var scaleX: Double get() = firstOrNull?.scaleX ?: 1.0 ; set(value) = fastForEach { it.scaleX = value }
    var scaleY: Double get() = firstOrNull?.scaleY ?: 1.0 ; set(value) = fastForEach { it.scaleY = value }
    var x: Double get() = firstOrNull?.x ?: 0.0 ; set(value) = fastForEach { it.x = value }
    var y: Double get() = firstOrNull?.y ?: 0.0 ; set(value) = fastForEach { it.y = value }


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
fun QView.alpha(value: Double) { alpha = value }
fun QView.onClick(handler: @EventsDslMarker suspend (MouseEvents) -> Unit) = fastForEach { it.onClick(handler) }
inline fun <reified T : View> QView.castTo(): T? = firstOrNull as? T?

/** Indexer that allows to get a descendant marked with the name [name]. */
operator fun View?.get(name: String): QView = QView(this)[name]
