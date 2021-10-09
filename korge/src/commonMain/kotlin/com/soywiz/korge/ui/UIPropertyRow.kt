package com.soywiz.korge.ui

import com.soywiz.korge.annotations.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.reflect.*

class UIEditableNumberProps(
    val value: Double = 0.0,
    val min: Double = 0.0,
    val max: Double = 1.0,
    val decimals: Int = 2,
    val clamped: Boolean = true,
    val onChange: (Double) -> Unit = { }
) {
}

@KorgeExperimental
inline fun Container.uiPropertyNumberRow(
    title: String, vararg propsList: UIEditableNumberProps,
    width: Double = 128.0, height: Double = 20.0,
    block: @ViewDslMarker UIPropertyNumberRow.() -> Unit = {},
): UIPropertyNumberRow = append(UIPropertyNumberRow(title, *propsList, width = width, height = height)).apply(block)


@KorgeExperimental
inline fun <T> Container.uiPropertyComboBox(
    title: String, field: KMutableProperty0<T>, values: List<T>,
    width: Double = 128.0, height: Double = 20.0,
): UIPropertyRow = UIPropertyRow(title, width, height).also {
    it.container.apply {
        val comboBox = uiComboBox(items = values, selectedIndex = values.indexOf(field.get()))
        comboBox.onSelectionUpdate {
            it.selectedItem?.let { field.set(it) }
        }
    }
}.addTo(this)

@KorgeExperimental
inline fun <reified T : Enum<T>> Container.uiPropertyComboBox(
    title: String, field: KMutableProperty0<T>,
    width: Double = 128.0, height: Double = 20.0,
): UIPropertyRow = uiPropertyComboBox(title, field, enumValues<T>().toList(), width, height)

@KorgeExperimental
inline fun Container.uiPropertyCheckBox(
    title: String, field: KMutableProperty0<Boolean>,
    width: Double = 128.0, height: Double = 20.0,
): UIPropertyRow = append(UIPropertyRow(title, width, height)) {
    this.container.append(uiCheckBox(checked = field.get(), text = "").also {
        it.onChange {
            field.set(it.checked)
        }
    })
}


fun UIEditableNumberPropsList(vararg mut: KMutableProperty0<Double>, min: Double = 0.0, max: Double = 1.0, decimals: Int = 2, clamped: Boolean = true): Array<UIEditableNumberProps> {
    return mut.map { mut ->
        UIEditableNumberProps(mut.get(), min, max, decimals, clamped) { mut.set(it) }
    }.toTypedArray()
}

fun UIEditableIntPropsList(vararg mut: KMutableProperty0<Int>, min: Int = 0, max: Int = 1000): Array<UIEditableNumberProps> {
    return mut.map { mut ->
        UIEditableNumberProps(mut.get().toDouble(), min.toDouble(), max.toDouble(), 0, true) { mut.set(it.toInt()) }
    }.toTypedArray()
}

fun UIEditableAnglePropsList(vararg mut: KMutableProperty0<Angle>, min: Angle = -360.degrees, max: Angle = +360.degrees, clamped: Boolean = true): Array<UIEditableNumberProps> {
    return mut.map { mut ->
        UIEditableNumberProps(mut.get().degrees, min.degrees, max.degrees, 0, clamped) { mut.set(it.degrees) }
    }.toTypedArray()
}

fun UIEditableColorPropsList(prop: KProperty0<RGBAf>): Array<UIEditableNumberProps> {
    return UIEditableNumberPropsList(prop.get()::rd, prop.get()::gd, prop.get()::bd, prop.get()::ad, min = 0.0, max = 1.0)
}

fun UIEditablePointPropsList(prop: KProperty0<Point>, min: Double = -1000.0, max: Double = +1000.0): Array<UIEditableNumberProps> {
    return UIEditableNumberPropsList(prop.get()::x, prop.get()::y, min = min, max = max)
}

@KorgeExperimental
open class UIPropertyNumberRow(title: String, vararg propsList: UIEditableNumberProps, width: Double = 128.0, height: Double = 20.0) : UIPropertyRow(title, width, height) {
    init {
        container.apply {
            for (props in propsList) {
                uiEditableNumber(props.value, props.min, props.max, props.decimals, props.clamped) {
                    onSetValue { props.onChange(it.value) }
                }
            }
        }
    }
}

@KorgeExperimental
open class UIPropertyRow(val title: String, width: Double = 128.0, height: Double = 20.0) : UIView(width, height) {
    lateinit var container: Container
    val horizontal = append(UIHorizontalFill(width, height)) {
        uiText(title)
        container = append(UIHorizontalFill(width, height)) {
            //uiEditableNumber()
            //uiEditableNumber()
        }
    }

    override fun onSizeChanged() {
        horizontal.size(width, height)
    }
}
