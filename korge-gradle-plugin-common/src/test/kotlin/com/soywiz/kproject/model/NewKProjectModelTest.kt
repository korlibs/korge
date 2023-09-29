package com.soywiz.kproject.model

import com.soywiz.kproject.internal.*
import kotlin.test.*

class NewKProjectModelTest {
    @Test
    fun test() {
        val project = NewKProjectModel.parseObject(Yaml.decode("""
            name: "korge-compose"
            #targets: [jvm, desktop]
            #targets: [jvm, js, desktop, ios]
            #targets: [jvm, js, desktop]
            #targets: [all]
            #targets: [jvm, desktop]
            plugins:
              #- com.soywiz.korge
              - org.jetbrains.compose
            dependencies:
              # https://github.com/JetBrains/compose-jb/releases/tag/v1.3.0
              # https://androidx.dev/storage/compose-compiler/repository
              # https://github.com/JetBrains/compose-jb/issues/2108#issuecomment-1157978869
              #- "maven::common::com.soywiz.korlibs.korge2:korge"
              #- "maven::common::org.jetbrains.compose.runtime:runtime:1.3.3"
              #- "maven::common::com.soywiz.korlibs.korge2:korge:4.0.0-alpha-2"
              #- "maven::common::org.jetbrains.compose.runtime:runtime:1.3.0"
              - "maven::common::com.soywiz.korlibs.korge2:korge"
              - "maven::common::org.jetbrains.compose.runtime:runtime:1.4.0"
        """.trimIndent()))

        assertEquals("korge-compose", project.name)
        assertEquals("GradlePlugin(name=org.jetbrains.compose)", project.plugins.joinToString(",") { it.toString() })
        assertEquals("MavenDependency(group=com.soywiz.korlibs.korge2, name=korge, version=, target=common),MavenDependency(group=org.jetbrains.compose.runtime, name=runtime, version=1.4.0, target=common)", project.dependencies.joinToString(",") { it.toString() })
    }

    @Test
    fun testEdgeCases() {
        assertEquals(emptyList(), NewKProjectModel.parseObject(Yaml.decode("""
            dependencies:
        """.trimIndent())).dependencies)
        assertEquals(emptyList(), NewKProjectModel.parseObject(Yaml.decode("dependencies:")).dependencies)
        assertEquals(emptyList(), NewKProjectModel.parseObject(Yaml.decode("")).dependencies)
    }

    @Test
    fun testHasTarget() {
        checkKProject(
            """
            """
        ) {
            assertEquals("11111", targetsStr())
        }

        checkKProject(
            """
                targets: [jvm]
            """
        ) {
            assertEquals("10000", targetsStr())
        }
    }

    @Test
    fun testVersionSubstitution() {
        checkKProject(
            """
            """
        ) {
            assertEquals(null, versions["com.soywiz.korlibs.korge2:korge"])
        }

        checkKProject(
            """
            targets: [jvm]
            dependencies:
            - https://github.com/korlibs/korge-audio-formats/tree/0.0.1/korau-mod##05abfd6f151fa578e0c690f7725d741784bdfe53
            versions:
            - "com.soywiz.korlibs.korge2:korge": "4.0.0"
            """
        ) {
            assertEquals("4.0.0", versions["com.soywiz.korlibs.korge2:korge"])
        }
    }
    val targets = listOf(KProjectTarget.JVM, KProjectTarget.JS, KProjectTarget.ANDROID, KProjectTarget.DESKTOP, KProjectTarget.MOBILE)
    fun NewKProjectModel.targetsStr(): String = this@NewKProjectModelTest.targets.joinToString("") { if (hasTarget(it)) "1" else "0" }

    fun checkKProject(model: String, block: NewKProjectModel.() -> Unit) {
        NewKProjectModel.parseObject(Yaml.decode(model.trimIndent())).apply(block)
    }
}
