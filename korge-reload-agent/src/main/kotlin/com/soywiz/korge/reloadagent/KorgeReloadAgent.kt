package com.soywiz.korge.reloadagent

import com.soywiz.korge.reloadagent.KorgeReloadAgent.getRecursivePaths
import com.sun.nio.file.SensitivityWatchEventModifier
import java.io.File
import java.lang.instrument.ClassDefinition
import java.lang.instrument.Instrumentation
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchService
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes

// https://www.baeldung.com/java-instrumentation
object KorgeReloadAgent {
    //@JvmStatic
    //fun main(args: Array<String>) {
    //    val classLoader = this::class.java.classLoader
    //    println("KorgeReloadAgent: main")
    //    println("CLASSLOADER: $classLoader")
    //    val jvm: VirtualMachine = VirtualMachine.attach(jvmPid)
    //    jvm.loadAgent(agentFile.getAbsolutePath())
    //    jvm.detach()
    //}

    class ClassInfo(val path: Path, val className: String)

    @JvmStatic
    fun premain(agentArgs: String?, inst: Instrumentation) {
        reloadCommon("premain", agentArgs, inst)
    }

    @JvmStatic
    fun agentmain(agentArgs: String?, inst: Instrumentation) {
        reloadCommon("agentmain", agentArgs, inst)
    }

    fun reloadCommon(type: String, agentArgs: String?, inst: Instrumentation) {
        val agentArgs = agentArgs ?: ""
        println("[KorgeReloadAgent] In $type method: agentArgs=$agentArgs")
        val rootFolders = agentArgs.split(",")
        Thread {
            val watcher: WatchService = FileSystems.getDefault().newWatchService()

            val cannonicalRootFolders = rootFolders.map {
                File(it).canonicalPath.replace("\\", "/").trimEnd('/') + "/"
            }

            fun getPathRelativeToRoot(path: String): String? {
                for (root in cannonicalRootFolders) {
                    if (path.startsWith(root)) {
                        return path.removePrefix(root)
                    }
                }
                return null
            }

            for (rootFolder in cannonicalRootFolders) {
                val folderToWatch = File(rootFolder).toPath()
                println("[KorgeReloadAgent]: !!ROOT $folderToWatch")

                for (path in folderToWatch.getRecursivePaths()) {
                    println("[KorgeReloadAgent]: Watching $path")
                    path.register(
                        watcher,
                        arrayOf(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY),
                        SensitivityWatchEventModifier.HIGH
                    )
                }
            }

            while (true) {
                //println("[KorgeReloadAgent]: Waiting for event...")
                val key = watcher.take()
                //println("KorgeReloadAgent: received event $key")
                val modifiedClassNames = arrayListOf<ClassInfo>()
                try {
                    for (event in key.pollEvents()) {
                        // @TODO: If we create a new folder, we should register a new key
                        val kind: WatchEvent.Kind<*> = event.kind()
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue
                        }
                        val ev: WatchEvent<Path> = event as WatchEvent<Path>
                        val dir = key.watchable() as Path
                        val fileName: Path = ev.context()
                        val fullPath: Path = dir.resolve(fileName)
                        if (fileName.name.endsWith(".class")) {
                            val fullPathStr = fullPath.absolutePathString().replace("\\", "/")
                            val pathRelativeToRoot = getPathRelativeToRoot(fullPathStr)

                            //println("[KorgeReloadAgent] ev=$ev[${kind.name()}], pathRelativeToRoot=$pathRelativeToRoot, fullPath=$fullPathStr")
                            if (pathRelativeToRoot != null) {
                                modifiedClassNames += ClassInfo(
                                    fullPath,
                                    pathRelativeToRoot.removeSuffix(".class").replace("/", ".")
                                )
                            }
                        }
                    }
                } finally {
                    key.reset()
                }

                if (modifiedClassNames.isNotEmpty()) {
                    println("[KorgeReloadAgent] modifiedClassNames=$modifiedClassNames")
                    val classesByName = inst.allLoadedClasses.associateBy { it.name }
                    try {
                        val definitions = modifiedClassNames.mapNotNull { info ->
                            val clazz = classesByName[info.className] ?: return@mapNotNull null
                            if (!info.path.exists()) return@mapNotNull null
                            ClassDefinition(clazz, info.path.readBytes())
                        }
                        //inst.redefineClasses(*definitions.toTypedArray())
                        for (def in definitions) {
                            try {
                                inst.redefineClasses(def)
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                        val triggerReload = Class.forName("com.soywiz.korge.KorgeReload").getMethod("triggerReload", java.util.List::class.java)
                        triggerReload.invoke(null, definitions.map { it.definitionClass.name })
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }

    fun Path.getRecursivePaths(out: ArrayList<Path> = arrayListOf()): List<Path> {
        out.add(this)
        for (child in this.listDirectoryEntries()) {
            if (child.isDirectory()) {
                child.getRecursivePaths(out)
            }
        }
        return out
    }

    /*

    public static void premain(
    String agentArgs, Instrumentation inst) {

        LOGGER.info("[Agent] In premain method");
        String className = "com.baeldung.instrumentation.application.MyAtm";
        transformClass(className,inst);
    }

     */
}
