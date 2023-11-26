package korlibs.korge.style

import korlibs.datastructure.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.text.*
import korlibs.io.concurrent.atomic.*
import korlibs.korge.view.*
import kotlin.reflect.*

val View.styles: ViewStyles by Extra.PropertyThis { ViewStyles(this) }

inline fun <T : View> T.styles(block: ViewStyles.() -> Unit): T {
    styles.updateStyles {
        styles.apply(block)
    }
    return this
}

interface StyleableView {
    fun updatedStyles() {
    }
}

class ViewStyles(val view: View) {
    @PublishedApi internal var data: LinkedHashMap<String, Any?>? = null
    var updating = KorAtomicInt(0)

    fun <T> getProp(prop: KProperty<T>, default: T): T =
        (data?.get(prop.name) as? T?) ?: view.parent?.styles?.getProp(prop, default) ?: default

    fun doUpdate(updating: Int = this.updating.value) {
        if (updating == 0) {
            (view as? StyleableView)?.updatedStyles()
        }
    }

    inline fun updateStyles(block: ViewStyles.() -> Unit) {
        try {
            updating.addAndGet(+1)
            block()
        } finally {
            doUpdate(updating.addAndGet(-1))
        }
    }
}

var ViewStyles.textFont: Font by ViewStyle(DefaultTtfFontAsBitmap)
var ViewStyles.textSize: Double by ViewStyle(16.0)
var ViewStyles.textColor: RGBA by ViewStyle(Colors.WHITE)
var ViewStyles.buttonBackColor: RGBA by ViewStyle(Colors.DARKGRAY)
var ViewStyles.textAlignment: TextAlignment by ViewStyle(TextAlignment.TOP_LEFT)

class ViewStyle<T>(val default: T) {
    inline operator fun getValue(styles: ViewStyles, property: KProperty<*>): T {
        return styles.getProp(property, default) as? T? ?: error("Can't cast $property to T")
    }

    inline operator fun setValue(styles: ViewStyles, property: KProperty<*>, value: T) {
        if (styles.data == null) styles.data = linkedHashMapOf()
        styles.data!![property.name] = value
        styles.doUpdate()
    }
}
