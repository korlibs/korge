package korlibs.memory.internal

//@JsFun("(obj) => { return obj }")
//private external fun <T> __unsafeCast(value: Any?): T

//fun <T> Any?.unsafeCast(): T = __unsafeCast(this)
fun <T> Any?.unsafeCast(): T = this as T
