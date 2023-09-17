package korlibs.korge.gradle

import korlibs.*
import org.gradle.api.*
import org.gradle.api.artifacts.*

fun Project.directDependantProjects(): Set<Project> {
    val key = "directDependantProjects"
    if (!project.selfExtra.has(key)) {
    //if (true) {
        project.selfExtra.set(key, (project.configurations
            .flatMap { it.dependencies.withType(ProjectDependency::class.java).toList() }
            .map { it.dependencyProject }
            .toSet() - project))
    }
    return project.selfExtra.get(key) as Set<Project>
}

fun Project.allDependantProjects(): Set<Project> {
    val key = "allDependantProjects"
    if (!project.selfExtra.has(key)) {
    //if (true) {
        val toExplore = arrayListOf<Project>(this)
        val out = LinkedHashSet<Project>()
        val explored = LinkedHashSet<Project>()
        while (toExplore.isNotEmpty()) {
            val item = toExplore.removeLast()
            if (item in explored) continue
            val directDependencies = item.directDependantProjects()
            explored += item
            out += directDependencies
            toExplore += directDependencies
        }
        project.selfExtra.set(key, out - this)
    }
    return project.selfExtra.get(key) as Set<Project>
}
