package com.soywiz.korge.reloadagent

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.lang.instrument.ClassDefinition
import java.lang.instrument.Instrumentation
import java.net.InetSocketAddress
import java.util.concurrent.Executors

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

    data class ClassInfo(val path: File, val className: String)

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
        val args = agentArgs.split(":::")
        val httpPort = args[0].toIntOrNull() ?: 22011
        val continuousCommand = args[1]
        val rootFolders = args.drop(2)
        println("[KorgeReloadAgent] In $type method")
        println("[KorgeReloadAgent] - httpPort=$httpPort")
        println("[KorgeReloadAgent] - continuousCommand=$continuousCommand")
        println("[KorgeReloadAgent] - rootFolders=$rootFolders")

        val processor = KorgeReloaderProcessor(rootFolders, inst)
        val taskExecutor = Executors.newSingleThreadExecutor()

        Runtime.getRuntime().addShutdownHook(Thread {
            println("[KorgeReloadAgent] - shutdown")
        })
        Thread {
            val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", httpPort), 0)
            httpServer.createContext("/") { t ->
                val response = "ok".toByteArray()
                val parts = t.requestURI.query.trim('?').split("&").associate { val (key, value) = it.split('=', limit = 2); key to value }
                val startTime = parts["startTime"]?.toLongOrNull() ?: 0L
                val endTime = parts["endTime"]?.toLongOrNull() ?: 0L
                println("[KorgeReloadAgent] startTime=$startTime, endTime=$endTime, parts=$parts")
                taskExecutor.submit {
                    processor.reloadClassFilesChangedIn(startTime, endTime)
                }
                t.sendResponseHeaders(200, response.size.toLong())
                t.responseBody.write(response)
                t.responseBody.close()
            }
            Runtime.getRuntime().addShutdownHook(Thread {
                httpServer.stop(0)
            })
            httpServer.start()
        }.also { it.isDaemon = true }.start()
        Thread {
            println("[KorgeReloadAgent] - Running $continuousCommand")
            try {
                val p = ProcessBuilder("/bin/sh", "-c", continuousCommand)
                    //.directory(File("."))
                    .inheritIO()
                    .start()
                Runtime.getRuntime().addShutdownHook(Thread {
                    p.destroy()
                })
                val exit = p.waitFor()
                println("[KorgeReloadAgent] - Exited continuous command with $exit code")
            } catch (e: Throwable) {
                println("[KorgeReloadAgent] - Continuous command failed with exception '${e.message}'")
                e.printStackTrace()
            }
        }.also { it.isDaemon = true }.start()
        /*
        Thread {
            val watcher = JVMWatchCommonWatcher()
            //val watcher = FSWatchCommonWatcher()

            val seq = watcher.watch(*cannonicalRootFolders.toTypedArray())

            for (files in seq) {
                val modifiedClassNames = arrayListOf<ClassInfo>()
                for (fullPathStr in files) {
                    if (fullPathStr.endsWith(".class")) {
                        val pathRelativeToRoot = getPathRelativeToRoot(fullPathStr)
                        if (pathRelativeToRoot != null) {
                            modifiedClassNames += ClassInfo(
                                File(fullPathStr),
                                pathRelativeToRoot.removeSuffix(".class").replace("/", ".")
                            )
                        }
                    }
                }

                if (modifiedClassNames.isNotEmpty()) {
                    println("[KorgeReloadAgent] modifiedClassNames=$modifiedClassNames")
                    val classesByName = inst.allLoadedClasses.associateBy { it.name }
                    try {
                        val definitions: List<ClassDefinition> = modifiedClassNames.mapNotNull { info ->
                            val clazz = classesByName[info.className] ?: return@mapNotNull null
                            if (!info.path.exists()) return@mapNotNull null
                            ClassDefinition(clazz, info.path.readBytes())
                        }
                        //inst.redefineClasses(*definitions.toTypedArray())
                        val workedDefinitions = arrayListOf<ClassDefinition>()
                        for (def in definitions) {
                            try {
                                inst.redefineClasses(def)
                                workedDefinitions += def
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                        val triggerReload = Class.forName("com.soywiz.korge.KorgeReload").getMethod("triggerReload", java.util.List::class.java)
                        triggerReload.invoke(null, workedDefinitions.map { it.definitionClass.name })
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }.also { it.isDaemon = true }.start()
        */
    }
}

class KorgeReloaderProcessor(val rootFolders: List<String>, val inst: Instrumentation) {
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

    fun getAllClassFiles(): List<File> {
        return cannonicalRootFolders.map { File(it) }.flatMap { it.walkBottomUp().toList() }.filter { it.name.endsWith(".class") }
    }

    fun getAllModifiedClassFiles(startTime: Long, endTime: Long): List<File> {
        return getAllClassFiles().filter { it.lastModified() in startTime..endTime }
    }

    fun reloadClassFilesChangedIn(startTime: Long, endTime: Long) {
        val modifiedClassNames = arrayListOf<KorgeReloadAgent.ClassInfo>()
        for (file in getAllModifiedClassFiles(startTime, endTime)) {
            val fullPathStr = file.absolutePath
            val pathRelativeToRoot = getPathRelativeToRoot(fullPathStr)
            if (pathRelativeToRoot != null) {
                modifiedClassNames += KorgeReloadAgent.ClassInfo(
                    File(fullPathStr),
                    pathRelativeToRoot.removeSuffix(".class").replace("/", ".")
                )
            }
        }

        if (modifiedClassNames.isNotEmpty()) {
            println("[KorgeReloadAgent] modifiedClassNames=$modifiedClassNames")
            val classesByName = inst.allLoadedClasses.associateBy { it.name }
            try {
                val definitions: List<ClassDefinition> = modifiedClassNames.mapNotNull { info ->
                    val clazz = classesByName[info.className] ?: return@mapNotNull null
                    if (!info.path.exists()) return@mapNotNull null
                    ClassDefinition(clazz, info.path.readBytes())
                }
                //inst.redefineClasses(*definitions.toTypedArray())
                val workedDefinitions = arrayListOf<ClassDefinition>()
                for (def in definitions) {
                    try {
                        inst.redefineClasses(def)
                        workedDefinitions += def
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
                val triggerReload = Class.forName("com.soywiz.korge.KorgeReload").getMethod("triggerReload", java.util.List::class.java)
                triggerReload.invoke(null, workedDefinitions.map { it.definitionClass.name })
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

}
