package com.soywiz.korge.view.ktree

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.particle.*
import com.soywiz.korge.tiled.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.grid.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*
import kotlin.jvm.*
import kotlin.reflect.*

interface KTreeSerializerHolder {
    val serializer: KTreeSerializer
}

open class KTreeSerializerExtension(val name: String) {
    val nameLC = name.toLowerCase()

    open fun complete(serializer: KTreeSerializer, view: View) {
    }

    open fun getProps(serializer: KTreeSerializer, view: View): Map<String, Any?>? {
        return null
    }

    open fun setProps(serializer: KTreeSerializer, view: View, xml: Xml) {
    }

    open fun extend(serializer: KTreeSerializer, view: View, xml: Xml): Xml {
        val props = getProps(serializer, view) ?: return xml
        return xml.withExtraChild(Xml("${KTreeSerializer.__ex_}$name", props))
    }
}

open class KTreeSerializerExt<T : View>(val name: String, val clazz: KClass<T>, val factory: () -> T, val block: KTreeSerializerExt<T>.() -> Unit) {
    val nameLC = name.toLowerCase()
    data class Prop<T, R>(val prop: KMutableProperty1<T, R>, val default: R) {
        val name get() = prop.name
    }

    data class CustomProp<T, R>(val prop: KMutableProperty1<T, R>, val serialize: (String) -> R, val deserialize: (R) -> String) {
        val name get() = prop.name
    }

    val propsBoolean = arrayListOf<Prop<T, Boolean>>()
    val propsString = arrayListOf<Prop<T, String>>()
    val propsStringNull = arrayListOf<Prop<T, String?>>()
    val propsDouble = arrayListOf<Prop<T, Double>>()
    val propsInt = arrayListOf<Prop<T, Int>>()
    val propsRGBA = arrayListOf<Prop<T, RGBA>>()
    val propsCustom = arrayListOf<CustomProp<T, *>>()

    val allPropsList = listOf(propsBoolean, propsString, propsStringNull, propsDouble, propsInt, propsRGBA)

    init {
        block()
    }

    @JvmName("addCustom")
    fun <R> add(prop: KMutableProperty1<T, R>, serialize: (String) -> R, deserialize: (R) -> String) {
        propsCustom.add(CustomProp(prop, serialize, deserialize))
    }

    @JvmName("addBoolean")
    fun add(prop: KMutableProperty1<T, Boolean>, default: Boolean = false) = propsBoolean.add(Prop(prop, default))

    @JvmName("addString")
    fun add(prop: KMutableProperty1<T, String>, default: String = "") = propsString.add(Prop(prop, default))

    @JvmName("addStringNull")
    fun add(prop: KMutableProperty1<T, String?>) = propsStringNull.add(Prop(prop, null))

    @JvmName("addDouble")
    fun add(prop: KMutableProperty1<T, Double>, default: Double = 0.0) = propsDouble.add(Prop(prop, default))

    @JvmName("addInt")
    fun add(prop: KMutableProperty1<T, Int>, default: Int = 0) = propsInt.add(Prop(prop, default))

    //@JvmName("addRGBA")
    fun add(prop: KMutableProperty1<T, RGBA>, default: RGBA = Colors.WHITE) = propsRGBA.add(Prop(prop, default))

    open suspend fun ktreeToViewTree(xml: Xml, currentVfs: VfsFile): T {
        if (xml.nameLC != nameLC) error("Not a '$nameLC' ($clazz) : ${xml.nameLC}")
        val instance = factory()
        propsBoolean.fastForEach { it.prop.set(instance, xml.str(it.prop.name, "${it.default}") == "true") }
        propsString.fastForEach { it.prop.set(instance, xml.str(it.prop.name, it.default)) }
        propsStringNull.fastForEach { it.prop.set(instance, xml.strNull(it.prop.name)) }
        propsRGBA.fastForEach { it.prop.set(instance, Colors[(xml.strNull(it.prop.name) ?: it.default.hexString)]) }
        propsDouble.fastForEach { it.prop.set(instance, xml.double(it.prop.name, it.default)) }
        propsInt.fastForEach { it.prop.set(instance, xml.int(it.prop.name, it.default)) }
        propsCustom.fastForEach {
            it as CustomProp<T, Any>
            it.prop.set(instance, it.serialize(xml.str(it.prop.name)))
        }
        return instance
    }

    open fun viewTreeToKTree(view: T, currentVfs: VfsFile, level: Int, props: MutableMap<String, Any?>?): Xml {
        if (view::class != clazz) error("Not a '$name' ($clazz) : $view")
        if (props != null) {
            allPropsList.fastForEach { list ->
                list.fastForEach { props[it.name] = it.prop.get(view) }
            }
            propsCustom.fastForEach {
                it as CustomProp<T, Any>
                props[it.name] = it.deserialize(it.prop.get(view))
            }
        }
        return Xml(nameLC, props)
    }
}

@OptIn(KorgeExperimental::class)
open class KTreeSerializer(val views: Views) : KTreeSerializerHolder, Extra by Extra.Mixin() {
    override val serializer get() = this

    val registrationsExt = mutableSetOf<KTreeSerializerExt<*>>()
    private val registrationsByClass = LinkedHashMap<KClass<*>, KTreeSerializerExt<*>>()
    private val registrationsByNameLC = LinkedHashMap<String, KTreeSerializerExt<*>>()
    private val registrations = mutableSetOf<Registration>()
    val extensions = mutableSetOf<KTreeSerializerExtension>()
    val extensionsByName = LinkedHashMap<String, KTreeSerializerExtension>()

    init {
        register(TextButton.Serializer)
        register(Text2.Serializer)
        register(UIProgressBar.Serializer)
        register(UICheckBox.Serializer)
    }

    class Registration(
        val name: String,
        val deserializer: suspend (xml: Xml) -> View?,
        val serializer: (view: View, properties: MutableMap<String, Any?>?) -> Xml?
    ) {
        override fun toString(): String = "KTreeSerializer($name)"
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Registration
            if (name != other.name) return false
            return true
        }

        override fun hashCode(): Int = name.hashCode()
    }

    fun registerExtension(ext: KTreeSerializerExtension) {
        extensions += ext
        extensionsByName[ext.nameLC] = ext
    }

    fun register(registration: KTreeSerializerExt<*>) {
        registrationsExt.add(registration)
        registrationsByClass[registration.clazz] = registration
        registrationsByNameLC[registration.nameLC] = registration
    }

    fun register(registration: Registration) {
        registrations.add(registration)
    }

    fun register(name: String, deserializer: suspend (xml: Xml) -> View?, serializer: (view: View, properties: MutableMap<String, Any?>?) -> Xml?) {
        register(Registration(name, deserializer, serializer))
    }

    open suspend fun ktreeToViewTree(xml: Xml, currentVfs: VfsFile, parent: Container?): View {
        return ktreeToViewTreeInternal(xml, currentVfs, parent).also {
            for (extension in extensions) {
                extension.complete(this, it)
            }
        }
    }

    open suspend fun ktreeToViewTreeInternal(xml: Xml, currentVfs: VfsFile, parent: Container?): View {
        var view: View? = null
        when (xml.nameLC) {
            "solidrect" -> view = SolidRect(100, 100, Colors.RED)
            "ellipse" -> view = Ellipse(50.0, 50.0, Colors.RED)
            "container" -> view = Container()
            "image" -> view = Image(Bitmaps.transparent)
            "treeviewref" -> view = TreeViewRef()
            "particle" -> view = ParticleEmitterView(ParticleEmitter())
            "animation" -> view = AnimationViewRef()
            "tiledmapref" -> view = TiledMapViewRef()
            "ninepatch" -> view = NinePatchEx(NinePatchBitmap32(Bitmap32(62, 62)))
            "ktree" -> view = KTreeRoot(100.0, 100.0)
            else -> {
                val registration = registrationsByNameLC[xml.nameLC]
                if (registration != null) {
                    view = registration.ktreeToViewTree(xml, currentVfs)
                } else {
                    for (registration in registrations) {
                        view = registration.deserializer(xml)
                        if (view != null) break
                    }
                }
            }
        }

        if (view == null) {
            TODO("Unsupported node '${xml.name}'")
        }

        if (view is ViewFileRef) {
            val sourceFile = xml.str("sourceFile")
            if (sourceFile.isNotBlank()) {
                try {
                    view.forceLoadSourceFile(views, currentVfs, sourceFile)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        fun double(prop: KMutableProperty0<Double>, defaultValue: Double) {
            prop.set(xml.double(prop.name, defaultValue))
        }

        fun angleDegrees(prop: KMutableProperty0<Angle>, defaultValue: Angle) {
            prop.set(xml.double(prop.name, defaultValue.degrees).degrees)
        }

        fun color(prop: KMutableProperty0<RGBA>, defaultValue: RGBA) {
            prop.set(Colors[(xml.strNull(prop.name) ?: defaultValue.hexString)])
        }

        fun string(prop: KMutableProperty0<String>, defaultValue: String) {
            prop.set(xml.str(prop.name, defaultValue))
        }
        fun stringNull(prop: KMutableProperty0<String?>) {
            prop.set(xml.strNull(prop.name))
        }

        stringNull(view::name)
        color(view::colorMul, Colors.WHITE)
        double(view::alpha, 1.0)
        double(view::speed, 1.0)
        double(view::ratio, 0.0)
        double(view::x, 0.0)
        double(view::y, 0.0)
        angleDegrees(view::rotation, 0.degrees)
        double(view::scaleX, 1.0)
        double(view::scaleY, 1.0)
        double(view::skewXDegrees, 0.0)
        double(view::skewYDegrees, 0.0)
        if (view is RectBase) {
            double(view::anchorX, 0.0)
            double(view::anchorY, 0.0)
            double(view::width, 100.0)
            double(view::height, 100.0)
        }
        if (view is NinePatchEx) {
            double(view::width, 100.0)
            double(view::height, 100.0)
        }
        if (view is Text2) {
            string(view::text, "Text")
            double(view::fontSize, 10.0)
            //view.fontSource = xml.str("fontSource", "")
            view.verticalAlign = VerticalAlign(xml.str("verticalAlign"))
            view.horizontalAlign = HorizontalAlign(xml.str("horizontalAlign"))
        }
        if (view is KTreeRoot) {
            view.width = xml.double("width", 512.0)
            view.height = xml.double("height", 512.0)
            view.grid.width = xml.int("gridWidth", 20)
            view.grid.height = xml.int("gridHeight", 20)
        }
        view.blendMode = BlendMode[xml.str("blendMode", "INHERIT")]

        parent?.addChild(view)

        if (view is Container) {
            for (node in xml.allNodeChildren) {
                if (node.nameLC.startsWith(__ex_)) continue
                ktreeToViewTreeInternal(node, currentVfs, view)
            }
        }
        for (node in xml.allNodeChildren) {
            if (!node.nameLC.startsWith(__ex_)) continue
            val extensionName = node.nameLC.removePrefix(__ex_)
            val extension = extensionsByName[extensionName]
            if (extension == null) {
                println("Can't find extension $__ex_ : extensionName=$extensionName")
            }
            extension?.setProps(this, view, node)
        }
        return view
    }

    open fun viewTreeToKTree(view: View, currentVfs: VfsFile, level: Int): Xml {
        val properties = LinkedHashMap<String, Any?>()

        fun add(prop: KProperty0<*>) {
            properties[prop.name] = prop.get()
        }
        fun add(prop: KProperty0<Angle>) {
            properties[prop.name] = prop.get().degrees
        }

        if (view.name !== null) add(view::name)
        if (view.colorMul != Colors.WHITE) {
            properties["colorMul"] = view.colorMul.hexString
        }
        if (view.blendMode != BlendMode.INHERIT) add(view::blendMode)
        if (view.alpha != 1.0) add(view::alpha)
        if (view.speed != 1.0) add(view::speed)
        if (view.ratio != 0.0) add(view::ratio)
        if (view.x != 0.0) add(view::x)
        if (view.y != 0.0) add(view::y)
        if (view.rotation != 0.radians) add(view::rotation)
        if (view.scaleX != 1.0) add(view::scaleX)
        if (view.scaleY != 1.0) add(view::scaleY)
        if (view.skewXDegrees != 0.0) add(view::skewXDegrees)
        if (view.skewYDegrees != 0.0) add(view::skewYDegrees)
        if (view is RectBase) {
            if (view.anchorX != 0.0) add(view::anchorX)
            if (view.anchorY != 0.0) add(view::anchorY)
            add(view::width)
            add(view::height)
        }
        if (view is NinePatchEx) {
            add(view::width)
            add(view::height)
        }
        if (view is ViewFileRef) {
            add(view::sourceFile)
        }

        val rproperties: LinkedHashMap<String, Any?>? = if (level == 0) null else properties

        val registration = registrationsByClass[view::class]
        var out = if (registration != null) {
            (registration as KTreeSerializerExt<View>).viewTreeToKTree(view, currentVfs, level, rproperties)
        } else {
            val results = registrations.map { it.serializer(view, rproperties) }
            val result = results.filterNotNull().firstOrNull()

            //println("registrations: $registrations, $result, $results")
            result ?: when (view) {
                is NinePatchEx -> Xml("ninepatch", rproperties)
                is KTreeRoot -> Xml("ktree", mapOf("width" to view.width, "height" to view.height, "gridWidth" to view.grid.width, "gridHeight" to view.grid.height)) {
                    view.forEachChildren { this@Xml.node(viewTreeToKTree(it, currentVfs, level + 1)) }
                }
                is AnimationViewRef -> Xml("animation", rproperties)
                is ParticleEmitterView -> Xml("particle", rproperties)
                is SolidRect -> Xml("solidrect", rproperties)
                is Ellipse -> Xml("ellipse", rproperties)
                is Image -> Xml("image", rproperties)
                is TreeViewRef -> Xml("treeviewref", rproperties)
                is TiledMapViewRef -> Xml("tiledmapref", rproperties)
                is Text2 -> Xml("text", rproperties)
                is TextButton -> Xml("uitextbutton", rproperties)
                is Container -> Xml("container", rproperties) {
                    view.forEachChildren { this@Xml.node(viewTreeToKTree(it, currentVfs, level + 1)) }
                }
                else -> error("Don't know how to serialize $view")
            }
        }
        //println("extensions: $extensions")
        for (ext in extensions) {
            out = ext.extend(this, view, out)
        }
        return out
    }

    companion object {
        internal val __ex_ = "__ex_"
    }
}

suspend fun Xml.ktreeToViewTree(views: Views, currentVfs: VfsFile = views.currentVfs, parent: Container? = null): View = views.serializer.ktreeToViewTree(this, currentVfs, parent)
suspend fun Xml.ktreeToViewTree(serializer: KTreeSerializerHolder, currentVfs: VfsFile, parent: Container? = null): View = serializer.serializer.ktreeToViewTree(this, currentVfs, parent)
suspend fun VfsFile.readKTree(serializer: KTreeSerializerHolder, parent: Container? = null): View = readXml().ktreeToViewTree(serializer, this.parent, parent)

fun View.viewTreeToKTree(views: Views, level: Int = 1): Xml = views.serializer.viewTreeToKTree(this, views.currentVfs, level)
fun View.viewTreeToKTree(serializer: KTreeSerializerHolder, currentVfs: VfsFile, level: Int = 1): Xml = serializer.serializer.viewTreeToKTree(this, currentVfs, level)

class KTreeRoot(width: Double, height: Double) : FixedSizeContainer(width, height) {
    val grid = OrthographicGrid(20, 20)
}
