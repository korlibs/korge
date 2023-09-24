import korlibs.korge.gradle.targets.android.*

plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.kotlin.jvm")
}

description = "Multiplatform Game Engine written in Kotlin"
group = "com.soywiz.korlibs.korge.plugins"
//group = "korlibs.korge"
//this.name = "korlibs.korge.gradle.plugin"

gradlePlugin {
    website.set("https://korge.soywiz.com/")
    vcsUrl.set("https://github.com/korlibs/korge-plugins")
    //tags = ["korge", "game", "engine", "game engine", "multiplatform", "kotlin"]

	plugins {
		create("korge") {
            //PluginDeclaration decl = it
			//id = "korlibs.korge"
            id = "com.soywiz.korge"
			displayName = "Korge"
			description = "Multiplatform Game Engine for Kotlin"
			implementationClass = "korlibs.korge.gradle.KorgeGradlePlugin"
		}
        create("korge-library") {
            //PluginDeclaration decl = it
            //id = "korlibs.korge"
            id = "com.soywiz.korge.library"
            displayName = "Korge"
            description = "Multiplatform Game Engine for Kotlin"
            implementationClass = "korlibs.korge.gradle.KorgeLibraryGradlePlugin"
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
    kotlin.srcDirs(File(projectDir, "build/srcgen"), File(projectDir, "build/srcgen2"))
}
kotlin.sourceSets.test.configure {
    kotlin.srcDirs(File(projectDir, "build/testgen2"))
}
java.sourceSets.main.configure {
    resources.srcDirs(File(projectDir, "build/srcgen2res"))
}

korlibs.NativeTools.groovyConfigurePublishing(project, false)
korlibs.NativeTools.groovyConfigureSigning(project)

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization)

    implementation(libs.proguard.gradle)
    implementation(libs.closure.compiler)
    implementation(libs.gson)
    implementation(libs.gradle.publish.plugin)

    implementation(libs.kover)
    implementation(libs.dokka)

    implementation(libs.android.build.gradle)

    implementation(gradleApi())
	implementation(localGroovy())
    //compileOnly(gradleKotlinDsl())

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("io.mockk:mockk:1.11.0")
    testImplementation("junit:junit:4.13.2")

    //implementation(project(":korge-reload-agent"))
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).configureEach {
    kotlinOptions.suppressWarnings = true
}

//def publishAllPublications = false

val publishJvmPublicationToMavenLocal = tasks.register("publishJvmPublicationToMavenLocal", Task::class) {
    group = "publishing"
    //dependsOn(publishAllPublications ? "publishToMavenLocal" : "publishPluginMavenPublicationToMavenLocal")
    dependsOn("publishPluginMavenPublicationToMavenLocal")
    dependsOn("publishKorgePluginMarkerMavenPublicationToMavenLocal")
}

// publishKorgePluginMarkerMavenPublicationToMavenLocal

afterEvaluate {
    //def publishTaskOrNull = tasks.findByName(publishAllPublications ? "publishAllPublicationsToMavenRepository" : "publishPluginMavenPublicationToMavenRepository")

    if (tasks.findByName("publishKorgePluginMarkerMavenPublicationToMavenRepository") != null) {
        @Suppress("UNUSED_VARIABLE")
        val publishJvmPublicationToMavenRepository = tasks.register("publishJvmPublicationToMavenRepository", Task::class) {
            group = "publishing"
            dependsOn("publishPluginMavenPublicationToMavenRepository")
            dependsOn("publishKorgePluginMarkerMavenPublicationToMavenRepository")
        }
    }
}

val jvmTest = tasks.register("jvmTest", Task::class) {
    dependsOn("test")
}
