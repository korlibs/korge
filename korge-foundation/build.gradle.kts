import korlibs.applyProjectProperties
import korlibs.korge.gradle.generate.TemplateGenerator
import korlibs.tasks

description = "Korge Foundation Libraries"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties("https://github.com/korlibs/korge/korge-foundation",
        "Apache 2.0",
        "https://raw.githubusercontent.com/korlibs/korge/master/korge-foundation/LICENSE"
    )
}

dependencies {
    //add("commonMainImplementation", "org.jetbrains.kotlinx:atomicfu:${libs.versions.kotlinx.atomicfu.get()}")
    //add("commonMainApi", "org.jetbrains.kotlinx:atomicfu:${libs.versions.kotlinx.atomicfu.get()}")
    //add("jvmTestApi", "org.powermock:powermock-mockito-release-full:1.6.4")
    //add("jvmTestApi", "org.fuin:units4j:0.8.4")
    //add("jvmTestApi", "org.ow2.asm:asm:8.0.1")
    add("commonMainApi", libs.kotlinx.coroutines.core)
    add("commonMainApi", libs.kotlinx.atomicfu)
    add("commonTestApi", libs.kotlinx.coroutines.test)
    add("jvmMainApi", libs.bundles.jna)
}

//korlibs.korge.gradle.generate.TemplateGenerator.synchronize(new File(projectDir, "template"))

fun doGenerateKdsTemplates() {
    TemplateGenerator.synchronizeNew(File(projectDir, "src/commonMain/kotlin/korlibs/datastructure/Deque.kt"), true, true, true, true, true, true, true, false)
    TemplateGenerator.synchronizeNew(File(projectDir, "src/commonMain/kotlin/korlibs/datastructure/Array2.kt"), true, true, true, true, true, true, true, true)
    TemplateGenerator.synchronizeNew(File(projectDir, "src/commonMain/kotlin/korlibs/datastructure/Stack.kt"), false)
    TemplateGenerator.synchronizeNew(File(projectDir, "src/commonMain/kotlin/korlibs/datastructure/PriorityQueue.kt"), false)
}

// Run only with the generate task
//tas
task("generateKdsTemplates") {
    doLast { doGenerateKdsTemplates() }
}

afterEvaluate {
    doGenerateKdsTemplates()
}
