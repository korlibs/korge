package korlibs.korge.awt

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.event.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.io.file.*
import korlibs.io.util.*
import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
import korlibs.math.geom.Point
import korlibs.memory.*
import java.awt.*
import javax.swing.*
import kotlin.math.*
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

internal var UiApplication.views by Extra.PropertyThis<UiApplication, Views?> { null }

internal class UiEditProperties(app: UiApplication, view: View?, val views: Views) : UiContainer(app) {
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
                            addChild(UiButton(app).also {
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

    private fun setViewBase(view: View?) {
        propsContainer.removeChildren()
        currentView = view
        if (view != null) {
            createPropsForInstance(view, propsContainer)
        }
        //view?.buildDebugComponent(views, this@UiEditProperties.propsContainer)
    }

    data class Two<T>(val x: T, val y: T) {
        operator fun get(index: Int): T = when (index) {
            0 -> x; 1 -> y
            else -> TODO()
        }
        fun with(index: Int, v: T): Two<T> = when (index) {
            0 -> Two(v, y)
            1 -> Two(x, v)
            else -> TODO()
        }
    }

    data class Four<T>(val x: T, val y: T, val z: T, val w: T) {
        operator fun get(index: Int): T = when (index) {
            0 -> x; 1 -> y; 2 -> z; 3 -> w
            else -> TODO()
        }
        fun with(index: Int, v: T): Four<T> = when (index) {
            0 -> Four(v, y, z, w)
            1 -> Four(x, v, z, w)
            2 -> Four(x, y, v, w)
            3 -> Four(x, y, z, v)
            else -> TODO()
        }
    }

    private inline fun <reified T> createOne(
        noinline get: (p: Double) -> T, noinline extract: (T) -> Double,
        obs: ObservableProperty<*>,
        viewProp: ViewProperty,
        name: String = viewProp.name,
        rangeMin: Double = viewProp.min,
        rangeMax: Double = viewProp.max,
        clampMin: Boolean = viewProp.clampMin,
        clampMax: Boolean = viewProp.clampMax,
    ): UiComponent? {
        val rr = obs as ObservableProperty<T>
        val robs = ObservableProperty(name, { rr.value = get(it) }, { extract(rr.value) })
        return UiNumberEditableValue(app, robs, rangeMin, rangeMax, clampMin, clampMax, 0)
    }

    private inline fun <reified T> createPair(noinline get: (p: Two<Double>) -> T, noinline extract: (T) -> Two<Double>, instance: Any, prop: KProperty1<*, *>?, viewProp: ViewProperty, names: List<String> = listOf("x", "y")): UiComponent? {
        prop as KMutableProperty1<Any, T>
        val vv = (0 until 2).map { index ->
            ObservableProperty(names[index], { prop.set(instance, get(extract(prop.get(instance)).with(index, it))) }, { extract(prop.get(instance))[index] })
        }.map { UiNumberEditableValue(app, it, viewProp.min, viewProp.max, viewProp.clampMin, viewProp.clampMax, viewProp.decimalPlaces) }
        return UiTwoItemEditableValue(app, vv[0], vv[1])
    }

    private inline fun <reified T> createQuad(noinline get: (p: Four<Double>) -> T, noinline extract: (T) -> Four<Double>, instance: Any, prop: KProperty1<*, *>?, viewProp: ViewProperty, names: List<String> = listOf("x", "y", "z", "w")): UiComponent? {
        prop as KMutableProperty1<Any, T>
        val vv = (0 until 4).map { index ->
            ObservableProperty(names[index], { prop.set(instance, get(extract(prop.get(instance)).with(index, it))) }, { extract(prop.get(instance))[index] })
        }.map { UiNumberEditableValue(app, it, viewProp.min, viewProp.max, viewProp.clampMin, viewProp.clampMax, viewProp.decimalPlaces) }
        return UiFourItemEditableValue(app, vv[0], vv[1], vv[2], vv[3])
    }

    class WrappedValue<T>(val value: T?) {
        override fun toString(): String = if (value == null) "null" else "$value"
    }

    fun createUiEditableValueFor(instance: Any, type: KType, viewProp: ViewProperty, prop: KProperty1<View, Any?>?, obs: ObservableProperty<*>? = null): UiComponent? {
        val name = prop?.name ?: "Unknown"
        val obs: ObservableProperty<Any?> = (obs ?: ObservableProperty<Any?>(
            name,
            internalSet = { (prop as KMutableProperty1<Any, Any?>).set(instance, it) },
            internalGet = { (prop as KProperty1<Any, Any?>).get(instance) }
        )) as ObservableProperty<Any?>

        prop?.findAnnotation<ViewPropertyProvider>()?.let { propertyProvider ->
            val singletonClazz = propertyProvider.provider as KClass<Any>
            val singletonInstance = singletonClazz.objectInstance as ViewPropertyProvider.Impl<Any, Any>

            val obs2 = ObservableProperty<WrappedValue<Any?>?>(
                obs.name,
                internalSet = { obs.value = it?.value },
                internalGet = { WrappedValue(obs.value) }
            ) as ObservableProperty<Any?>

            return UiListEditableValue<Any?>(
                app,
                {
                    (singletonInstance.provider(instance).values.toList() + (if (type.isMarkedNullable) listOf(null) else emptyList()))
                        .map { WrappedValue(it) }
                },
                obs2
            )
        }

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
            type.isSubtypeOf(MPoint::class.starProjectedType) -> createPair({ MPoint(it.x, it.y) }, { Two(it.x, it.y) }, instance, prop, viewProp)
            type.isSubtypeOf(Point::class.starProjectedType) -> createPair({ Point(it.x, it.y) }, { Two(it.xD, it.yD) }, instance, prop, viewProp)
            type.isSubtypeOf(Size::class.starProjectedType) -> createPair({ Size(it.x, it.y) }, { Two(it.widthD, it.heightD) }, instance, prop, viewProp)
            type.isSubtypeOf(Scale::class.starProjectedType) -> createPair({ Scale(it.x, it.y) }, { Two(it.scaleXD, it.scaleYD) }, instance, prop, viewProp)
            type.isSubtypeOf(Anchor::class.starProjectedType) -> createPair({ Anchor(it.x, it.y) }, { Two(it.sx.toDouble(), it.sy.toDouble()) }, instance, prop, viewProp)
            type.isSubtypeOf(IntRange::class.starProjectedType) -> {
                @Suppress("UNCHECKED_CAST")
                prop as KMutableProperty1<Any, IntRange>

                val vv = listOf(
                    ObservableProperty("x", { prop.set(instance, it.toInt()..prop.get(instance).last) }, { prop.get(instance).first.toDouble() }),
                    ObservableProperty("y", { prop.set(instance, prop.get(instance).first..it.toInt()) }, { prop.get(instance).last.toDouble() }),
                ).map { UiNumberEditableValue(app, it, viewProp.min, viewProp.max, viewProp.clampMin, viewProp.clampMax, 0) }

                UiTwoItemEditableValue(app, vv[0], vv[1])
            }
            type.isSubtypeOf(RectCorners::class.starProjectedType) -> createQuad({ RectCorners(it.x, it.y, it.z, it.w) }, { Four(it.topLeft.toDouble(), it.topRight.toDouble(), it.bottomRight.toDouble(), it.bottomLeft.toDouble()) }, instance, prop, viewProp)
            type.isSubtypeOf(Margin::class.starProjectedType) -> createQuad({ Margin(it.x.toFloat(), it.y.toFloat(), it.z.toFloat(), it.w.toFloat()) }, { Four(it.top.toDouble(), it.right.toDouble(), it.bottom.toDouble(), it.left.toDouble()) }, instance, prop, viewProp)
            type.isSubtypeOf(Double::class.starProjectedType) -> UiNumberEditableValue(app, obs as ObservableProperty<Double>, viewProp.min, viewProp.max, viewProp.clampMin, viewProp.clampMax, viewProp.decimalPlaces)
            type.isSubtypeOf(Float::class.starProjectedType) -> UiNumberEditableValue(app, (obs as ObservableProperty<Float>).toDouble(), viewProp.min, viewProp.max, viewProp.clampMin, viewProp.clampMax, viewProp.decimalPlaces)
            type.isSubtypeOf(Int::class.starProjectedType) -> {
                val pobs = (obs as ObservableProperty<Int>)
                val robs = ObservableProperty(obs.name, { pobs.value = it.toInt() }, { pobs.value.toDouble() })
                UiNumberEditableValue(app, robs, viewProp.min, viewProp.max, viewProp.clampMin, viewProp.clampMax, 0)
            }
            type.isSubtypeOf(Boolean::class.starProjectedType) -> UiBooleanEditableValue(app, obs as ObservableProperty<Boolean>)
            type.isSubtypeOf(Angle::class.starProjectedType) -> createOne({ it.degrees }, { it.degreesD }, obs, viewProp, rangeMin = -360.0, rangeMax = +360.0, clampMin = true, clampMax = true)
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

internal fun UiComponent.findObservableProperties(out: ArrayList<ObservableProperty<*>> = arrayListOf()): List<ObservableProperty<*>> {
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

internal class UiBooleanEditableValue(
    app: UiApplication,
    prop: ObservableProperty<Boolean>,
) : UiEditableValue<Boolean>(app, prop), ObservablePropertyHolder<Boolean> {
    val initial = prop.value

    init {
        prop.onChange {
            //println("prop.onChange: $it")
            setValue(it, setProperty = false)
        }
    }

    val contentCheckBox = UiCheckBox(app).also { it.text = "" }.also { it.checked = prop.value }

    override fun hideEditor() {
    }

    override fun showEditor() {
    }

    fun setValue(value: Boolean, setProperty: Boolean = true) {
        if (contentCheckBox.checked != value) {
            contentCheckBox.checked = value
        }
        if (setProperty) prop.value = value
    }

    init {
        layout = HorizontalUiLayout
        addChild(contentCheckBox)
        visible = true
        contentCheckBox.onChange {
            setValue(contentCheckBox.checked)
        }
    }
}

internal fun UiContainer.uiCollapsibleSection(name: String?, block: UiContainer.() -> Unit): UiCollapsibleSection {
    return UiCollapsibleSection(app, name, block).also { addChild(it) }
}

internal class UiCollapsibleSection(app: UiApplication, val name: String?, val componentChildren: List<UiComponent>) : UiContainer(app) {
    companion object {
        operator fun invoke(app: UiApplication, name: String?, block: UiContainer.() -> Unit): UiCollapsibleSection =
            UiCollapsibleSection(app, name, listOf()).also { block(it.mycontainer) }

        private fun createIcon(angle: Angle): NativeImage {
            return NativeImage(ICON_SIZE, ICON_SIZE).context2d {
                val s = ICON_SIZE.toDouble()
                fill(Colors.DIMGREY) {
                    if (angle == 0.degrees) {
                        translate(s * 0.5, s * 0.25)
                    } else {
                        translate(s * 0.25, s * 0.5)
                    }
                    scale(ICON_SIZE.toDouble())
                    rotate(angle)
                    moveTo(Point(-0.5, 0.0))
                    lineTo(Point(+0.5, 0.0))
                    lineTo(Point(0.0, 0.5))
                    closePath()
                }
            }
        }

        val ICON_SIZE = 16
        val ICON_OPEN = createIcon(0.degrees)
        val ICON_CLOSE = createIcon((-90).degrees)
    }

    private lateinit var mycontainer: UiContainer

    init {
        button(name ?: "Unknown") {
            this.icon = ICON_OPEN
            onClick {
                mycontainer.visible = !mycontainer.visible
                mycontainer.root?.relayout()
                this.icon = if (mycontainer.visible) ICON_OPEN else ICON_CLOSE
            }
        }
        mycontainer = container {
            for (child in componentChildren) {
                addChild(child)
            }
        }
    }
}

internal fun <T> Views.completedEditing(prop: ObservableProperty<T>) {
    debugSaveView("Adjusted ${prop.name}", null)
    completedEditing(Unit)
}


internal open class UiEditableValue<T>(app: UiApplication, override val prop: ObservableProperty<T>) : UiContainer(app), ObservablePropertyHolder<T> {
    fun completedEditing() {
        app.views?.completedEditing(prop)
    }

    open fun hideEditor() {
        completedEditing()
    }

    open fun showEditor() {
    }
}

internal class UiListEditableValue<T>(
    app: UiApplication,
    val itemsFactory: () -> List<T>,
    prop: ObservableProperty<T>
) : UiEditableValue<T>(app, prop) {
    init {
        layout = UiFillLayout
        visible = true
    }

    var items = itemsFactory()
    val contentText = UiLabel(app)
        .also { it.text = "" }
        .also { it.visible = true }
    val contentComboBox = UiComboBox<T>(app)
        .also { it.items = items }
        .also { it.visible = false }

    fun setValue(value: T, setProperty: Boolean = true) {
        contentText.text = value.toString()
        if (!contentComboBox.visible) {
            contentComboBox.selectedItem = value
        }
        if (setProperty) prop.value = value
    }

    override fun hideEditor() {
        if (!contentText.visible) {
            val selectedItem = contentComboBox.selectedItem
            //println("UiListEditableValue.hideEditor.selectedItem: $selectedItem")
            contentText.visible = true
            contentComboBox.visible = false
            if (selectedItem != null) {
                setValue(selectedItem)
            }
            super.hideEditor()
        }
    }

    override fun showEditor() {
        contentText.visible = false
        contentComboBox.visible = true
        contentComboBox.focus()
    }

    init {
        setValue(prop.value)

        prop.onChange {
            items = itemsFactory()
            setValue(it, false)
        }

        contentText.onClick {
            showEditor()
        }

        contentComboBox.onChange {
            hideEditor()
        }
        contentComboBox.onFocus { e ->
            if (e.typeBlur) {
                hideEditor()
            } else {
                contentComboBox.open()
            }
            //println(e)
        }
        addChild(contentText)
        addChild(contentComboBox)
    }
}

internal class UiNumberEditableValue(
    app: UiApplication,
    prop: ObservableProperty<Double>,
    var min: Double = -1.0,
    var max: Double = +1.0,
    var clampMin: Boolean = false,
    var clampMax: Boolean = false,
    var decimalPlaces: Int = 2
) : UiEditableValue<Double>(app, prop), ObservablePropertyHolder<Double> {
    var evalContext: () -> Any? = { null }
    val initial = prop.value
    companion object {
        //val MAX_WIDTH = 300
        val MAX_WIDTH = 1000
    }

    init {
        prop.onChange {
            //println("prop.onChange: $it")
            if (current != it) {
                setValue(it, setProperty = false)
            }
        }
    }

    val contentText = UiLabel(app).also { it.text = "" }.also { it.visible = true }
    val contentTextField = UiTextField(app).also { it.text = contentText.text }.also { it.visible = false }
    var current: Double = Double.NaN

    val isEditorVisible get() = !contentText.visible

    override fun hideEditor() {
        if (!isEditorVisible) return
        contentText.visible = true
        contentTextField.visible = false
        if (contentTextField.text.isNotEmpty()) {
            setValue(contentTextField.text.toDoubleOrNull() ?: 0.0)
        }
        super.hideEditor()
    }

    override fun showEditor() {
        if (isEditorVisible) return
        contentTextField.text = contentText.text
        contentText.visible = false
        contentTextField.visible = true
        contentTextField.select()
        contentTextField.focus()
    }

    fun setValue(value: Double, setProperty: Boolean = true) {
        //println("setValue")
        var rvalue = value
        if (clampMin) rvalue = rvalue.coerceAtLeast(min)
        if (clampMax) rvalue = rvalue.coerceAtMost(max)
        val valueStr = rvalue.toStringDecimal(decimalPlaces)
        if (current != rvalue) {
            current = rvalue
            if (setProperty) {
                prop.value = rvalue
            }
            contentText.text = valueStr
            if (!isEditorVisible) {
                contentTextField.text = valueStr
            }
        }
    }

    init {
        layout = UiFillLayout
        //layout = HorizontalUiLayout
        visible = true
        contentText.onClick {
            showEditor()
        }
        contentTextField.onKeyEvent { e ->
            if (e.typeDown && e.key == Key.RETURN) {
                hideEditor()
            }
            //println(e)
        }
        contentTextField.onFocus { e ->
            if (e.typeBlur) {
                hideEditor()
            }
            //println(e)
        }
        var startX = 0
        var startY = 0
        var startValue = current
        contentText.onMouseEvent { e ->
            if (e.typeDown) {
                startX = e.x
                startY = e.y
                startValue = current
                e.requestLock()
            }
            if (e.typeUp) {
                app.views?.completedEditing(prop)
            }
            if (e.typeDrag) {
                val dx = (e.x - startX).toDouble()
                val dy = (e.y - startY).toDouble()
                //println("typeDrag: dx=$dx")
                val lenAbs = dx.absoluteValue.convertRange(0.0, MAX_WIDTH.toDouble(), 0.0, max - min)
                val len = lenAbs.withSign(dx)
                setValue(startValue + len)
                //println("//")
            }
        }
        setValue(initial)
        addChild(contentText)
        addChild(contentTextField)
    }
}

internal class UiRowEditableValue(app: UiApplication, val labelText: String, val editor: UiComponent) : UiContainer(app) {
    val leftPadding = UiLabel(app)
    val label = UiLabel(app).apply {
        text = labelText
        val component = (this@apply.nativeComponent as JLabel)
        component.font = Font(component.font.name, Font.BOLD, component.font.size)
        component.foreground = Color.DARK_GRAY
    }
    init {
        layout = HorizontalUiLayout
        leftPadding.preferredSize(8.pt, 24.pt)
        label.preferredSize(100.pt, 24.pt)
        editor.preferredSize(100.percent - 100.pt - (8.pt * 2) - 8.pt, 24.pt)
        addChild(leftPadding)
        addChild(label)
        addChild(editor)
        label.onClick {
            if (editor is UiEditableValue<*>) {
                editor.hideEditor()
            }
        }
    }
}

internal class UiTextEditableValue(
    app: UiApplication,
    prop: ObservableProperty<String>,
    val kind: Kind
) : UiEditableValue<String>(app, prop), ObservablePropertyHolder<String> {
    open class Kind {
        object STRING : Kind()
        object COLOR : Kind()
        class FILE(val currentVfs: VfsFile, val filter: (VfsFile) -> Boolean) : Kind()
    }


    val initial = prop.value


    init {
        prop.onChange {
            if (current != it) {
                setValue(it, setProperty = false)
            }
        }
    }

    val contentText = UiLabel(app).also { it.text = "" }.also { it.visible = true }
    val contentTextField = UiTextField(app).also { it.text = contentText.text }.also { it.visible = false }
    var current: String = ""

    override fun hideEditor() {
        if (!contentText.visible) {
            contentText.visible = true
            contentTextField.visible = false
            setValue(untransformed(contentTextField.text))
            super.hideEditor()
        }
    }

    override fun showEditor() {
        contentTextField.text = contentText.text
        contentText.visible = false
        contentTextField.visible = true
        contentTextField.select()
        contentTextField.focus()
    }

    fun transformed(text: String): String = text.uescape()
    fun untransformed(text: String): String = text.unescape()

    fun setValue(value: String, setProperty: Boolean = true) {
        if (current != value) {
            current = value
            if (setProperty) prop.value = value
            val transformed = transformed(value)
            contentText.text = transformed
            contentTextField.text = transformed
        }
    }

    init {
        visible = true
        layout = HorizontalUiLayout
        contentText.onClick { showEditor() }
        contentTextField.onKeyEvent { e -> if (e.typeDown && e.key == Key.RETURN) hideEditor() }
        contentTextField.onFocus { e -> if (e.typeBlur) hideEditor() }
        var childCount = 1
        when (kind) {
            is Kind.FILE -> {
                button("...") {
                    preferredWidth = 50.percent
                    childCount++
                    onClick {
                        val file = openFileDialog(null, kind.filter)
                        if (file != null) {
                            val filePathInfo = file.absolutePathInfo
                            val currentVfsPathInfo = kind.currentVfs.absolutePathInfo
                            val relativePath = filePathInfo.relativePathTo(currentVfsPathInfo)
                            println("filePathInfo: $filePathInfo")
                            println("currentVfsPathInfo: $currentVfsPathInfo")
                            println("relativePath: $relativePath")

                            //PathInfo("test").rela
                            if (relativePath != null) {
                                setValue(relativePath)
                                completedEditing()
                            }
                        }
                    }
                }
            }
            is Kind.COLOR -> {
                button("...") {
                    preferredWidth = 50.percent
                    childCount++
                    onClick {
                        val color = Colors[prop.value]
                        prop.value = (openColorPickerDialog(color) { prop.value = it.hexString } ?: color).hexString
                        completedEditing()
                    }
                }
            }
        }
        container {
            layout = UiFillLayout
            preferredWidth = if (childCount == 2) 50.percent else 100.percent
            setValue(initial)
            addChild(contentText)
            addChild(contentTextField)
        }
    }
}

internal class UiTwoItemEditableValue<T>(app: UiApplication, left: UiEditableValue<T>, right: UiEditableValue<T>) : UiEditableValue<T>(app, left.prop) {
    init {
        layout = HorizontalUiLayout
        this.preferredWidth = 100.percent
        left.preferredWidth = 50.percent
        right.preferredWidth = 50.percent
        addChild(left)
        addChild(right)
    }
}

internal class UiFourItemEditableValue<T>(app: UiApplication, a: UiEditableValue<T>, b: UiEditableValue<T>, c: UiEditableValue<T>, d: UiEditableValue<T>) : UiEditableValue<T>(app, a.prop) {
    init {
        layout = HorizontalUiLayout
        this.preferredWidth = 100.percent
        a.preferredWidth = 25.percent
        b.preferredWidth = 25.percent
        c.preferredWidth = 25.percent
        d.preferredWidth = 25.percent
        addChild(a)
        addChild(b)
        addChild(c)
        addChild(d)
    }
}
