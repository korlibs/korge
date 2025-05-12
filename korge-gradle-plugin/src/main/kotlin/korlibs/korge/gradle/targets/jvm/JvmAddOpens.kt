package korlibs.korge.gradle.targets.jvm

import korlibs.korge.gradle.targets.*

object JvmAddOpens {
    val beforeJava9 = System.getProperty("java.version").startsWith("1.")

    fun createAddOpensTypedArray(): Array<String> = createAddOpens().toTypedArray()

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

    @OptIn(ExperimentalStdlibApi::class)
    fun createAddOpens(): List<String> = buildList<String> {
        for (item in jvmAddOpensList(mac = isMacos, linux = isLinux)) {
            add("--add-opens=$item=ALL-UNNAMED")
        }
    }
}
