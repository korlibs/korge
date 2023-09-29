package com.soywiz.kproject.util

import com.soywiz.kproject.model.*
import kotlin.test.*

class NewKProjectResolverTest {
    @Test
    fun testResolver() {
        val files = MemoryFiles().root
        files["demo/kproject.yml"] = """
            dependencies:
              - ../demo2
        """.trimIndent()
        files["demo2/kproject.yml"] = """
            name: Ademo2
            dependencies:
              - ../demo3.kproject.yml
              - ../demo4
        """.trimIndent()
        files["demo4/kproject.yml"] = """
            name: Ademo4
        """.trimIndent()
        files["demo3.kproject.yml"] = """
            name: Ademo3
            dependencies:
              - ../demo
        """.trimIndent()
        val resolver = NewKProjectResolver()
        val mainProject = resolver.load(files["demo/kproject.yml"])
        assertEquals(
            """
                demo
                  Ademo2
                    Ademo3
                      <recursion detected>
                    Ademo4
            """.trimIndent(),
            mainProject.dumpDependenciesToString()
        )
        //println(mainProject)
        /*
        println("---")
        for (dep in mainProject.dependencies) {
            println(resolver.getProjectByDependency(dep))
        }
        println("---")
        for (dep in resolver.getProjectByName("Ademo2").project.dependencies) {
            println(resolver.getProjectByDependency(dep))
        }
        println("---")
        println(resolver.getAllProjects().values.joinToString("\n"))
        */
        assertEquals(listOf("demo", "Ademo2", "Ademo3", "Ademo4"), resolver.getProjectNames().toList())
        val paths = resolver.getAllProjects().map {
            it.key to ((it.value.dep as? FileRefDependency?)?.path as? MemoryFileRef?)?.path?.fullPath
        }.toMap()

        assertEquals(mapOf(
            "demo" to "/demo/kproject.yml",
            "Ademo2" to "/demo2/kproject.yml",
            "Ademo3" to "/demo3.kproject.yml",
            "Ademo4" to "/demo4/kproject.yml",
        ), paths)
    }

    @Test
    fun testVersionConflict() {
        val files = MemoryFiles().root
        files["demo/kproject.yml"] = """
            dependencies:
              - ../demo2
              -"maven::common::org.jetbrains.compose.runtime:runtime:1.4.0"
        """.trimIndent()
        files["demo2/kproject.yml"] = """
            name: Ademo2
            dependencies:
              - ../demo3
              - "maven::common::org.jetbrains.compose.runtime:runtime:1.4.2"
        """.trimIndent()
        files["demo3/kproject.yml"] = """
            name: Ademo3
            dependencies:
              - "maven::common::org.jetbrains.compose.runtime:runtime:1.4.1"
        """.trimIndent()

        val resolver = NewKProjectResolver()
        val mainProject = resolver.load(files["demo/kproject.yml"])
        assertEquals(
            """
                demo
                  Ademo2
                    Ademo3
                      org.jetbrains.compose.runtime-runtime:1.4.2
                    <recursion detected>
                  <recursion detected>
            """.trimIndent(),
            mainProject.dumpDependenciesToString()
        )
        assertEquals(listOf("demo", "Ademo2", "Ademo3"), resolver.getProjectNames().toList())

        assertEquals(
            listOf(
                MavenDependency(group="org.jetbrains.compose.runtime", name="runtime", version="1.4.2".version, target="common")
            ),
            resolver.getAllMavenDependencies().map { it.dep }
        )

        val out = arrayListOf<String>().also { it ->
            for (project in resolver.getAllProjects().values) {
                it.add("${project.name}:")
                for (dep in project.dependencies) {
                    it.add(" - ${dep.dep}")
                }
            }
        }
        assertEquals(
            """
                demo:
                 - FileRefDependency(path=MemoryFileRef(files=MemoryFiles[3], path=PathInfo(/demo2/kproject.yml)))
                 - MavenDependency(group=org.jetbrains.compose.runtime, name=runtime, version=1.4.2, target=common)
                Ademo2:
                 - FileRefDependency(path=MemoryFileRef(files=MemoryFiles[3], path=PathInfo(/demo3/kproject.yml)))
                 - MavenDependency(group=org.jetbrains.compose.runtime, name=runtime, version=1.4.2, target=common)
                Ademo3:
                 - MavenDependency(group=org.jetbrains.compose.runtime, name=runtime, version=1.4.2, target=common)
             """.trimIndent(),
            out.joinToString("\n")
        )

        val paths = resolver.getAllProjects().map {
            it.key to ((it.value.dep as? FileRefDependency?)?.path as? MemoryFileRef?)?.path?.fullPath
        }.toMap()

        assertEquals(mapOf(
            "demo" to "/demo/kproject.yml",
            "Ademo2" to "/demo2/kproject.yml",
            "Ademo3" to "/demo3/kproject.yml",
        ), paths)
    }

    @Test
    fun testResolverWithGit() {
        val files = MemoryFiles().root
        files["demo/kproject.yml"] = """
            dependencies:
              - "https://github.com/korlibs/kproject.git/samples/demo2#95696dd942ebc8db4ee9d9f4835ce12d853ff16f"
        """.trimIndent()
        val resolver = NewKProjectResolver()
        val mainProject = resolver.load(files["demo/kproject.yml"])
        assertEquals(
            """
                demo
                  Ademo2:95696dd942ebc8db4ee9d9f4835ce12d853ff16f
                    Ademo3
                      org.jetbrains.compose.runtime-runtime:1.4.1
            """.trimIndent(),
            mainProject.dumpDependenciesToString()
        )
        assertEquals(listOf("demo", "Ademo2", "Ademo3"), resolver.getProjectNames().toList())
        val paths = resolver.getAllProjects().map {
            it.key to ((it.value.dep as? FileRefDependency?)?.path as? MemoryFileRef?)?.path?.fullPath
        }.toMap()

        assertEquals(mapOf(
            "demo" to "/demo/kproject.yml",
            "Ademo2" to null,
            "Ademo3" to null,
        ), paths)
    }
}
