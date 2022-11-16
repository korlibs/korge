package com.soywiz.korge.debug

import com.soywiz.kds.*
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.property.*
import com.soywiz.korim.color.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import com.soywiz.korui.layout.UiFillLayout
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

class UiEditProperties(app: UiApplication, view: View?, val views: Views) : UiContainer(app) {
    val propsContainer = scrollPanel(xbar = false)
    var currentView: View? = null

    fun setView(view: View?) {
        setViewBase(view)

        root?.updateUI()
        root?.relayout()
        root?.repaintAll()

        update()
    }

    class PropWithProperty(val prop: KProperty<*>, val viewProp: ViewProperty, val clazz: KClass<*>) {
        val order: Int get() = viewProp.order
        val name: String get() = viewProp.name.takeIf { it.isNotBlank() } ?: prop.name
    }

    fun setViewBase(view: View?) {
        propsContainer.removeChildren()
        currentView = view
        if (view != null) {
            val allProps = arrayListOf<PropWithProperty>()
            fun findAllProps(clazz: KClass<*>, explored: MutableSet<KClass<*>> = mutableSetOf()) {
                if (clazz in explored) return
                explored += clazz
                //println("findAllProps.explored: clazz=$clazz")

                for (prop in clazz.declaredMemberProperties) {
                    val viewProp = prop.findAnnotation<ViewProperty>()
                    if (viewProp != null) {
                        prop.isAccessible = true
                        allProps.add(PropWithProperty(prop, viewProp, clazz))
                    }
                }

                for (sup in clazz.superclasses) {
                    findAllProps(sup, explored)
                }
            }
            findAllProps(view::class)

            for ((clazz, epropsAll) in allProps.groupBy { it.clazz }) {
                propsContainer.uiCollapsibleSection("${clazz.simpleName}") {
                    for ((groupName, eprops) in epropsAll.groupBy { it.viewProp.groupName }) {
                        for (eprop in eprops.multisorted(PropWithProperty::order, PropWithProperty::name)) {
                            val name = eprop.name
                            val prop = eprop.prop
                            val viewProp = eprop.viewProp
                            try {
                                val res = createUiEditableValueFor(view, prop.returnType, viewProp, prop as KProperty1<View, *>, null)
                                addChild(UiRowEditableValue(
                                    app, name,
                                    res ?: UiLabel(app).also { it.text = "<UNSUPPORTED TYPE>" }
                                ))
                            } catch (e: Throwable) {
                                e.printStackTrace()
                                addChild(UiRowEditableValue(app, prop.name, UiLabel(app).also { it.text = "<EXCEPTION>" }))
                            }
                        }
                    }
                }
            }
        }
        view?.buildDebugComponent(views, this@UiEditProperties.propsContainer)
    }

    fun createUiEditableValueFor(view: View, type: KType, viewProp: ViewProperty, prop: KProperty1<View, Any?>?, obs: ObservableProperty<*>? = null): UiComponent? {
        val name = prop?.name ?: "Unknown"
        val obs = obs ?: ObservableProperty<Any?>(
            name,
            internalSet = { (prop as KMutableProperty1<View, Any?>).set(view, it) },
            internalGet = { prop?.get(view) }
        )
        return when {
            type.isSubtypeOf(Pair::class.starProjectedType) -> {
                val rprop = prop as KMutableProperty1<View?, Pair<Any, Any>>
                val vv = listOf(
                    ObservableProperty("x", { rprop.set(view, rprop.get(view).copy(first = it)) }, { rprop.get(view).first }),
                    ObservableProperty("y", { rprop.set(view, rprop.get(view).copy(second = it)) }, { rprop.get(view).second }),
                ).mapIndexed { index, obs ->
                    createUiEditableValueFor(view, type.arguments[index].type!!, viewProp, prop, obs) as UiEditableValue<Any>
                }
                UiTwoItemEditableValue(app, vv[0], vv[1])
            }
            type.isSubtypeOf(IPoint::class.starProjectedType) -> {
                @Suppress("UNCHECKED_CAST")
                prop as KMutableProperty1<View, IPoint>
                val vv = listOf(
                    ObservableProperty("x", { prop.set(view, prop.get(view).copy(x = it)) }, { prop.get(view).x }),
                    ObservableProperty("y", { prop.set(view, prop.get(view).copy(y = it)) }, { prop.get(view).y }),
                ).map { UiNumberEditableValue(app, it, viewProp.min, viewProp.max, viewProp.clampMin, viewProp.clampMax, viewProp.decimalPlaces) }

                UiTwoItemEditableValue(app, vv[0], vv[1])
            }
            type.isSubtypeOf(RectCorners::class.starProjectedType) -> {
                @Suppress("UNCHECKED_CAST")
                prop as KMutableProperty1<View, RectCorners>
                val vv = listOf(
                    ObservableProperty("a", { prop.set(view, prop.get(view).duplicate(topLeft = it)) }, { prop.get(view).topLeft }),
                    ObservableProperty("b", { prop.set(view, prop.get(view).duplicate(topRight = it)) }, { prop.get(view).topRight }),
                    ObservableProperty("c", { prop.set(view, prop.get(view).duplicate(bottomRight = it)) }, { prop.get(view).bottomRight }),
                    ObservableProperty("d", { prop.set(view, prop.get(view).duplicate(bottomLeft = it)) }, { prop.get(view).bottomLeft }),
                ).map { UiNumberEditableValue(app, it, viewProp.min, viewProp.max, viewProp.clampMin, viewProp.clampMax, viewProp.decimalPlaces) }

                UiFourItemEditableValue(app, vv[0], vv[1], vv[2], vv[3])
            }
            type.isSubtypeOf(Double::class.starProjectedType) -> {
                UiNumberEditableValue(app, obs as ObservableProperty<Double>, viewProp.min, viewProp.max, viewProp.clampMin, viewProp.clampMax, viewProp.decimalPlaces)
            }
            type.isSubtypeOf(Boolean::class.starProjectedType) -> {
                UiBooleanEditableValue(app, obs as ObservableProperty<Boolean>)
            }
            type.isSubtypeOf(Angle::class.starProjectedType) -> {
                val pobs = (obs as ObservableProperty<Angle>)
                val robs = ObservableProperty(obs.name, { pobs.value = it.degrees }, { pobs.value.degrees })
                UiNumberEditableValue(app, robs, -360.0, +360.0, true, true, 0)
            }
            type.isSubtypeOf(String::class.starProjectedType.withNullability(true)) -> {
                if (!viewProp.editable) {
                    prop as KProperty1<View, String?>
                    UiLabel(app).also { it.text = prop.get(view) ?: "" }
                } else {
                    val pobs = obs as ObservableProperty<String?>
                    val robs = ObservableProperty<String>(
                        name,
                        internalSet = { pobs.value = if (it.isEmpty()) null else it },
                        internalGet = { pobs.value ?: "" }
                    )
                    UiTextEditableValue(app, robs, UiTextEditableValue.Kind.STRING)
                }
            }
            type.isSubtypeOf(RGBA::class.starProjectedType) -> {
                val pobs = obs as ObservableProperty<RGBA>
                val robs = ObservableProperty(name, internalSet = { pobs.value = Colors[it] }, internalGet = { pobs.value.hexString })
                UiTextEditableValue(app, robs, UiTextEditableValue.Kind.COLOR)
            }
            else -> {
                val propertyProvider = prop?.findAnnotation<ViewPropertyProvider>()
                if (propertyProvider != null) {
                    val singletonClazz = propertyProvider.provider as KClass<Any>

                    for (member in singletonClazz.memberProperties) {
                        val items = member.get(singletonClazz.objectInstance!!)
                        if (items is Iterable<*>) {
                            return UiListEditableValue<Any?>(app, { (member.get(singletonClazz.objectInstance!!) as Iterable<Any?>).toList() }, obs as ObservableProperty<Any?>)
                        }
                    }
                    println("propertyProvider.provider.memberProperties=" + singletonClazz.memberProperties)
                }
                null
            }
        }
    }

    private val registeredProperties = WeakMap<ObservableProperty<*>, Boolean>()

    // Calls from time to time to update all the properties
    private var updating = false
    fun update() {
        if (updating) return
        updating = true
        try {
            //println("obsProperties[update]: $obsProperties")
            //obsProperties.fastForEach { it.forceRefresh() }
            val obsProperties = this.findObservableProperties()

            obsProperties.fastForEach {
                if (it !in registeredProperties) {
                    registeredProperties[it] = true
                    it.onChange { update() }
                }
                it.forceRefresh()
            }

        } finally {
            updating = false
        }
    }

    init {
        layout = UiFillLayout
        setView(view)
    }
}
