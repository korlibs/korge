package com.soywiz.korge

import com.soywiz.korinject.jvmAutomapping
import com.soywiz.korinject.jvmRemoveMappingsByClassName
import java.io.File
import kotlin.reflect.*
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.soywiz.klogger.*
import kotlin.system.*

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
        val objectMapper = jacksonObjectMapper()
        for (newProp in new::class.memberProperties) {
            if (!newProp.hasAnnotation<KeepOnReload>()) continue

            //println("- $prop : ${prop.annotations}")
            try {
                val newProp = newProp as? KMutableProperty1<Any, Any>? ?: continue
                val oldProp = old::class.memberProperties.firstOrNull { it.name == newProp.name } as? KMutableProperty1<Any, Any>? ?: continue
                val oldClass = oldProp.returnType.jvmErasure
                val newClass = newProp.returnType.jvmErasure
                val oldValue = oldProp.get(old)
                //println("   ** Trying to set $oldValue to $newProp")

                val newValue = when {
                    oldClass === newClass -> oldValue
                    // We try to serialize & unserialize in the case we are for example using an enum that has been reloaded
                    else -> objectMapper.convertValue(oldValue, newClass.java)
                }

                newProp.set(new, newValue)
            } catch (e: Throwable) {
                e.printStackTrace()
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
    companion object {
        val logger = Logger("KorgeReloadClassLoader")
    }

    init {
        logger.info { "KorgeReloadClassLoader:\n${folders.joinToString("\n")}" }
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
                    val clazz: Class<*>
                    val time = measureNanoTime {
                        clazz = defineClass(name, bytes, 0, bytes.size)
                    }
                    logger.debug { "KorgeReload: reloaded class=$rname... size: ${bytes.size} bytes, time: ${time.toDouble() / 1_000_000.0} ms" }
                    return clazz
                }
            }
            return super.loadClass(name, resolve)
        }
    }
}
