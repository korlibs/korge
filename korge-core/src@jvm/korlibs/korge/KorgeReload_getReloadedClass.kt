package korlibs.korge

import korlibs.inject.jvmAutomapping
import korlibs.inject.jvmRemoveMappingsByClassName
import java.io.File
import kotlin.reflect.*
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import korlibs.logger.*
import kotlin.system.*

internal actual val KorgeReloadInternal: KorgeReloadInternalImpl = KorgeReloadInternalJvm

object KorgeReloadInternalJvm : KorgeReloadInternalImpl() {
    val logger = Logger("KorgeReloadInternalJvm")

    override fun <T : Any> getReloadedClass(clazz: KClass<T>, context: ReloadClassContext): KClass<T> {
        logger.debug { "### KorgeReload_getReloadedClass: $clazz" }
        val oldClass = clazz
        val newClass = KorgeReloadClassLoader(extraFolders = context.rootFolders).loadClass(oldClass.qualifiedName).kotlin as KClass<T>
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
                //println("$oldClass -> $newClass")

                val newValue = when {
                    oldClass == newClass -> oldValue
                    // We try to serialize & unserialize in the case we are for example using an enum that has been reloaded
                    else -> {
                        objectMapper.convertValue(oldValue, newClass.java)
                    }
                }

                newProp.set(new, newValue)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}

class KorgeReloadClassLoader(
    val extraFolders: List<String> = emptyList(),
    val allEntries: List<File> = (extraFolders + System.getProperty("java.class.path").split(File.pathSeparator)).map { File(it).absoluteFile }.distinct(),
    val jars: List<File> = allEntries.filter { it.name.endsWith(".jar") },
    val folders: List<File> = allEntries.filter { !it.name.endsWith(".jar") },
    parent: ClassLoader? = null
) : ClassLoader(parent ?: ClassLoader.getSystemClassLoader()) {
    companion object {
        val logger = Logger("KorgeReloadClassLoader")
    }

    init {
        logger.info { "KorgeReloadClassLoader.jars:\n${jars.joinToString("\n")}" }
        logger.info { "KorgeReloadClassLoader.folders:\n${folders.joinToString("\n")}" }
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
