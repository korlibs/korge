import korlibs.korge.gradle.targets.android.*
import korlibs.root.*

plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.kotlin.jvm")
    id("com.github.gmazzo.buildconfig") version "5.3.5"
}

description = "Multiplatform Game Engine written in Kotlin"
group = RootKorlibsPlugin.KORGE_GRADLE_PLUGIN_GROUP
//group = "korlibs.korge"
//this.name = "korlibs.korge.gradle.plugin"

dependencies {
    implementation(kotlin("gradle-plugin-api"))
    implementation(project(":korge-gradle-plugin-common"))
}

gradlePlugin {
    website.set("https://korge.soywiz.com/")
    vcsUrl.set("https://github.com/korlibs/korge-plugins")
    //tags = ["korge", "game", "engine", "game engine", "multiplatform", "kotlin"]

	plugins {
        val korge by creating {
            //PluginDeclaration decl = it
			//id = "korlibs.korge"
            id = "com.soywiz.korge"
			displayName = "Korge"
			description = "Multiplatform Game Engine for Kotlin"
			implementationClass = "korlibs.korge.gradle.KorgeGradlePlugin"
		}
        val `korge-library` by creating {
            //PluginDeclaration decl = it
            //id = "korlibs.korge"
            id = "com.soywiz.korge.library"
            displayName = "Korge Library"
            description = "Multiplatform Game Engine for Kotlin"
            implementationClass = "korlibs.korge.gradle.KorgeLibraryGradlePlugin"
        }
        val `korge-kotlin-plugin` by creating {
            //PluginDeclaration decl = it
            //id = "korlibs.korge"
            id = "com.soywiz.korge.kotlinplugin"
            displayName = "Korge Kotlin Plugin"
            description = "Multiplatform Game Engine for Kotlin"
            implementationClass = "korlibs.korge.kotlin.plugin.KorgeKotlinCompilerPlugin"
        }

        val kproject by creating {
            id = "com.soywiz.kproject"
            displayName = "kproject"
            description = "Allows to use sourcecode & git-based dependencies"
            implementationClass = "com.soywiz.kproject.KProjectPlugin"
        }
        val kprojectRoot by creating {
            id = "com.soywiz.kproject.root"
            displayName = "kproject"
            description = "Allows to use sourcecode & git-based dependencies"
            implementationClass = "com.soywiz.kproject.KProjectRootPlugin"
        }
	}
}

/*
afterEvaluate {
    GenerateMavenPom generatePomFileForKorgePluginMarkerMavenPublication = tasks.findByName("generatePomFileForKorgePluginMarkerMavenPublication")
    generatePomFileForKorgePluginMarkerMavenPublication.pom.licenses {
        it.license {
            it.name = "MIT"
        }
    }
}
*/

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

kotlin.sourceSets.main.configure {
    kotlin.srcDirs(File(projectDir, "build/srcgen"))
}

dependencies {
    //implementation(project(":korge-gradle-plugin-common"))

    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization)

    implementation(libs.proguard.gradle)
    implementation(libs.gson)
    implementation(libs.gradle.publish.plugin)

    implementation(libs.kover)
    implementation(libs.dokka)

    implementation(libs.android.build.gradle)

    implementation(gradleApi())
	implementation(localGroovy())
    //compileOnly(gradleKotlinDsl())

    testImplementation(libs.bundles.kotlin.test)

    //implementation(project(":korge-reload-agent"))
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).configureEach {
    kotlinOptions.suppressWarnings = true
}

//def publishAllPublications = false

tasks {
    val publishJvmPublicationToMavenLocal by creating(Task::class) {
        group = "publishing"
        //dependsOn(publishAllPublications ? "publishToMavenLocal" : "publishPluginMavenPublicationToMavenLocal")
        dependsOn("publishPluginMavenPublicationToMavenLocal")
        dependsOn("publishToMavenLocal")
    }
}

// publishKorgePluginMarkerMavenPublicationToMavenLocal

afterEvaluate {
    //def publishTaskOrNull = tasks.findByName(publishAllPublications ? "publishAllPublicationsToMavenRepository" : "publishPluginMavenPublicationToMavenRepository")

    if (tasks.findByName("publishAllPublicationsToMavenRepository") != null) {
        @Suppress("UNUSED_VARIABLE")
        val publishJvmPublicationToMavenRepository = tasks.register("publishJvmPublicationToMavenRepository", Task::class) {
            group = "publishing"
            dependsOn("publishPluginMavenPublicationToMavenRepository")
            dependsOn("publishAllPublicationsToMavenRepository")
        }
    }
}

tasks { val jvmTest by creating { dependsOn("test") } }

korlibs.NativeTools.groovyConfigurePublishing(project, false)
korlibs.NativeTools.groovyConfigureSigning(project)

buildConfig {
    //val project = project(":korge-kotlin-plugin")
    packageName("korlibs.korge.gradle.plugin")
    //buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlin_plugin_id"]}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"com.soywiz.korge.korge-kotlin-plugin\"")
    //buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${project.group}\"")
    //buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${project.name}\"")
    //buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${project.version}\"")
}

afterEvaluate {
    //tasks.getByName("sourceJar").dependsOn("generateBuildConfig")
}
