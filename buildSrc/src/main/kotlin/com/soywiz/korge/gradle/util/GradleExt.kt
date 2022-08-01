package com.soywiz.korge.gradle.util

import org.gradle.api.plugins.PluginContainer

fun PluginContainer.applyOnce(id: String) {
    if (!hasPlugin(id)) {
        apply(id)
    }
}
