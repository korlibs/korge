package com.soywiz.kproject.model

import java.io.IOException

class NewKProjectResolver {
    class DependencyWithProject(val resolver: NewKProjectResolver, val name: String, val dep: Dependency, val project: NewKProjectModel?) {
        override fun toString(): String = "DependencyWithProject(name='$name', dep=$dep, project=$project)"

        val dependencies by lazy {
            project?.dependencies?.map {
                //println(it)
                resolver.getProjectByDependency(it)
            } ?: emptyList()
        }

        val testDependencies by lazy {
            project?.testDependencies?.map {
                //println(it)
                resolver.getProjectByDependency(it)
            } ?: emptyList()
        }

        fun dumpDependenciesToString(): String {
            val out = arrayListOf<String>()
            dumpDependencies { level, name -> out += "${"  ".repeat(level)}$name" }
            return out.joinToString("\n")
        }

        fun dumpDependencies(
            level: Int = 0,
            explored: MutableSet<DependencyWithProject> = mutableSetOf(),
            gen: (level: Int, name: String) -> Unit = { level, name -> println("${"  ".repeat(level)}$name") }
        ) {
            if (this in explored) {
                gen(level, "<recursion detected>")
                return
            }
            explored.add(this)
            gen(level, when {
                this.dep.version != Dependency.MAX_VERSION -> "$name:${this.dep.version}"
                else -> name
            })
            for (deps in listOf(dependencies, testDependencies)) {
                for (dependency in deps) {
                    dependency.dumpDependencies(level + 1, explored, gen)
                }
            }
        }
    }

    private val projectsByFile = LinkedHashMap<FileRef, DependencyWithProject>()
    private val projectsByName = LinkedHashMap<String, DependencyWithProject>()
    private val projectsByDependency = LinkedHashMap<Dependency, DependencyWithProject>()
    private val mavenDependenciesByName = LinkedHashMap<String, DependencyWithProject>()

    fun getProjectNames(): Set<String> = projectsByName.keys
    fun getAllProjects(): Map<String, DependencyWithProject> = projectsByName.toMap()
    fun getAllMavenDependencies(): List<DependencyWithProject> = mavenDependenciesByName.values.toList()

    fun getProjectByName(name: String): DependencyWithProject =
        projectsByName[name] ?: error("Can't find project $name")

    fun getProjectByDependency(dependency: Dependency): DependencyWithProject {
        resolveDependency(dependency)
        return mavenDependenciesByName[dependency.projectName] ?: projectsByDependency[dependency] ?: error("Can't find dependency $dependency")
    }

    fun load(file: FileRef, dep: Dependency = FileRefDependency(file)): DependencyWithProject {
        projectsByFile[file]?.let { return it }

        val project = NewKProjectModel.loadFile(file)
        val projectName = project.name ?: dep.projectName
        val oldProject = projectsByName[projectName]

        if (oldProject == null || dep > oldProject.dep) {
            //println("projectName:$projectName : $dep")
            val depEx = DependencyWithProject(this, projectName, dep, project)
            projectsByName[projectName] = depEx
            projectsByDependency[dep] = depEx
            projectsByFile[file] = depEx
            for (dependency in project.allDependencies) {
                try {
                    resolveDependency(dependency)
                } catch (e: IOException) {
                    throw Exception("Failed to load dependency $dependency referenced in $file ${e.message}", e)
                }
            }
            return depEx
        }

        return oldProject
    }

    fun resolveDependency(dep: Dependency): DependencyWithProject? {
        val fileRef = when (dep) {
            is FileRefDependency -> dep.path
            is GitDependency -> dep.file
            is MavenDependency -> {
                val oldMavenDep = mavenDependenciesByName[dep.projectName]
                if (oldMavenDep == null || dep > oldMavenDep.dep) {
                    mavenDependenciesByName[dep.projectName] = DependencyWithProject(this, dep.projectName, dep, null)
                }
                null
            }
        }
        return fileRef?.let {
            this@NewKProjectResolver.load(
                when {
                    fileRef.name.endsWith("kproject.yml") -> fileRef
                    else -> fileRef["kproject.yml"]
                },
                dep
            )
        }
    }
}
