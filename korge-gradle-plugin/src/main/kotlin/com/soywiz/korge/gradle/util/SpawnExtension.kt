package com.soywiz.korge.gradle.util

import org.gradle.api.*
import java.io.*

open class SpawnExtension {
    open fun spawn(dir: File, command: List<String>) {
        ProcessBuilder(command).redirectErrorStream(true).directory(dir).start()
    }
}

var Project.spawnExt: SpawnExtension by projectExtension { SpawnExtension() }
