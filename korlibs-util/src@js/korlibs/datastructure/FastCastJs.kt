package korlibs.datastructure

actual inline fun <T> Any?.fastCastTo(): T = this.unsafeCast<T>()
