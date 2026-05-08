package org.korge.kproject

import org.korge.kproject.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class KProjectRootPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.allprojects {
            it.defineStandardRepositories()
        }
    }
}
