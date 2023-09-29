package com.soywiz.kproject

import kproject
import org.gradle.api.*
import org.gradle.api.initialization.*
import java.io.*

@Suppress("unused")
class KProjectSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        //println("KProjectSettingsPlugin: $settings")
        if (File(settings.rootDir, "deps.kproject.yml").exists()) {
            settings.kproject("./deps")
        }
    }
}
