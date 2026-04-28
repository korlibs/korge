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
    id("com.vanniktech.maven.publish")
}
val jversion = GRADLE_JAVA_VERSION_STR

java {
    setSourceCompatibility(jversion)
    setTargetCompatibility(jversion)
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(jversion))
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion("2.1"))
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion("2.1"))
    }
}

dependencies {
    implementation(project(":korge-gradle-plugin-common"))
    testImplementation(libs.bundles.kotlin.test)
}

gradlePlugin {
    website.set("https://github.com/korlibs/kproject")
    vcsUrl.set("https://github.com/korlibs/kproject")
    //tags = ["kproject", "git"]

    plugins {
        val `korge-settings` by creating {
            id = "org.korge.engine.settings"
            displayName = "KProject Settings Gradle Plugin"
            description = "Allows to use sourcecode & git-based dependencies"
            // language=jvm-class-name
            implementationClass = "org.korge.kproject.KProjectSettingsPlugin"
        }
    }
}

// GradlePlugin type is auto-detected by vanniktech because java-gradle-plugin is applied
mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)

    pom {
        name.set("korge-gradle-plugin-settings")
        description.set("Multiplatform Game Engine written in Kotlin – Settings Gradle Plugin")
        url.set("https://github.com/korlibs/korge")
        licenses {
            license {
                name.set("MIT")
                url.set("https://raw.githubusercontent.com/korlibs/korge/master/LICENSE")
            }
        }
        developers {
            developer {
                id.set("korge")
                name.set("KorGE Team")
                email.set("info@korge.org")
            }
        }
        scm {
            url.set("https://github.com/korlibs/korge")
        }
    }
}

korlibs.NativeTools.groovyConfigureSigning(project)

val publishJvmPublicationToMavenLocal = tasks.register("publishJvmPublicationToMavenLocal", Task::class) {
    group = "publishing"
    dependsOn("publishPluginMavenPublicationToMavenLocal")
    dependsOn("publishToMavenLocal")
}

afterEvaluate {
    if (tasks.findByName("publishAllPublicationsToMavenRepository") != null) {
        tasks.register("publishJvmPublicationToMavenRepository", Task::class) {
            group = "publishing"
            dependsOn("publishPluginMavenPublicationToMavenRepository")
            dependsOn("publishAllPublicationsToMavenRepository")
        }
    }
}

tasks { val jvmTest by creating { dependsOn("test") } }
