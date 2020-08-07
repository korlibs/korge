package com.soywiz.korge.debug

import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korui.*
import kotlin.reflect.*

fun <T> UiContainer.uiEditableValue(
    prop: KMutableProperty0<T>,
    name: String = prop.name,
    values: List<T>,
): UiRowEditableValue {
    val obs = ObservableProperty(name, internalSet = { prop.set(it) }, internalGet = { prop.get() })
    return UiRowEditableValue(app, name, UiListEditableValue<T>(app, values, obs)).also { addChild(it) }
}

fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<RGBA>,
    name: String = prop.name
): UiRowEditableValue {
    val obs = ObservableProperty(name, internalSet = { prop.set(Colors[it]) }, internalGet = { prop.get().hexString })
    return UiRowEditableValue(app, name, UiTextEditableValue(app, obs, UiTextEditableValue.Kind.COLOR)).also { addChild(it) }
}

fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<Double>,
    name: String = prop.name,
    min: Double = -1.0,
    max: Double = +1.0,
    clamp: Boolean = true,
    clampMin: Boolean = clamp,
    clampMax: Boolean = clamp,
    decimalPlaces: Int = 2
): UiRowEditableValue {
    val obs = ObservableProperty(name, internalSet = { prop.set(it) }, internalGet = { prop.get() })
    return UiRowEditableValue(app, name, UiNumberEditableValue(app, obs, min, max, clampMin, clampMax, decimalPlaces)).also { addChild(it) }
}

fun UiContainer.uiEditableValue(
    props: Pair<KMutableProperty0<Double>, KMutableProperty0<Double>>,
    name: String = "pair",
    min: Double = -1.0,
    max: Double = +1.0,
    clamp: Boolean = true,
    clampMin: Boolean = clamp,
    clampMax: Boolean = clamp,
    decimalPlaces: Int = 2
): UiRowEditableValue {
    val obs1 = ObservableProperty(props.first.name, internalSet = { props.first.set(it) }, internalGet = { props.first.get() })
    val obs2 = ObservableProperty(props.second.name, internalSet = { props.second.set(it) }, internalGet = { props.second.get() })
    return UiRowEditableValue(
        app, name,
        UiTwoItemEditableValue(app,
            UiNumberEditableValue(app, obs1, min, max, clampMin, clampMax, decimalPlaces),
            UiNumberEditableValue(app, obs2, min, max, clampMin, clampMax, decimalPlaces),
        )).also { addChild(it) }
}

fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<String?>,
    name: String = prop.name,
    kind: UiTextEditableValue.Kind = UiTextEditableValue.Kind.STRING,
): UiRowEditableValue {
    val obs = ObservableProperty(
        name,
        internalSet = { prop.set(it.takeIf { it.isNotEmpty() }) },
        internalGet = { prop.get() ?: "" }
    )
    return UiRowEditableValue(app, name, UiTextEditableValue(app, obs, kind)).also { addChild(it) }
}
