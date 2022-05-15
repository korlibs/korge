package com.soywiz.korge.ui

import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.append
import com.soywiz.korge.view.size
import com.soywiz.korim.color.RGBAf
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.unaryMinus
import com.soywiz.korma.geom.unaryPlus
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0

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
    title: String, vararg propsList: UIEditableBooleanProps,
    width: Double = 128.0, height: Double = 20.0,
): UIPropertyRow = append(UIPropertyRow(title, width, height)) {
    for (props in propsList) {
        this.container.append(uiCheckBox(checked = props.value, text = "").also { checkBox ->
            props.getDisplayValue = { checkBox.checked }
            props.onChanged { prop, value -> checkBox.checked = value }
            checkBox.onChange {
                props.setValue(checkBox.checked, notify = false)
            }
        })
    }
}

@KorgeExperimental
fun <T : UIObservableProperty<*>> Array<T>.register(list: UIObservablePropertyList): Array<T> {
    list.register(this.toList())
    return this
}

@KorgeExperimental
fun UIEditableNumberPropsList(vararg mut: KMutableProperty0<Double>, min: Double = 0.0, max: Double = 1.0, decimals: Int = 2, clamped: Boolean = true): Array<UIEditableNumberProps> {
    return mut.map { mut ->
        UIEditableNumberProps(mut.toUI(), min, max, decimals, clamped)
    }.toTypedArray()
}

@KorgeExperimental
fun UIEditableIntPropsList(vararg mut: KMutableProperty0<Int>, min: Int = 0, max: Int = 1000): Array<UIEditableNumberProps> {
    return mut.map { mut ->
        UIEditableNumberProps(UIProperty(set = { mut.set(it.toInt()) }, get = { mut.get().toDouble() }), min.toDouble(), max.toDouble(), 0, true)
    }.toTypedArray()
}

@KorgeExperimental
fun UIEditableAnglePropsList(vararg mut: KMutableProperty0<Angle>, min: Angle = -360.degrees, max: Angle = +360.degrees, clamped: Boolean = true): Array<UIEditableNumberProps> {
    return mut.map { mut ->
        UIEditableNumberProps(UIProperty(set = { mut.set(it.degrees) }, get = { mut.get().degrees }), min.degrees, max.degrees, 0, clamped)
    }.toTypedArray()
}

@KorgeExperimental
fun UIEditableColorPropsList(prop: KProperty0<RGBAf>): Array<UIEditableNumberProps> {
    return UIEditableNumberPropsList(prop.get()::rd, prop.get()::gd, prop.get()::bd, prop.get()::ad, min = 0.0, max = 1.0)
}

@KorgeExperimental
fun UIEditablePointPropsList(prop: KProperty0<Point>, min: Double = -1000.0, max: Double = +1000.0): Array<UIEditableNumberProps> {
    return UIEditableNumberPropsList(prop.get()::x, prop.get()::y, min = min, max = max)
}

@KorgeExperimental
fun UIEditableBooleanPropsList(prop: KMutableProperty0<Boolean>): Array<UIEditableBooleanProps> {
    return arrayOf(UIEditableBooleanProps(prop.toUI()))
}

@KorgeExperimental
open class UIPropertyNumberRow(title: String, vararg propsList: UIEditableNumberProps, width: Double = 128.0, height: Double = 20.0) : UIPropertyRow(title, width, height) {
    init {
        container.apply {
            for (props in propsList) {
                uiEditableNumber(props.value, props.min, props.max, props.decimals, props.clamped) {
                    val editableNumber = this
                    props.getDisplayValue = { editableNumber.value }
                    onSetValue { props.setValue(it.value, notify = false) }
                    props.onChanged { prop, value -> if (!editableNumber.isEditing) editableNumber.value = value }
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
