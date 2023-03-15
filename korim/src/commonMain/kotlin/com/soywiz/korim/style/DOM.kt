package com.soywiz.korim.style

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.iterators.fastForEachReverse
import com.soywiz.korim.annotation.KorimExperimental
import com.soywiz.korma.geom.*
import kotlin.jvm.JvmName
import kotlin.reflect.KMutableProperty1

@KorimExperimental
open class DOM(val css: CSS) {
    val elementsById = HashMap<String, DomElement>()
    val elementsByClassName = HashMap<String, MutableSet<DomElement>>()
    fun getSetForClassName(className: String) =  elementsByClassName.getOrPut(className) { mutableSetOf() }

    private val internalListener: DomListener = object : DomListener {
        override fun removedId(element: DomElement, id: String) {
            elementsById.remove(id)
        }

        override fun addedId(element: DomElement, id: String) {
            elementsById[id] = element
        }

        override fun removedClass(element: DomElement, className: String) {
            getSetForClassName(className).remove(element)
        }

        override fun addedClass(element: DomElement, className: String) {
            getSetForClassName(className).add(element)
        }
    }

    val listeners = arrayListOf(internalListener)
    private val listener = ComposedDomListener(listeners)

    class DomPropertyMapping() {
        interface Mapping<T> {
            val name: String
            val property: KMutableProperty1<DomElement, T>
            fun set(element: DomElement, prop: String, value: Any?)
        }
        class RatioMapping(
            override val name: String,
            override val property: KMutableProperty1<DomElement, Double?>
        ) : Mapping<Double?> {
            override fun set(element: DomElement, prop: String, value: Any?) {
                property.set(element, getRatio(prop, value))
            }
        }
        class MatrixMapping(
            override val name: String,
            override val property: KMutableProperty1<DomElement, MMatrix?>
        ) : Mapping<MMatrix?> {
            override fun set(element: DomElement, prop: String, value: Any?) {
                property.set(element, getMatrix(prop, value))
            }
        }

        val mappings = HashMap<String, Mapping<*>>()
        @JvmName("addRatio")
        fun add(name: String, property: KMutableProperty1<out DomElement, out Double?>): DomPropertyMapping = this.apply { mappings[name] = RatioMapping(name,
            property as KMutableProperty1<DomElement, Double?>
        ) }
        @JvmName("addMatrix")
        fun add(name: String, property: KMutableProperty1<out DomElement, out MMatrix?>): DomPropertyMapping = this.apply { mappings[name] = MatrixMapping(name,
            property as KMutableProperty1<DomElement, MMatrix?>
        ) }
    }

    open class DomElement(val dom: DOM, val mappings: DomPropertyMapping? = null) {
        private val listener get() = dom.listener
        var id: String? = null
            set(value) {
                if (field != null) {
                    listener.removedId(this, field!!)
                }
                field = value
                if (field != null) {
                    listener.addedId(this, field!!)
                }
            }
        private val _classNames = mutableSetOf<String>()
        val classNames: Set<String> get() = _classNames
        fun addClassNames(vararg names: String) = names.fastForEach { addClassName(it) }
        fun removeClassNames(vararg names: String) = names.fastForEach { removeClassName(it) }
        fun addClassName(name: String) {
            if (name in _classNames) return
            _classNames.add(name)
            listener.addedClass(this, name)
        }
        fun removeClassName(name: String) {
            if (name !in _classNames) return
            _classNames.remove(name)
            listener.removedClass(this, name)
        }

        // @TODO: Maybe we can create a map like: "transform" to SvgElement::transform to Matrix::class
        open fun setProperty(prop: String, value: Any?) {
            mappings?.mappings?.get(prop)?.let { map ->
                map.set(this, prop, value)
            }
        }

        fun setPropertyInterpolated(result: CSS.InterpolationResult) {
            val properties = result.properties
            for (prop in properties) {
                setProperty(prop, result)
            }
        }
    }

    interface DomListener {
        fun removedId(element: DomElement, id: String) = updatedElement(element)
        fun addedId(element: DomElement, id: String) = updatedElement(element)
        fun removedClass(element: DomElement, className: String) = updatedElement(element)
        fun addedClass(element: DomElement, className: String) = updatedElement(element)
        fun updatedElement(element: DomElement) = Unit
    }

    class ComposedDomListener(val list: List<DomListener>) : DomListener {
        override fun removedId(element: DomElement, id: String) = list.fastForEachReverse { it.removedId(element, id) }
        override fun addedId(element: DomElement, id: String) = list.fastForEachReverse { it.addedId(element, id) }
        override fun removedClass(element: DomElement, className: String) = list.fastForEachReverse { it.removedClass(element, className) }
        override fun addedClass(element: DomElement, className: String) = list.fastForEachReverse { it.addedClass(element, className) }
        override fun updatedElement(element: DomElement) = list.fastForEachReverse { it.updatedElement(element) }
    }

    companion object {
        fun getTransform(prop: String, value: Any?): MatrixTransform = when (value) {
            is MatrixTransform -> value
            is CSS.InterpolationResult -> value.getTransform(prop)
            is CSS.Expression -> value.transform
            else -> MatrixTransform.IDENTITY
        }
        fun getMatrix(prop: String, value: Any?): MMatrix = when (value) {
            is MMatrix -> value
            is CSS.InterpolationResult -> value.getMatrix(prop)
            is CSS.Expression -> value.matrix
            else -> MMatrix()
        }
        fun getRatio(prop: String, value: Any?): Double = when (value) {
            is Double -> value
            is CSS.InterpolationResult -> value.getRatio(prop)
            is CSS.Expression -> value.ratio
            else -> 0.0
        }
    }
}
