
import korlibs.*
import korlibs.korge.gradle.generate.*

description = "Korge Foundation Libraries"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties("https://github.com/korlibs/korge/korge-foundation",
        "Apache 2.0",
        "https://raw.githubusercontent.com/korlibs/korge/master/korge-foundation/LICENSE"
    )
}

dependencies {
    add("jvmMainApi", libs.bundles.jna)
    add("commonMainApi", libs.kotlinx.coroutines.core)
    add("commonTestApi", libs.kotlinx.coroutines.test)
}

//korlibs.korge.gradle.generate.TemplateGenerator.synchronize(new File(projectDir, "template"))

fun doGenerateKdsTemplates() {
    TemplateGenerator.synchronizeNew(File(projectDir, "src/common/korlibs/datastructure/Deque.kt"), true, true, true, true, true, true, true, false)
    TemplateGenerator.synchronizeNew(File(projectDir, "src/common/korlibs/datastructure/Array2.kt"), true, true, true, true, true, true, true, true)
    TemplateGenerator.synchronizeNew(File(projectDir, "src/common/korlibs/datastructure/Stack.kt"), false)
    TemplateGenerator.synchronizeNew(File(projectDir, "src/common/korlibs/datastructure/PriorityQueue.kt"), false)
}

// Run only with the generate task
//tas
task("generateKdsTemplates") {
    doLast { doGenerateKdsTemplates() }
}

afterEvaluate {
    doGenerateKdsTemplates()
}
