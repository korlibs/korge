package com.soywiz.korge.debug

import com.soywiz.korge.view.property.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import kotlin.reflect.*

@JvmName("uiEditableValueGeneric")
internal fun <T> UiContainer.uiEditableValue(
    prop: KMutableProperty0<T>,
    values: () -> List<T>,
    name: String = prop.name,
): UiRowEditableValue {
    val obs = ObservableProperty(name, internalSet = { prop.set(it) }, internalGet = { prop.get() })
    return UiRowEditableValue(app, name, UiListEditableValue<T>(app, values, obs)).also { addChild(it) }
}

@JvmName("uiEditableValueEnum")
internal inline fun <reified T : Enum<T>> UiContainer.uiEditableValue(
    prop: KMutableProperty0<T>,
    name: String = prop.name,
): UiRowEditableValue {
    return uiEditableValue(prop, { enumValues<T>().toList() }, name)
}

@JvmName("uiEditableValueEnumLike")
internal inline fun <reified T : EnumLike<T>> UiContainer.uiEditableValue(
    prop: KMutableProperty0<T>,
    name: String = prop.name,
): UiRowEditableValue {
    return uiEditableValue(prop, { EnumLike.getValues(prop.get()) }, name)
}

@JvmName("uiEditableValueGeneric")
internal fun <T> UiContainer.uiEditableValue(
    prop: KMutableProperty0<T>,
    values: List<T>,
    name: String = prop.name,
): UiRowEditableValue = uiEditableValue(prop, { values }, name)

@JvmName("uiEditableValueRGBA")
internal fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<RGBA>,
    name: String = prop.name
): UiRowEditableValue {
    val obs = ObservableProperty(name, internalSet = { prop.set(Colors[it]) }, internalGet = { prop.get().hexString })
    return UiRowEditableValue(app, name, UiTextEditableValue(app, obs, UiTextEditableValue.Kind.COLOR)).also { addChild(it) }
}

@JvmName("uiEditableValueRGBAf")
internal fun UiContainer.uiEditableValue(
    name: String,
    prop: RGBAf
) {
    uiEditableValue(listOf(prop::rd, prop::gd, prop::bd, prop::ad), name = name)
}

@JvmName("uiEditableValuePoint")
internal fun UiContainer.uiEditableValue(
    name: String,
    prop: Point
) {
    uiEditableValue(Pair(prop::x, prop::y), name = name)
}

@JvmName("uiEditableValueDouble")
internal fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<Double>,
    min: Double = -1.0,
    max: Double = +1.0,
    clamp: Boolean = false,
    clampMin: Boolean = clamp,
    clampMax: Boolean = clamp,
    decimalPlaces: Int = 2,
    name: String = prop.name,
): UiRowEditableValue {
    val obs = ObservableProperty(name, internalSet = { prop.set(it) }, internalGet = { prop.get() })
    return UiRowEditableValue(app, name, UiNumberEditableValue(app, obs, min, max, clampMin, clampMax, decimalPlaces)).also { addChild(it) }
}

@JvmName("uiEditableValueAngle")
internal fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<Angle>,
    name: String = prop.name,
): UiRowEditableValue {
    val obs = ObservableProperty(name, internalSet = { prop.set(it.degrees) }, internalGet = { prop.get().degrees })
    return UiRowEditableValue(app, name, UiNumberEditableValue(app, obs, -360.0, +360.0, true, true, 2)).also { addChild(it) }
}

@JvmName("uiEditableValueAnglePair")
internal fun UiContainer.uiEditableValue(
    props: Pair<KMutableProperty0<Angle>, KMutableProperty0<Angle>>,
    name: String = props.first.name,
): UiRowEditableValue {
    val obs1 = ObservableProperty(props.first.name, internalSet = { props.first.set(it.degrees) }, internalGet = { props.first.get().degrees })
    val obs2 = ObservableProperty(props.second.name, internalSet = { props.second.set(it.degrees) }, internalGet = { props.second.get().degrees })
    return UiRowEditableValue(
        app, name,
        UiTwoItemEditableValue(app,
            UiNumberEditableValue(app, obs1,  -360.0, +360.0, true, true, 2),
            UiNumberEditableValue(app, obs2,  -360.0, +360.0, true, true, 2),
        )).also { addChild(it) }
}

@JvmName("uiEditableValueFloat")
internal fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<Float>,
    min: Float = -1f,
    max: Float = +1f,
    clamp: Boolean = false,
    clampMin: Boolean = clamp,
    clampMax: Boolean = clamp,
    decimalPlaces: Int = 2,
    name: String = prop.name,
): UiRowEditableValue {
    val obs = ObservableProperty(name, internalSet = { prop.set(it.toFloat()) }, internalGet = { prop.get().toDouble() })
    return UiRowEditableValue(app, name, UiNumberEditableValue(app, obs, min.toDouble(), max.toDouble(), clampMin, clampMax, decimalPlaces)).also { addChild(it) }
}

@JvmName("uiEditableValueAngle")
internal fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<Angle>,
    name: String = prop.name,
    clamp: Boolean = true
): UiRowEditableValue {
    val obs = ObservableProperty<Double>(name, internalSet = { prop.set(it.degrees) }, internalGet = { prop.get().degrees })
    return UiRowEditableValue(app, name, UiNumberEditableValue(app, obs, -360.0, +360.0, clamp, clamp, 0)).also { addChild(it) }
}

@JvmName("uiEditableValueInt")
internal fun UiContainer.uiEditableValue(
    prop: KMutableProperty0<Int>,
    name: String = prop.name,
    min: Int = -1000,
    max: Int = +1000,
    clamp: Boolean = false,
    clampMin: Boolean = clamp,
    clampMax: Boolean = clamp
): UiRowEditableValue {
    val obs = ObservableProperty(name, internalSet = { prop.set(it.toInt()) }, internalGet = { prop.get().toDouble() })
    return UiRowEditableValue(app, name, UiNumberEditableValue(app, obs, min.toDouble(), max.toDouble(), clampMin, clampMax, 0)).also { addChild(it) }
}

@JvmName("uiEditableValuePair")
internal fun UiContainer.uiEditableValue(
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

/*
@JvmName("uiEditableValuePoint")
fun UiContainer.uiEditableValue(
    props: KMutableProperty0<IPoint>,
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
*/

@JvmName("uiEditableValueTuple")
internal fun UiContainer.uiEditableValue(
    props: List<KMutableProperty0<Double>>,
    min: Double = -1.0,
    max: Double = +1.0,
    clamp: Boolean = false,
    clampMin: Boolean = clamp,
    clampMax: Boolean = clamp,
    decimalPlaces: Int = 2,
    name: String = props.first().name,
): UiRowEditableValue {
    val obs = props.map { prop -> ObservableProperty(prop.name, internalSet = { prop.set(it) }, internalGet = { prop.get() }) }
    return UiRowEditableValue(
        app, name,
        UiMultipleItemEditableValue(app, obs.map { UiNumberEditableValue(app, it, min, max, clampMin, clampMax, decimalPlaces) })
    ).also { addChild(it) }
}

@JvmName("uiEditableValueTupleFloat")
internal fun UiContainer.uiEditableValue(
    props: List<KMutableProperty0<Float>>,
    min: Float = -1f,
    max: Float = +1f,
    clamp: Boolean = false,
    clampMin: Boolean = clamp,
    clampMax: Boolean = clamp,
    decimalPlaces: Int = 2,
    name: String = props.first().name,
): UiRowEditableValue {
    val obs = props.map { prop -> ObservableProperty(prop.name, internalSet = { prop.set(it.toFloat()) }, internalGet = { prop.get().toDouble() }) }
    return UiRowEditableValue(
        app, name,
        UiMultipleItemEditableValue(app, obs.map { UiNumberEditableValue(app, it, min.toDouble(), max.toDouble(), clampMin, clampMax, decimalPlaces) })
    ).also { addChild(it) }
}

@JvmName("uiEditableValueTupleInt")
internal fun UiContainer.uiEditableValue(
    props: List<KMutableProperty0<Int>>,
    min: Int = -100,
    max: Int = +100,
    clamp: Boolean = false,
    clampMin: Boolean = clamp,
    clampMax: Boolean = clamp,
    name: String = props.first().name,
): UiRowEditableValue {
    val obs = props.map { prop -> ObservableProperty(prop.name, internalSet = { prop.set(it.toInt()) }, internalGet = { prop.get().toDouble() }) }
    return UiRowEditableValue(
        app, name,
        UiMultipleItemEditableValue(app, obs.map { UiNumberEditableValue(app, it, min.toDouble(), max.toDouble(), clampMin, clampMax, 0) })
    ).also { addChild(it) }
}


@JvmName("uiEditableValueTupleAngle")
internal fun UiContainer.uiEditableValue(
    props: List<KMutableProperty0<Angle>>,
    name: String = props.first().name,
): UiRowEditableValue {
    val obs = props.map { prop -> ObservableProperty(prop.name, internalSet = { prop.set(it.degrees) }, internalGet = { prop.get().degrees }) }
    return UiRowEditableValue(
        app, name,
        UiMultipleItemEditableValue(app, obs.map { UiNumberEditableValue(app, it, -360.0, +360.0, true, true, 0) })
    ).also { addChild(it) }
}

@JvmName("uiEditableValueStringOrNull")
internal fun UiContainer.uiEditableValue(
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
internal fun UiContainer.uiEditableValue(
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
internal fun UiContainer.uiEditableValue(
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
