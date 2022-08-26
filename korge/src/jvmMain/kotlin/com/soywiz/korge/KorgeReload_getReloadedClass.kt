package com.soywiz.korge

import com.soywiz.korinject.jvmAutomapping
import com.soywiz.korinject.jvmRemoveMappingsByClassName
import java.io.File
import kotlin.reflect.*
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

internal actual val KorgeReloadInternal: KorgeReloadInternalImpl = object : KorgeReloadInternalImpl() {
    override fun <T : Any> getReloadedClass(clazz: KClass<T>, context: ReloadClassContext): KClass<T> {
        println("### KorgeReload_getReloadedClass: $clazz")
        val oldClass = clazz
        val newClass = KorgeReloadClassLoader().loadClass(oldClass.qualifiedName).kotlin as KClass<T>
        context.injector.jvmRemoveMappingsByClassName(context.refreshedClasses)
        context.injector.removeMapping(oldClass)
        context.injector.root.jvmAutomapping()
        return newClass
    }

    override fun transferKeepProperties(old: Any, new: Any) {
        //println("transferKeepProperties: ${old::class.java}")
        for (prop in old::class.memberProperties) {
            if (prop.hasAnnotation<KeepOnReload>()) {
                //println("- $prop : ${prop.annotations}")
                try {
                    val newProp = new::class.memberProperties.firstOrNull { it.name == prop.name }
                    if (newProp != null && newProp.hasAnnotation<KeepOnReload>()) {
                        val oldValue = (prop as KProperty1<Any, Any>).get(old)
                        //println("   ** Trying to set $oldValue to $newProp")
                        (newProp as KMutableProperty1<Any, Any>).set(new, oldValue)
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }
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
