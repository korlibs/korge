package com.soywiz.korge.debug

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.color.*
import com.soywiz.korim.text.*
import com.soywiz.korio.file.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import com.soywiz.korui.layout.*
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
    class ActionWithProperty(val func: KFunction<*>, val viewProp: ViewProperty, val clazz: KClass<*>) {
        val order: Int get() = viewProp.order
        val name: String get() = viewProp.name.takeIf { it.isNotBlank() } ?: func.name
    }

    fun createPropsForInstance(instance: Any?, outputContainer: UiContainer) {
        if (instance == null) return

        val allProps = arrayListOf<PropWithProperty>()
        val allActions = arrayListOf<ActionWithProperty>()

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
            for (func in clazz.declaredMemberFunctions) {
                val viewProp = func.findAnnotation<ViewProperty>()
                if (viewProp != null) {
                    func.isAccessible = true
                    allActions.add(ActionWithProperty(func, viewProp, clazz))
                }
            }

            for (sup in clazz.superclasses) {
                findAllProps(sup, explored)
            }
        }
        findAllProps(instance::class)

        val allPropsByClazz = allProps.groupBy { it.clazz }
        val allActionsByClazz = allActions.groupBy { it.clazz }

        val classes = allPropsByClazz.keys + allActionsByClazz.keys

        for (clazz in classes) {
            outputContainer.uiCollapsibleSection("${clazz.simpleName}") {
                val propWithProperties = allPropsByClazz[clazz]
                val actionWithProperties = allActionsByClazz[clazz]
                if (actionWithProperties != null) {
                    for ((groupName, eactions) in actionWithProperties.groupBy { it.viewProp.groupName }) {
                        for (eaction in eactions.multisorted(ActionWithProperty::order, ActionWithProperty::name)) {
                            outputContainer.addChild(UiButton(app).also {
                                it.text = eaction.name
                                it.onClick {
                                    (eaction.func as (Any.() -> Unit)).invoke(instance)
                                }
                            })
                        }
                    }
                }
                if (propWithProperties != null) {
                    for ((groupName, eprops) in propWithProperties.groupBy { it.viewProp.groupName }) {
                        for (eprop in eprops.multisorted(PropWithProperty::order, PropWithProperty::name)) {
                            val name = eprop.name
                            val prop = eprop.prop
                            val viewProp = eprop.viewProp
                            try {
                                val res = createUiEditableValueFor(instance, prop.returnType, viewProp, prop as KProperty1<View, *>, null)
                                val item = res ?: UiLabel(app).also { it.text = "<UNSUPPORTED TYPE>" }
                                if (item is UiEditableValue<*> || item is UiLabel) {
                                    addChild(UiRowEditableValue(app, name, item))
                                } else {
                                    addChild(item)
                                }
                            } catch (e: Throwable) {
                                e.printStackTrace()
                                addChild(UiRowEditableValue(app, prop.name, UiLabel(app).also { it.text = "<EXCEPTION>" }))
                            }
                        }
                    }
                }
            }
        }
    }

    fun setViewBase(view: View?) {
        propsContainer.removeChildren()
        currentView = view
        if (view != null) {
            createPropsForInstance(view, propsContainer)
        }
        //view?.buildDebugComponent(views, this@UiEditProperties.propsContainer)
    }

    fun createUiEditableValueFor(instance: Any, type: KType, viewProp: ViewProperty, prop: KProperty1<View, Any?>?, obs: ObservableProperty<*>? = null): UiComponent? {
        val name = prop?.name ?: "Unknown"
        val obs = obs ?: ObservableProperty<Any?>(
            name,
            internalSet = { (prop as KMutableProperty1<Any, Any?>).set(instance, it) },
            internalGet = { (prop as KMutableProperty1<Any, Any?>).get(instance) }
        )
        return when {
            type.isSubtypeOf(Pair::class.starProjectedType) -> {
                val rprop = prop as KMutableProperty1<Any, Pair<Any, Any>>
                val vv = listOf(
                    ObservableProperty("x", { rprop.set(instance, rprop.get(instance).copy(first = it)) }, { rprop.get(instance).first }),
                    ObservableProperty("y", { rprop.set(instance, rprop.get(instance).copy(second = it)) }, { rprop.get(instance).second }),
                ).mapIndexed { index, obs ->
                    createUiEditableValueFor(instance, type.arguments[index].type!!, viewProp, prop, obs) as UiEditableValue<Any>
                }
                UiTwoItemEditableValue(app, vv[0], vv[1])
            }
            type.isSubtypeOf(IPoint::class.starProjectedType) -> {
                @Suppress("UNCHECKED_CAST")
                prop as KMutableProperty1<Any, IPoint>
                val vv = listOf(
                    ObservableProperty("x", { prop.set(instance, prop.get(instance).copy(x = it)) }, { prop.get(instance).x }),
                    ObservableProperty("y", { prop.set(instance, prop.get(instance).copy(y = it)) }, { prop.get(instance).y }),
                ).map { UiNumberEditableValue(app, it, viewProp.min, viewProp.max, viewProp.clampMin, viewProp.clampMax, viewProp.decimalPlaces) }

                UiTwoItemEditableValue(app, vv[0], vv[1])
            }
            type.isSubtypeOf(RectCorners::class.starProjectedType) -> {
                @Suppress("UNCHECKED_CAST")
                prop as KMutableProperty1<Any, RectCorners>
                val vv = listOf(
                    ObservableProperty("a", { prop.set(instance, prop.get(instance).duplicate(topLeft = it)) }, { prop.get(instance).topLeft }),
                    ObservableProperty("b", { prop.set(instance, prop.get(instance).duplicate(topRight = it)) }, { prop.get(instance).topRight }),
                    ObservableProperty("c", { prop.set(instance, prop.get(instance).duplicate(bottomRight = it)) }, { prop.get(instance).bottomRight }),
                    ObservableProperty("d", { prop.set(instance, prop.get(instance).duplicate(bottomLeft = it)) }, { prop.get(instance).bottomLeft }),
                ).map { UiNumberEditableValue(app, it, viewProp.min, viewProp.max, viewProp.clampMin, viewProp.clampMax, viewProp.decimalPlaces) }

                UiFourItemEditableValue(app, vv[0], vv[1], vv[2], vv[3])
            }
            type.isSubtypeOf(Margin::class.starProjectedType) -> {
                @Suppress("UNCHECKED_CAST")
                prop as KMutableProperty1<Any, Margin>
                val vv = listOf(
                    ObservableProperty("a", { prop.set(instance, prop.get(instance).duplicate(top = it)) }, { prop.get(instance).top }),
                    ObservableProperty("b", { prop.set(instance, prop.get(instance).duplicate(right = it)) }, { prop.get(instance).right }),
                    ObservableProperty("c", { prop.set(instance, prop.get(instance).duplicate(bottom = it)) }, { prop.get(instance).bottom }),
                    ObservableProperty("d", { prop.set(instance, prop.get(instance).duplicate(left = it)) }, { prop.get(instance).left }),
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
                    prop as KProperty1<Any, String?>
                    UiLabel(app).also { it.text = prop.get(instance) ?: "" }
                } else {
                    val pobs = obs as ObservableProperty<String?>
                    val robs = ObservableProperty<String>(
                        name,
                        internalSet = { pobs.value = if (it.isEmpty()) null else it },
                        internalGet = { pobs.value ?: "" }
                    )

                    val fileRef = prop?.findAnnotation<ViewPropertyFileRef>()
                    val kind = when {
                        fileRef != null -> {
                            UiTextEditableValue.Kind.FILE(views.currentVfs) {
                                for (ext in fileRef.extensions) {
                                    if (it.extension.endsWith(".$ext", ignoreCase = true)) return@FILE true
                                }
                                false
                            }
                        }
                        else -> {
                            UiTextEditableValue.Kind.STRING
                        }
                    }
                    UiTextEditableValue(app, robs, kind)
                }
            }
            type.isSubtypeOf(RGBA::class.starProjectedType) -> {
                val pobs = obs as ObservableProperty<RGBA>
                val robs = ObservableProperty(name, internalSet = { pobs.value = Colors[it] }, internalGet = { pobs.value.hexString })
                UiTextEditableValue(app, robs, UiTextEditableValue.Kind.COLOR)
            }
            type.isSubtypeOf(RichTextData::class.starProjectedType) -> {
                val pobs = obs as ObservableProperty<RichTextData>
                val robs = ObservableProperty(name, internalSet = { pobs.value = RichTextData.fromHTML(it) }, internalGet = { pobs.value.toHTML() })
                UiTextEditableValue(app, robs, UiTextEditableValue.Kind.STRING)
            }
            type.isSubtypeOf(ViewActionList::class.starProjectedType) -> {
                val actionList = obs.value as ViewActionList
                UiContainer(app).also { container ->
                    container.layout = HorizontalUiLayout
                    for (action in actionList.actions) {
                        container.addChild(UiButton(app).also {
                            it.text = action.name
                            it.onClick { action.action(views, instance) }
                        })
                    }
                }
            }
            type.isSubtypeOf(Enum::class.starProjectedType) -> {
                val enumClass = type.jvmErasure as KClass<Enum<*>>
                return UiListEditableValue<Any?>(app, { enumClass.java.enumConstants.toList() }, obs as ObservableProperty<Any?>)
            }
            //type.isSubtypeOf(List::class.starProjectedType) -> {
            //    val items = obs.value as List<*>
            //    UiLabel(app).also { it.text = "<LIST>" }
            //}
            else -> {
                prop?.findAnnotation<ViewPropertySubTree>()?.let { subTree ->
                    return UiContainer(app).also {
                        createPropsForInstance(obs.value, it)
                    }
                }

                prop?.findAnnotation<ViewPropertyProvider>()?.let { propertyProvider ->
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

fun UiComponent.findObservableProperties(out: ArrayList<ObservableProperty<*>> = arrayListOf()): List<ObservableProperty<*>> {
    if (this is ObservablePropertyHolder<*>) {
        out.add(prop)
    }
    if (this is UiContainer) {
        forEachChild {
            it.findObservableProperties(out)
        }
    }
    return out
}
