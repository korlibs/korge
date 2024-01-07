import korlibs.*

description = "Korlibs Datastructure Library"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-datastructure",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-datastructure/LICENSE"
    )
}

dependencies {
    commonMainApi(libs.kotlinx.atomicfu)
    commonTestApi(libs.kotlinx.coroutines.test)
    commonMainApi(project(":korlibs-time"))
    commonMainApi(project(":korlibs-platform"))
}

fun doGenerateKdsTemplates() {
    korlibs.korge.gradle.generate.TemplateGenerator.synchronizeNew(
        File(
            projectDir,
            "src/korlibs/datastructure/Deque.kt"
        ), true, true, true, true, true, true, true, false
    )
    korlibs.korge.gradle.generate.TemplateGenerator.synchronizeNew(
        File(
            projectDir,
            "src/korlibs/datastructure/Array2.kt"
        ), true, true, true, true, true, true, true, true
    )
    korlibs.korge.gradle.generate.TemplateGenerator.synchronizeNew(
        File(
            projectDir,
            "src/korlibs/datastructure/Stack.kt"
        ), false
    )
    korlibs.korge.gradle.generate.TemplateGenerator.synchronizeNew(
        File(
            projectDir,
            "src/korlibs/datastructure/PriorityQueue.kt"
        ), false
    )
}

// Run only with the generate task
//tas
task("generateKdsTemplates") {
    doLast { doGenerateKdsTemplates() }
}

afterEvaluate {
    doGenerateKdsTemplates()
}
