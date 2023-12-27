package korlibs.datastructure

import kotlin.reflect.*

interface Ref<T : Any> {
    var value: T
}

fun <T : Any> Ref(): Ref<T> = object : Ref<T> {
    override lateinit var value: T
}
fun <T : Any> Ref(value: T): Ref<T> = Ref<T>().also { it.value = value }

fun <T : Any> KMutableProperty0<T>.toRef(): Ref<T> = Ref(this)

fun <T : Any> Ref(prop: KMutableProperty0<T>): Ref<T> = object : Ref<T> {
    override var value: T
        get() = prop.get()
        set(value) { prop.set(value) }
}
