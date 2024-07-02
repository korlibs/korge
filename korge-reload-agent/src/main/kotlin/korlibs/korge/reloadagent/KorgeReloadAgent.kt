package korlibs.korge.reloadagent

import com.sun.net.httpserver.*
import java.io.*
import java.lang.instrument.*
import java.net.*
import java.nio.*
import java.nio.channels.*
import java.security.*
import java.util.concurrent.*
import kotlin.concurrent.*
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

    fun ReadableByteChannel.readFully(size: Int): ByteBuffer = readFully(ByteBuffer.allocate(size))

    fun ReadableByteChannel.readFully(buffer: ByteBuffer): ByteBuffer {
        while (buffer.hasRemaining()) if (this.read(buffer) == -1) break
        buffer.flip()
        return buffer
    }

    fun reloadCommon(type: String, agentArgs: String?, inst: Instrumentation) {
        val agentArgs = agentArgs ?: ""
        val ARGS_SEPARATOR = "<:/:>"
        val CMD_SEPARATOR = "<@/@>"
        printlnDebug("[KorgeReloadAgent] agentArgs=$agentArgs")

        val (portOrUnixStr, continuousCommandStr, enableRedefinitionStr, argsStr) = agentArgs.split(ARGS_SEPARATOR)
        //val httpPort = portStr.toIntOrNull() ?: 22011
        val httpPort = portOrUnixStr.toIntOrNull() ?: -1
        val unixSocketPath = if (portOrUnixStr.toIntOrNull() == null) portOrUnixStr else null
        val continuousCommand = if (continuousCommandStr.isEmpty()) emptyList() else continuousCommandStr.split(CMD_SEPARATOR)
        val enableRedefinition = enableRedefinitionStr.toBoolean()
        val rootFolders = argsStr.split(CMD_SEPARATOR)

        printlnDebug("[KorgeReloadAgent] In $type method")
        printlnDebug("[KorgeReloadAgent] - httpPort=$httpPort")
        printlnDebug("[KorgeReloadAgent] - unixSocketPath=$unixSocketPath")
        printlnDebug("[KorgeReloadAgent] - continuousCommand=$continuousCommand")
        printlnDebug("[KorgeReloadAgent] - enableRedefinition=$enableRedefinition")
        printlnDebug("[KorgeReloadAgent] - rootFolders=$rootFolders")

        val processor = KorgeReloaderProcessor(rootFolders, inst, enableRedefinition)
        val taskExecutor = Executors.newSingleThreadExecutor()

        Runtime.getRuntime().addShutdownHook(Thread {
            printlnDebug("[KorgeReloadAgent] - shutdown")
        })
        when {
            unixSocketPath != null -> {
                thread(start = true, isDaemon = true, name = "KorgeReloadAgent.unixSocketServer") {
                    ServerSocketChannel.open(StandardProtocolFamily.UNIX).bind(UnixDomainSocketAddress.of(unixSocketPath)).use { server ->
                        printlnDebug("[KorgeReloadAgent] - Listening to $unixSocketPath")
                        while (true) {
                            try {
                                val socket = server.accept()
                                val buf = socket.readFully(16)
                                val startTime = buf.getLong()
                                val endTime = buf.getLong()
                                taskExecutor.submit {
                                    processor.reloadClassFilesChangedIn(startTime, endTime)
                                }
                                socket.close()
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            httpPort != -1 -> {
                thread(start = true, isDaemon = true, name = "KorgeReloadAgent.httpServer") {
                    val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", httpPort), 0)
                    httpServer.createContext("/") { t ->
                        val response = "ok".toByteArray()
                        val parts = t.requestURI.query.trim('?').split("&").associate { val (key, value) = it.split('=', limit = 2); key to value }
                        val startTime = parts["startTime"]?.toLongOrNull() ?: 0L
                        val endTime = parts["endTime"]?.toLongOrNull() ?: 0L
                        printlnDebug("[KorgeReloadAgent] startTime=$startTime, endTime=$endTime, parts=$parts")
                        taskExecutor.submit {
                            processor.reloadClassFilesChangedIn(startTime, endTime)
                        }
                        t.sendResponseHeaders(200, response.size.toLong())
                        t.responseBody.write(response)
                        t.responseBody.close()
                    }
                    Runtime.getRuntime().addShutdownHook(Thread {
                        printlnDebug("[KorgeReloadAgent] - shutdown http server")
                        httpServer.stop(0)
                        printlnDebug("[KorgeReloadAgent] - done shutting down http server")
                    })
                    httpServer.start()
                }
            }
        }

        if (continuousCommand.isNotEmpty()) {
            thread(isDaemon = true, name = "KorgeReloadAgent.continuousCommand") {
                printlnDebug("[KorgeReloadAgent] - Running ${continuousCommand.joinToString(" ")}")
                while (true) {
                    try {
                        val isWindows = System.getProperty("os.name").lowercase().contains("win")
                        //val args = arrayOf<String>()
                        //val args = if (isWindows) arrayOf("cmd.exe", "/k") else arrayOf("/bin/sh", "-c")
                        //val args = if (isWindows) arrayOf() else arrayOf("/bin/sh", "-c")
                        val javaHomeBinFolder =
                            System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator
                        val jvmLocation = when {
                            System.getProperty("os.name").startsWith("Win") -> "${javaHomeBinFolder}java.exe"
                            else -> "${javaHomeBinFolder}java"
                        }

                        val isGradle = (continuousCommand.any { it == "org.gradle.wrapper.GradleWrapperMain" })

                        val p = ProcessBuilder()
                            .redirectError(ProcessBuilder.Redirect.INHERIT)
                            //.inheritIO()
                            .command(
                                when {
                                    isGradle -> listOf(jvmLocation, *continuousCommand.toTypedArray())
                                    else -> continuousCommand
                                }
                            )
                            .start()

                        //val p = Runtime.getRuntime().exec("$jvmLocation $continuousCommand")
                        //val p = ProcessBuilder(*args, continuousCommand).inheritIO().start()
                        //val pID = p.pid()
                        //printlnDebug("[KorgeReloadAgent] - Started continuousCommand PID=$pID")

                        Runtime.getRuntime().addShutdownHook(Thread {
                            //if (isWindows) {
                            //    printlnDebug("[KorgeReloadAgent] - [isAlive=${p.isAlive}] Killing task")
                            //    Runtime.getRuntime().exec(arrayOf("taskkill", "/PID", "$pID")).waitFor()
                            //}

                            //p.outputStream.write()
                            printlnDebug("[KorgeReloadAgent] - [isAlive=${p.isAlive}] Stopping continuousCommand")
                            p.destroy()
                            Thread.sleep(500L)
                            printlnDebug("[KorgeReloadAgent] - [isAlive=${p.isAlive}] Stopping forcibly")
                            p.destroyForcibly()
                            printlnDebug("[KorgeReloadAgent] - [isAlive=${p.isAlive}] Done stopping forcibly")
                        })
                        val exit = p.waitFor()
                        printlnDebug("[KorgeReloadAgent] - Exited continuous command with $exit code")
                    } catch (e: Throwable) {
                        printlnDebug("[KorgeReloadAgent] - Continuous command failed with exception '${e.message}'")
                        e.printStackTrace()
                        if (e is InterruptedException) throw e
                    }
                    printlnDebug("[KorgeReloadAgent] Restarting in 5 seconds...")
                    Thread.sleep(5000L)
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            val threadSet = Thread.getAllStackTraces().keys
            printlnDebug("[KorgeReloadAgent] - shutdown: threads=${threadSet.size}")
            printlnDebug("[KorgeReloadAgent] ${threadSet.map { it.name to it.state }}")
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
                    //printlnDebug("KorgeReloaderProcessor.className=$className")
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
                    //printlnDebug("ClassFileTransformer: className=$className, classfileBuffer=${classfileBuffer.size}")
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
        printlnDebug("[KorgeReloadAgent] allClassFiles=${allClassFiles.size}")
        return allClassFiles.filter { it.lastModified() in startTime..endTime }
    }

    fun reloadClassFilesChangedIn(startTime: Long, endTime: Long) {
        val modifiedClassNames = arrayListOf<KorgeReloadAgent.ClassInfo>()
        val allModifiedClassFiles = getAllModifiedClassFiles(startTime, endTime)
        printlnDebug("[KorgeReloadAgent] allModifiedClassFiles=${allModifiedClassFiles.size}")
        for (file in allModifiedClassFiles) {
            val fullPathStr = file.absolutePath.replace("\\", "/")
            val pathRelativeToRoot = getPathRelativeToRoot(fullPathStr)
            if (pathRelativeToRoot != null) {
                modifiedClassNames += KorgeReloadAgent.ClassInfo(
                    File(fullPathStr),
                    pathRelativeToRoot.removeSuffix(".class").replace("/", ".")
                )
            } else {
                printlnDebug("[KorgeReloadAgent] ERROR: couldn't find relative to root: '$fullPathStr' in $cannonicalRootFolders")
            }
        }

        if (modifiedClassNames.isEmpty()) {
            printlnDebug("[KorgeReloadAgent] modifiedClassNames=$modifiedClassNames [EMPTY] STOPPING")
        } else {
            printlnDebug("[KorgeReloadAgent] modifiedClassNames=\n${modifiedClassNames.joinToString("\n")}")
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
                                    //printlnDebug("def.definitionClass.name: canonicalClassName=${canonicalClassName}, classfileBuffer=${def.definitionClassFile.size}, classNameToBytes[canonicalClassName]=${classNameToBytes[canonicalClassName]?.size}")
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
            printlnDebug("[KorgeReloadAgent] reload enableRedefinition=$enableRedefinition, successRedefinition=$successRedefinition, changedDefinitions=${changedDefinitions.size}, classNameToBytes=${classNameToBytes.size}, times[${times.size}]=${times.sum()}ms")

            //korlibs.korge.KorgeReload.triggerReload(changedDefinitions, successRedefinition, rootFolders)
            val triggerReload = Class.forName("korlibs.korge.KorgeReload")
                .getMethod("triggerReload", java.util.List::class.java, java.lang.Boolean.TYPE, java.util.List::class.java)

            triggerReload.invoke(null, changedDefinitions.map { it.definitionClass.name }, successRedefinition, rootFolders)
        }
    }

}

val DEBUG_KORGE_RELOAD_AGENT = System.getenv("DEBUG_KORGE_RELOAD_AGENT") == "true"
fun printlnDebug(msg: String) {
    if (DEBUG_KORGE_RELOAD_AGENT) {
        println(msg)
    }
}
