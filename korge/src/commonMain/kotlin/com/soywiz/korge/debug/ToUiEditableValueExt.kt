package com.soywiz.korge.debug

import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import kotlin.jvm.*
import kotlin.reflect.*

@JvmName("uiEditableValueGeneric")
fun <T> UiContainer.uiEditableValue(
    prop: KMutableProperty0<T>,
    values: () -> List<T>,
    name: String = prop.name,
): UiRowEditableValue {
    val obs = ObservableProperty(name, internalSet = { prop.set(it) }, internalGet = { prop.get() })
    return UiRowEditableValue(app, name, UiListEditableValue<T>(app, values, obs)).also { addChild(it) }
}

@JvmName("uiEditableValueRGBA")
fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<RGBA>,
    name: String = prop.name
): UiRowEditableValue {
    val obs = ObservableProperty(name, internalSet = { prop.set(Colors[it]) }, internalGet = { prop.get().hexString })
    return UiRowEditableValue(app, name, UiTextEditableValue(app, obs, UiTextEditableValue.Kind.COLOR)).also { addChild(it) }
}

@JvmName("uiEditableValueRGBAf")
fun UiContainer.uiEditableValue(
    name: String,
    prop: RGBAf
) {
    uiCollapsableSection(name) {
        uiEditableValue(prop::rd)
        uiEditableValue(prop::gd)
        uiEditableValue(prop::bd)
        uiEditableValue(prop::ad)
    }
}

@JvmName("uiEditableValuePoint")
fun UiContainer.uiEditableValue(
    name: String,
    prop: Point
) {
    uiEditableValue(Pair(prop::x, prop::y), name = name)
}

@JvmName("uiEditableValueDouble")
fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<Double>,
    min: Double = -1.0,
    max: Double = +1.0,
    clamp: Boolean = true,
    clampMin: Boolean = clamp,
    clampMax: Boolean = clamp,
    decimalPlaces: Int = 2,
    name: String = prop.name,
): UiRowEditableValue {
    val obs = ObservableProperty(name, internalSet = { prop.set(it) }, internalGet = { prop.get() })
    return UiRowEditableValue(app, name, UiNumberEditableValue(app, obs, min, max, clampMin, clampMax, decimalPlaces)).also { addChild(it) }
}

@JvmName("uiEditableValueFloat")
fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<Float>,
    min: Float = -1f,
    max: Float = +1f,
    clamp: Boolean = true,
    clampMin: Boolean = clamp,
    clampMax: Boolean = clamp,
    decimalPlaces: Int = 2,
    name: String = prop.name,
): UiRowEditableValue {
    val obs = ObservableProperty(name, internalSet = { prop.set(it.toFloat()) }, internalGet = { prop.get().toDouble() })
    return UiRowEditableValue(app, name, UiNumberEditableValue(app, obs, min.toDouble(), max.toDouble(), clampMin, clampMax, decimalPlaces)).also { addChild(it) }
}

@JvmName("uiEditableValueAngle")
fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<Angle>,
    name: String = prop.name,
    clamp: Boolean = true
): UiRowEditableValue {
    val obs = ObservableProperty<Double>(name, internalSet = { prop.set(it.degrees) }, internalGet = { prop.get().degrees })
    return UiRowEditableValue(app, name, UiNumberEditableValue(app, obs, -360.0, +360.0, clamp, clamp, 0)).also { addChild(it) }
}

@JvmName("uiEditableValueInt")
fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<Int>,
    name: String = prop.name,
    min: Double = -1.0,
    max: Double = +1.0,
    clamp: Boolean = true,
    clampMin: Boolean = clamp,
    clampMax: Boolean = clamp,
    decimalPlaces: Int = 2
): UiRowEditableValue {
    val obs = ObservableProperty(name, internalSet = { prop.set(it.toInt()) }, internalGet = { prop.get().toDouble() })
    return UiRowEditableValue(app, name, UiNumberEditableValue(app, obs, min, max, clampMin, clampMax, decimalPlaces)).also { addChild(it) }
}

@JvmName("uiEditableValuePair")
fun UiContainer.uiEditableValue(
    props: Pair<KMutableProperty0<Double>, KMutableProperty0<Double>>,
    min: Double = -1.0,
    max: Double = +1.0,
    clamp: Boolean = false,
    clampMin: Boolean = clamp,
    clampMax: Boolean = clamp,
    decimalPlaces: Int = 2,
    name: String = "pair",
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


@JvmName("uiEditableValueStringOrNull")
fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<String?>,
    kind: UiTextEditableValue.Kind = UiTextEditableValue.Kind.STRING,
    name: String = prop.name,
): UiRowEditableValue {
    val obs = ObservableProperty(
        name,
        internalSet = { prop.set(it.takeIf { it.isNotEmpty() }) },
        internalGet = { prop.get() ?: "" }
    )
    return UiRowEditableValue(app, name, UiTextEditableValue(app, obs, kind)).also { addChild(it) }
}

@JvmName("uiEditableValueStringNullable")
fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<String>,
    kind: UiTextEditableValue.Kind = UiTextEditableValue.Kind.STRING,
    name: String = prop.name,
): UiRowEditableValue {
    val obs = ObservableProperty(
        name,
        internalSet = { prop.set(it) },
        internalGet = { prop.get() }
    )
    return UiRowEditableValue(app, name, UiTextEditableValue(app, obs, kind)).also { addChild(it) }
}

@JvmName("uiEditableValueBoolean")
fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<Boolean>,
    name: String = prop.name,
): UiRowEditableValue {
    val obs = ObservableProperty(
        name,
        internalSet = { prop.set(it) },
        internalGet = { prop.get() }
    )
    return UiRowEditableValue(app, name, UiBooleanEditableValue(app, obs)).also { addChild(it) }
}
