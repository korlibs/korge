package korlibs.korge.view.property

import korlibs.datastructure.iterators.*
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

abstract class BasePWithProperty(val callable: KCallable<*>, val viewProp: ViewProperty, val clazz: KClass<*>) {
    val kname get() = callable.name
    val ktype = callable.returnType
    val order: Int get() = viewProp.order
    val name: String get() = viewProp.name.takeIf { it.isNotBlank() } ?: kname
    //abstract fun invoke(instance: Any): Any?
    abstract fun get(instance: Any): Any?
    abstract fun set(instance: Any, value: String?)
}

class PropWithProperty(val prop: KProperty<*>, viewProp: ViewProperty, clazz: KClass<*>) : BasePWithProperty(prop, viewProp, clazz) {
    //override fun invoke(instance: Any): Any? = prop.getter.call(instance)
    override fun get(instance: Any): Any? = prop.getter.call(instance)

    override fun set(instance: Any, value: String?) {
        val clazz = ktype.classifier as KClass<*>
        val fvalue = when (clazz) {
            Int::class -> value?.toIntOrNull() ?: 0
            String::class -> value
            else -> error("Unsupported setting value of type $clazz")
        }
        (prop as KMutableProperty<*>).setter.call(instance, fvalue)
    }
}
class ActionWithProperty(val func: KFunction<*>, viewProp: ViewProperty, clazz: KClass<*>) : BasePWithProperty(func, viewProp, clazz) {
    //override fun invoke(instance: Any) { func.call(instance) }
    override fun get(instance: Any): Any? = Unit
    override fun set(instance: Any, value: String?) {
        func.call(instance)
    }
}

private fun <T> Iterable<T>.multisorted(vararg props: KProperty1<T, Comparable<*>>): List<T> {
    @Suppress("UNCHECKED_CAST")
    val props2 = props as Array<KProperty1<T, Comparable<Any>>>
    return sortedWith { a, b ->
        props2.fastForEach {
            val result = it.get(a).compareTo(it.get(b))
            if (result != 0) return@sortedWith result
        }
        return@sortedWith 0
    }
}


class PWithPropertyList<T : BasePWithProperty>(val items: List<T> = emptyList()) {
    val groups by lazy {
        items.groupBy { it.viewProp.groupName }.mapValues { it.value.multisorted(BasePWithProperty::order, BasePWithProperty::name) as List<T> }
    }
    val flatProperties by lazy { groups.flatMap { it.value } }
}

class ViewClassInfoGroup(
    val clazz: KClass<*>,
    val props: PWithPropertyList<PropWithProperty>?,
    val actions: PWithPropertyList<ActionWithProperty>?,
) {
    val actionsAndProps = (props?.flatProperties ?: emptyList()) + (actions?.flatProperties ?: emptyList())
}

class ViewPropsInfo private constructor(val clazz: KClass<*>) {
    companion object {
        val CACHE = LinkedHashMap<KClass<*>, ViewPropsInfo>()

        operator fun get(clazz: KClass<*>): ViewPropsInfo {
            return CACHE.getOrPut(clazz) { ViewPropsInfo(clazz) }
        }

        operator fun get(instance: Any?): ViewPropsInfo {
            return get(if (instance != null) instance::class else Unit::class)
        }
    }

    val allProps = arrayListOf<PropWithProperty>()
    val allActions = arrayListOf<ActionWithProperty>()

    init {
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
        findAllProps(clazz)
    }

    val allPropsAndActions by lazy { allProps + allActions }
    val allPropsAndActionsByKName by lazy { allPropsAndActions.associateBy { it.kname } }

    val allPropsByClazz by lazy { allProps.groupBy { it.clazz }.mapValues { PWithPropertyList(it.value) } }
    val allActionsByClazz by lazy { allActions.groupBy { it.clazz }.mapValues { PWithPropertyList(it.value) } }

    val classes by lazy { allPropsByClazz.keys + allActionsByClazz.keys }

    val groups by lazy { classes.map { ViewClassInfoGroup(it, allPropsByClazz[it], allActionsByClazz[it]) } }
}
