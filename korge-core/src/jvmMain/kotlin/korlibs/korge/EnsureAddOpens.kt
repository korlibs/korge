package korlibs.korge

import korlibs.io.dynamic.*
import korlibs.platform.*
import java.util.*
import kotlin.system.*

private fun getJavaVersion(): Int {
    val version = System.getProperty("java.version")
    val parts = version.split('.')
    if (parts.first() == "1") return parts.getOrElse(1) { "0" }.toInt()
    return parts.first().toInt()
}

fun jvmAddOpensList(mac: Boolean = true, linux: Boolean = true): List<String> = buildList {
    add("java.desktop/sun.java2d.opengl")
    add("java.desktop/java.awt")
    add("java.desktop/sun.awt")
    if (mac) {
        add("java.desktop/sun.lwawt")
        add("java.desktop/sun.lwawt.macosx")
        add("java.desktop/com.apple.eawt")
        add("java.desktop/com.apple.eawt.event")
    }
    if (linux) {
        add("java.desktop/sun.awt.X11")
    }
}

/** Call this as soon as possible to create a new process with the JVM --add-opens */
fun jvmEnsureAddOpens() {
    val javaVersion = getJavaVersion()
    if (javaVersion <= 8) return
    val processInfo = Dyn.global["java.lang.ProcessHandle"].dynamicInvoke("current").dynamicInvoke("info")
    val cli = processInfo.dynamicInvoke("commandLine").casted<Optional<String>>().orElse(null) ?: return
    val command = processInfo.dynamicInvoke("command").casted<Optional<String>>().orElse(null) ?: return
    val arguments = processInfo.dynamicInvoke("arguments").casted<Optional<Array<String>>>().orElse(null) ?: return
    if (!cli.contains("--add-opens")) {
        println("Java Version $javaVersion, not included --add-opens. Creating a new process...")
        println("CLI: $cli")
        val addOpens = buildList {
            for (item in jvmAddOpensList(mac = Platform.os.isMac, linux = Platform.os.isLinux)) {
                add("--add-opens=$item=ALL-UNNAMED")
            }
        }

        val pb = ProcessBuilder(command, *addOpens.toTypedArray(), *arguments)
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        val p = pb.start()
        exitProcess(p.waitFor())
    }
}
