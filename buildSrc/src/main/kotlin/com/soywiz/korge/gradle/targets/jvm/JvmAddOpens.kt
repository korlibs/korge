package com.soywiz.korge.gradle.targets.jvm

import com.soywiz.korge.gradle.targets.isLinux
import com.soywiz.korge.gradle.targets.isMacos
import java.util.ArrayList

object JvmAddOpens {
    val beforeJava9 = System.getProperty("java.version").startsWith("1.")

    fun createAddOpensTypedArray(): Array<String> = createAddOpens().toTypedArray()

    @OptIn(ExperimentalStdlibApi::class)
    fun createAddOpens(): List<String> = buildList<String> {
        add("--add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED")
        add("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
        add("--add-opens=java.desktop/sun.awt=ALL-UNNAMED")
        if (isMacos) {
            add("--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED")
            add("--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
            add("--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED")
        }
        if (isLinux) add("--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED")
    }
}
