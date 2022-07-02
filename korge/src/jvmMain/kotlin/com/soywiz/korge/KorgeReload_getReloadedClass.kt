package com.soywiz.korge

import com.soywiz.korinject.jvmAutomapping
import java.io.File
import kotlin.reflect.*

internal actual fun <T : Any> KorgeReload_getReloadedClass(clazz: KClass<T>, context: ReloadClassContext): KClass<T> {
    println("### KorgeReload_getReloadedClass: $clazz")
    val oldClass = clazz
    val newClass = KorgeReloadClassLoader().loadClass(oldClass.qualifiedName).kotlin as KClass<T>
    context.injector.removeMappingsByClassName(context.refreshedClasses)
    context.injector.removeMapping(oldClass)
    context.injector.root.jvmAutomapping()
    return newClass
}

class KorgeReloadClassLoader(
    val folders: List<File> = System.getProperty("java.class.path")
        .split(File.pathSeparator)
        .map { File(it) }
        .filter { !it.name.endsWith(".jar") }
    , parent: ClassLoader? = null
) : ClassLoader(parent ?: ClassLoader.getSystemClassLoader()) {
    init {
        println("KorgeReloadClassLoader:\n${folders.joinToString("\n")}")
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized (getClassLoadingLock(name)) {
            findLoadedClass(name)?.let {
                return it
            }
            //return findClass(name)
            //println("CustomClassLoader.loadClass: $name")
            //val packageName = name.substringBeforeLast('.')
            //val baseClassName = name.substringAfterLast('.')
            for (folder in folders) {
                val rname = File(folder, "${name.replace('.', '/')}.class")
                if (rname.exists()) {
                    val bytes = rname.readBytes()
                    println("Reloaded class=$rname")
                    return defineClass(name, bytes, 0, bytes.size)
                }
            }
            return super.loadClass(name, resolve)
        }
    }
}
