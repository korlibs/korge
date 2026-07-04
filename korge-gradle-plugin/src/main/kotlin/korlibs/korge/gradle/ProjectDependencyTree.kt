package korlibs.korge.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.internal.extensions.core.extra

fun Project.directDependantProjects(): Set<Project> {
    val key = "directDependantProjects"
    if (!project.extra.has(key)) {
        project.extra.set(key, (project.configurations
            .flatMap { it.dependencies.withType(ProjectDependency::class.java).toList() }
            .map { project.rootProject.project(it.path) }
            .toSet() - project))
    }
    return project.extra.get(key) as Set<Project>
}

fun Project.allDependantProjects(): Set<Project> {
    val key = "allDependantProjects"
    if (!project.extra.has(key)) {
        val toExplore = arrayListOf(this)
        val out = LinkedHashSet<Project>()
        val explored = LinkedHashSet<Project>()
        while (toExplore.isNotEmpty()) {
            val item = toExplore.removeAt(toExplore.size - 1)
            if (item in explored) continue
            val directDependencies = item.directDependantProjects()
            explored += item
            out += directDependencies
            toExplore += directDependencies
        }
        project.extra.set(key, out - this)
    }
    return project.extra.get(key) as Set<Project>
}
