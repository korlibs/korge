package com.soywiz.korge.reloadagent

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.lang.instrument.ClassDefinition
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.net.InetSocketAddress
import java.security.*
import java.util.concurrent.Executors
import kotlin.system.*

// https://www.baeldung.com/java-instrumentation
object KorgeReloadAgent {
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
        val enableRedefinition = args[2].toBoolean()
        val rootFolders = args.drop(3)

        println("[KorgeReloadAgent] In $type method")
        println("[KorgeReloadAgent] - httpPort=$httpPort")
        println("[KorgeReloadAgent] - continuousCommand=$continuousCommand")
        println("[KorgeReloadAgent] - enableRedefinition=$enableRedefinition")
        println("[KorgeReloadAgent] - rootFolders=$rootFolders")

        val processor = KorgeReloaderProcessor(rootFolders, inst, enableRedefinition)
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
                println("[KorgeReloadAgent] - shutdown http server")
                httpServer.stop(0)
                println("[KorgeReloadAgent] - done shutting down http server")
            })
            httpServer.start()
        }.also { it.isDaemon = true }.also { it.name = "KorgeReloadAgent.httpServer" }.start()
        Thread {
            println("[KorgeReloadAgent] - Running $continuousCommand")
            try {
                val isWindows = System.getProperty("os.name").toLowerCase().contains("win")
                //val args = arrayOf<String>()
                //val args = if (isWindows) arrayOf("cmd.exe", "/k") else arrayOf("/bin/sh", "-c")
                //val args = if (isWindows) arrayOf() else arrayOf("/bin/sh", "-c")
                val javaHomeBinFolder = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator
                val jvmLocation = when {
                    System.getProperty("os.name").startsWith("Win") -> "${javaHomeBinFolder}java.exe"
                    else -> "${javaHomeBinFolder}java"
                }
                val p = Runtime.getRuntime().exec("$jvmLocation $continuousCommand")
                //val p = ProcessBuilder(*args, continuousCommand).inheritIO().start()
                //val pID = p.pid()
                //println("[KorgeReloadAgent] - Started continuousCommand PID=$pID")

                Runtime.getRuntime().addShutdownHook(Thread {
                    //if (isWindows) {
                    //    println("[KorgeReloadAgent] - [isAlive=${p.isAlive}] Killing task")
                    //    Runtime.getRuntime().exec(arrayOf("taskkill", "/PID", "$pID")).waitFor()
                    //}

                    //p.outputStream.write()
                    println("[KorgeReloadAgent] - [isAlive=${p.isAlive}] Stopping continuousCommand")
                    p.destroy()
                    Thread.sleep(500L)
                    println("[KorgeReloadAgent] - [isAlive=${p.isAlive}] Stopping forcibly")
                    p.destroyForcibly()
                    println("[KorgeReloadAgent] - [isAlive=${p.isAlive}] Done stopping forcibly")
                })
                val exit = p.waitFor()
                println("[KorgeReloadAgent] - Exited continuous command with $exit code")
            } catch (e: Throwable) {
                println("[KorgeReloadAgent] - Continuous command failed with exception '${e.message}'")
                e.printStackTrace()
            }
        }.also { it.isDaemon = true }.also { it.name = "KorgeReloadAgent.continuousCommand" }.start()

        Runtime.getRuntime().addShutdownHook(Thread {
            val threadSet = Thread.getAllStackTraces().keys
            println("[KorgeReloadAgent] - shutdown: threads=${threadSet.size}")
            println("[KorgeReloadAgent] ${threadSet.map { it.name to it.state }}")
        })
    }
}

object ClassUtils {
    inline class ByteArrayInt(val data: ByteArray) {
        operator fun get(index: Int): Int = data[index].toInt() and 0xFF
        operator fun set(index: Int, value: Int) { data[index] = value.toByte() }
    }

    fun getCanonicalClassNameFromBytes(data: ByteArray): String? {
        val u = ByteArrayInt(data)
        if (u[0] != 0xCA) return null
        if (u[1] != 0xFE) return null
        if (u[2] != 0xBA) return null
        if (u[3] != 0xBE) return null
        if (u[10] != 1) return null
        val classNameSize = (u[11] shl 8) or (u[12] and 0xFF)
        return u.data.sliceArray(13 until 13 + classNameSize).decodeToString()
    }
}

class KorgeReloaderProcessor(val rootFolders: List<String>, val inst: Instrumentation, val enableRedefinition: Boolean) {
    val cannonicalRootFolders = rootFolders.map {
        File(it).canonicalPath.replace("\\", "/").trimEnd('/') + "/"
    }

    val classNameToBytes = LinkedHashMap<String, ByteArray>()

    init {
        if (enableRedefinition) {
            for (file in getAllClassFiles()) {
                val classBytes = file.readBytes()
                val className = ClassUtils.getCanonicalClassNameFromBytes(classBytes)?.let { getCanonicalClassName(it) } ?: continue
                if (className.endsWith(file.nameWithoutExtension)) {
                    classNameToBytes[className] = classBytes
                    //println("KorgeReloaderProcessor.className=$className")
                }
            }
            inst.addTransformer(object : ClassFileTransformer {
                override fun transform(
                    loader: ClassLoader,
                    className: String,
                    classBeingRedefined: Class<*>,
                    protectionDomain: ProtectionDomain,
                    classfileBuffer: ByteArray
                ): ByteArray? {
                    classNameToBytes[getCanonicalClassName(className)] = classfileBuffer
                    //println("ClassFileTransformer: className=$className, classfileBuffer=${classfileBuffer.size}")
                    return null
                }
            })
        }
    }

    fun getCanonicalClassName(className: String): String = className.replace('/', '.')

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
        val allClassFiles = getAllClassFiles()
        println("[KorgeReloadAgent] allClassFiles=${allClassFiles.size}")
        return allClassFiles.filter { it.lastModified() in startTime..endTime }
    }

    fun reloadClassFilesChangedIn(startTime: Long, endTime: Long) {
        val modifiedClassNames = arrayListOf<KorgeReloadAgent.ClassInfo>()
        val allModifiedClassFiles = getAllModifiedClassFiles(startTime, endTime)
        println("[KorgeReloadAgent] allModifiedClassFiles=${allModifiedClassFiles.size}")
        for (file in allModifiedClassFiles) {
            val fullPathStr = file.absolutePath.replace("\\", "/")
            val pathRelativeToRoot = getPathRelativeToRoot(fullPathStr)
            if (pathRelativeToRoot != null) {
                modifiedClassNames += KorgeReloadAgent.ClassInfo(
                    File(fullPathStr),
                    pathRelativeToRoot.removeSuffix(".class").replace("/", ".")
                )
            } else {
                println("[KorgeReloadAgent] ERROR: couldn't find relative to root: '$fullPathStr' in $cannonicalRootFolders")
            }
        }

        if (modifiedClassNames.isEmpty()) {
            println("[KorgeReloadAgent] modifiedClassNames=$modifiedClassNames [EMPTY] STOPPING")
        } else {
            println("[KorgeReloadAgent] modifiedClassNames=$modifiedClassNames")
            var successRedefinition = true
            val changedDefinitions = arrayListOf<ClassDefinition>()
            val times = arrayListOf<Long>()
            if (enableRedefinition) {
                val classesByName = inst.allLoadedClasses.associateBy { it.name }
                try {
                    val definitions: List<ClassDefinition> = modifiedClassNames.mapNotNull { info ->
                        val clazz = classesByName[info.className] ?: return@mapNotNull null
                        if (!info.path.exists()) return@mapNotNull null
                        ClassDefinition(clazz, info.path.readBytes())
                    }
                    //inst.redefineClasses(*definitions.toTypedArray())
                    for (def in definitions) {
                        times += measureTimeMillis {
                            try {
                                val canonicalClassName = getCanonicalClassName(def.definitionClass.name)
                                if (classNameToBytes[canonicalClassName]?.contentEquals(def.definitionClassFile) != true) {
                                    //println("def.definitionClass.name: canonicalClassName=${canonicalClassName}, classfileBuffer=${def.definitionClassFile.size}, classNameToBytes[canonicalClassName]=${classNameToBytes[canonicalClassName]?.size}")
                                    inst.redefineClasses(def)
                                    changedDefinitions += def
                                }
                            } catch (e: java.lang.UnsupportedOperationException) {
                                successRedefinition = false
                            } catch (e: Throwable) {
                                e.printStackTrace()
                                successRedefinition = false
                            }
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            } else {
                successRedefinition = false
            }
            println("[KorgeReloadAgent] reload enableRedefinition=$enableRedefinition, successRedefinition=$successRedefinition, changedDefinitions=${changedDefinitions.size}, classNameToBytes=${classNameToBytes.size}, times[${times.size}]=${times.sum()}ms")
            val triggerReload = Class.forName("com.soywiz.korge.KorgeReload").getMethod("triggerReload", java.util.List::class.java, java.lang.Boolean.TYPE)
            triggerReload.invoke(null, changedDefinitions.map { it.definitionClass.name }, successRedefinition)
        }
    }

}
