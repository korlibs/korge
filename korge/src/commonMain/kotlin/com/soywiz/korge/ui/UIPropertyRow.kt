package com.soywiz.korge.ui

import com.soywiz.korge.annotations.*
import com.soywiz.korge.view.*
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

fun UIEditableNumberPropsList(vararg mut: KMutableProperty0<Double>, min: Double = 0.0, max: Double = 1.0, decimals: Int = 2, clamped: Boolean = true): Array<UIEditableNumberProps> {
    return mut.map { mut ->
        UIEditableNumberProps(mut.get(), min, max, decimals, clamped) { mut.set(it) }
    }.toTypedArray()
}

@KorgeExperimental
inline fun Container.uiPropertyNumberRow(
    title: String, vararg propsList: UIEditableNumberProps,
    width: Double = 128.0, height: Double = 20.0,
    block: @ViewDslMarker UIPropertyNumberRow.() -> Unit = {},
): UIPropertyNumberRow = UIPropertyNumberRow(title, *propsList, width = width, height = height)
    .addTo(this).also { block(it) }

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
