@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package org.luaj.vm2.internal

import org.luaj.vm2.io.*
import kotlin.reflect.*
import kotlin.system.*

internal actual object JSystem {
    actual val out: LuaWriter get() = System.out.toLua().toWriter()
    actual val err: LuaWriter get() = System.err.toLua().toWriter()
    actual val `in`: LuaBinInput get() = System.`in`.toLua()

    actual fun exit(code: Int): Unit = exitProcess(code)
    actual fun getProperty(key: String, def: String?): String? = System.getProperty(key, def)
    actual fun gc() = System.gc()
    actual fun totalMemory(): Long = Runtime.getRuntime().totalMemory()
    actual fun freeMemory(): Long = Runtime.getRuntime().freeMemory()
    actual fun InstantiateClassByName(name: String): Any? = try {
        Class.forName(name).newInstance()
    } catch (e: ClassNotFoundException) {
        null
    }
    actual fun StartNativeThread(runnable: () -> Unit, name: String): Unit = Thread(Runnable(runnable), name).start()
    actual fun Object_notify(obj: Any) = (obj as Object).notify()
    actual fun Object_wait(obj: Any) = (obj as Object).wait()
    actual fun Object_wait(obj: Any, time: Long) = (obj as Object).wait(time)

    actual fun Class_portableName(clazz: KClass<*>): String = clazz.java.name
    actual fun Class_isInstancePortable(clazz: KClass<*>, ins: Any): Boolean = clazz.java.isAssignableFrom(ins::class.java)
    actual fun Class_getResourceAsStreamPortable(clazz: KClass<*>, res: String): LuaBinInput? = clazz.java.getResourceAsStream(res)?.toLua()
    actual val supportStatic: Boolean = true
}

actual typealias IOException = java.io.IOException
actual typealias EOFException = java.io.EOFException

actual typealias InterruptedException = java.lang.InterruptedException

actual typealias WeakReference<T> = java.lang.ref.WeakReference<T>
