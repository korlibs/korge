package com.soywiz.korge.view.ktree

import com.soywiz.korge.particle.*
import com.soywiz.korge.tiled.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.BlendMode
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*
import kotlin.reflect.*

interface KTreeSerializerHolder {
    val serializer: KTreeSerializer
}

open class KTreeSerializer(val views: Views) : KTreeSerializerHolder {
    override val serializer get() = this

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

    private val registrations = mutableSetOf<Registration>()

    fun register(registration: Registration) {
        registrations.add(registration)
    }

    fun register(name: String, deserializer: suspend (xml: Xml) -> View?, serializer: (view: View, properties: MutableMap<String, Any?>?) -> Xml?) {
        register(Registration(name, deserializer, serializer))
    }

    open suspend fun ktreeToViewTree(xml: Xml, currentVfs: VfsFile): View {
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
            "text" -> view = Text2(xml.str("text"))
            else -> {
                for (registration in registrations) {
                    view = registration.deserializer(xml)
                    if (view != null) break
                }
            }
        }

        if (view == null) {
            TODO("Unsupported node ${xml.name}")
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

        if (view is Text2) {
            val fontSource = xml.str("fontSource")
            if (fontSource.isNotBlank()) {
                try {
                    view.forceLoadFontSource(currentVfs, fontSource)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        fun double(prop: KMutableProperty0<Double>, defaultValue: Double) {
            prop.set(xml.double(prop.name, defaultValue))
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
        double(view::rotationDegrees, 0.0)
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
        view.blendMode = BlendMode[xml.str("blendMode", "INHERIT")]
        if (view is Container) {
            for (node in xml.allNodeChildren) {
                view.addChild(ktreeToViewTree(node, currentVfs))
            }
        }
        return view
    }
    
    open fun viewTreeToKTree(view: View, currentVfs: VfsFile, level: Int): Xml {
        val properties = LinkedHashMap<String, Any?>()

        fun add(prop: KProperty0<*>) {
            properties[prop.name] = prop.get()
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
        if (view.rotationDegrees != 0.0) add(view::rotationDegrees)
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
        if (view is Text2) {
            add(view::text)
            add(view::fontSize)
            add(view::fontSource)
            add(view::verticalAlign)
            add(view::horizontalAlign)
        }

        val rproperties: LinkedHashMap<String, Any?>? = if (level == 0) null else properties

        val results = registrations.map { it.serializer(view, rproperties) }
        val result = results.filterNotNull().firstOrNull()


        //println("registrations: $registrations, $result, $results")
        return result ?: when (view) {
            is NinePatchEx -> Xml("ninepatch", rproperties)
            is AnimationViewRef -> Xml("animation", rproperties)
            is ParticleEmitterView -> Xml("particle", rproperties)
            is SolidRect -> Xml("solidrect", rproperties)
            is Ellipse -> Xml("ellipse", rproperties)
            is Image -> Xml("image", rproperties)
            is TreeViewRef -> Xml("treeviewref", rproperties)
            is TiledMapViewRef -> Xml("tiledmapref", rproperties)
            is Text2 -> Xml("text", rproperties)
            is Container -> Xml("container", rproperties) {
                view.forEachChildren { this@Xml.node(viewTreeToKTree(it, currentVfs, level + 1)) }
            }
            else -> error("Don't know how to serialize $view")
        }
    }
}

suspend fun Xml.ktreeToViewTree(views: Views, currentVfs: VfsFile = views.currentVfs): View = views.serializer.ktreeToViewTree(this, currentVfs)
fun View.viewTreeToKTree(views: Views): Xml = views.serializer.viewTreeToKTree(this, views.currentVfs, 0)

suspend fun Xml.ktreeToViewTree(serializer: KTreeSerializerHolder, currentVfs: VfsFile): View = serializer.serializer.ktreeToViewTree(this, currentVfs)
fun View.viewTreeToKTree(serializer: KTreeSerializerHolder, currentVfs: VfsFile): Xml = serializer.serializer.viewTreeToKTree(this, currentVfs, 0)
suspend fun VfsFile.readKTree(serializer: KTreeSerializerHolder): View = readXml().ktreeToViewTree(serializer, this.parent)
