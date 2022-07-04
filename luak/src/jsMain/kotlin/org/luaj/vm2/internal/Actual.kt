package org.luaj.vm2.internal

import org.luaj.vm2.io.*
import kotlin.reflect.*

internal actual object JSystem {
    actual val out: LuaWriter by lazy {
        object : LuaWriter() {
            override fun print(v: String) = kotlin.io.println(v)
            override fun write(value: Int) = kotlin.io.print(value.toChar())
        }
    }
    actual val err: LuaWriter by lazy { out }
    actual val `in`: LuaBinInput by lazy {
        object : LuaBinInput() {
            // No input
            override fun read(): Int = -1
        }
    }

    actual fun exit(code: Int) {
        TODO("exit($code)")
    }

    actual fun getProperty(key: String, def: String?): String? = when (key) {
        "CALLS" -> "0"
        "TRACE" -> "0"
        "luaj.package.path" -> "./"
        "file.separator" -> "/"
        "line.separator" -> "\n"
        else -> def
    }

    actual fun gc() = Unit

    actual fun totalMemory(): Long = 0L
    actual fun freeMemory(): Long = 0L

    actual fun InstantiateClassByName(name: String): Any? = TODO()
    actual fun StartNativeThread(runnable: () -> Unit, name: String): Unit = TODO()
    actual fun Object_notify(obj: Any) {
        TODO()
    }

    actual fun Object_wait(obj: Any) {
        TODO()
    }

    actual fun Object_wait(obj: Any, time: Long) {
        TODO()
    }

    actual fun Class_portableName(clazz: KClass<*>): String = clazz.simpleName ?: "Unknown"
    actual fun Class_isInstancePortable(clazz: KClass<*>, ins: Any): Boolean = clazz.isInstance(ins)
    actual fun Class_getResourceAsStreamPortable(clazz: KClass<*>, res: String): LuaBinInput? = TODO("getResourceAsStream")

    actual val supportStatic: Boolean = false
}

actual open class IOException : Exception {
    actual constructor() : super()
    actual constructor(message: String) : super(message)
}

actual open class EOFException : IOException {
    actual constructor() : super()
    actual constructor(message: String) : super(message)
}


actual class InterruptedException : Exception()

actual class WeakReference<T> actual constructor(val value: T) {
    actual fun get(): T? {
        println("Warning: WeakReference not fully implemented in this target")
        return value
    }
}

