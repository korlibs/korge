import korlibs.korge.gradle.targets.android.*
import korlibs.root.*

description = "Multiplatform Game Engine written in Kotlin"
group = RootKorlibsPlugin.KORGE_GRADLE_PLUGIN_GROUP

plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.kotlin.jvm")
}
val jversion = GRADLE_JAVA_VERSION_STR

java {
    setSourceCompatibility(jversion)
    setTargetCompatibility(jversion)
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = jversion
        apiVersion = "1.7"
        languageVersion = "1.7"
    }
}

dependencies {
    implementation(project(":korge-gradle-plugin-common"))
}

gradlePlugin {
    website.set("https://github.com/korlibs/kproject")
    vcsUrl.set("https://github.com/korlibs/kproject")
    //tags = ["kproject", "git"]

    plugins {
        val `korge-settings` by creating {
            //id = "com.soywiz.kproject.settings"
            id = "com.soywiz.korge.settings"
            displayName = "kproject-settings"
            description = "Allows to use sourcecode & git-based dependencies"
            // language=jvm-class-name
            implementationClass = "com.soywiz.kproject.KProjectSettingsPlugin"
        }
    }
}

korlibs.NativeTools.groovyConfigurePublishing(project, false)
korlibs.NativeTools.groovyConfigureSigning(project)

val publishJvmPublicationToMavenLocal = tasks.register("publishJvmPublicationToMavenLocal", Task::class) {
    group = "publishing"
    dependsOn("publishPluginMavenPublicationToMavenLocal")
    dependsOn("publishToMavenLocal")
}

afterEvaluate {
    if (tasks.findByName("publishToMavenRepository") != null) {
        tasks.register("publishJvmPublicationToMavenRepository", Task::class) {
            group = "publishing"
            dependsOn("publishPluginMavenPublicationToMavenRepository")
            dependsOn("publishToMavenRepository")
        }
    }
}
