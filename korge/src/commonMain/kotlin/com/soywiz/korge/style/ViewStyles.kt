package com.soywiz.korge.style

import com.soywiz.kds.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.text.*
import kotlin.reflect.*

val View.styles: ViewStyles by Extra.PropertyThis { ViewStyles(this) }

inline fun <T : View> T.styles(block: ViewStyles.() -> Unit): T {
    styles.apply(block)
    return this
}

class ViewStyles(val view: View) {
    @PublishedApi internal var data: LinkedHashMap<KProperty<*>, Any>? = null

    fun <T> getProp(prop: KProperty<T>, default: T): T =
        (data?.get(prop) as? T?) ?: (view.parent as? UIView)?.styles?.getProp(prop, default) ?: default
}

var ViewStyles.textFont: Font by ViewStyle(DefaultTtfFontAsBitmap)
var ViewStyles.textSize: Double by ViewStyle(16.0)
var ViewStyles.textColor: RGBA by ViewStyle(Colors.WHITE)
var ViewStyles.buttonBackColor: RGBA by ViewStyle(Colors.DARKGRAY)
var ViewStyles.textAlignment: TextAlignment by ViewStyle(TextAlignment.TOP_LEFT)

inline class ViewStyle<T>(val default: T) {
    inline operator fun getValue(styles: ViewStyles, property: KProperty<*>): T {
        return styles.getProp(property, default) as T
    }

    inline operator fun setValue(styles: ViewStyles, property: KProperty<*>, t: T) {
        if (styles.data == null) styles.data = linkedHashMapOf()
        styles.data!![property] = property
    }
}
