import com.soywiz.korlibs.modules.*

description = "Multiplatform Game Engine written in Kotlin"

plugins {
	java
	`java-gradle-plugin`
	kotlin("jvm")
	`maven-publish`
	id("com.gradle.plugin-publish")
}

group = "com.soywiz.korlibs.korge.plugins"
//com.soywiz.korge:com.soywiz.korge.gradle.plugin
//group = "com.soywiz.korge"
//name = "com.soywiz.korge.gradle.plugin"

//apply(plugin = "kotlin")
//apply(plugin = "org.jetbrains.intellij")

pluginBundle {
	website = "https://korge.soywiz.com/"
	vcsUrl = "https://github.com/korlibs/korge-plugins"
	tags = listOf("korge", "game", "engine", "game engine", "multiplatform", "kotlin")
}

gradlePlugin {
	plugins {
		create("korge") {
			id = "com.soywiz.korge"
			displayName = "Korge"
			description = "Multiplatform Game Engine for Kotlin"
			implementationClass = "com.soywiz.korge.gradle.KorgeGradlePlugin"
		}
	}
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = "1.8"
        sourceCompatibility = "1.8"
        apiVersion = "1.4"
        languageVersion = "1.4"
		//jvmTarget = "1.6"
    }
}

kotlin.sourceSets.main.configure {
    kotlin.srcDir(File(buildDir, "srcgen"))
}

configurePublishing(multiplatform = false)
configureSigning()

//val kotlinVersion: String by project
val androidBuildGradleVersion: String by project
val proguardVersion: String by project

dependencies {
	//implementation(project(":korge-build"))
    implementation(kotlin("gradle-plugin"))
    implementation(kotlin("serialization"))

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
	implementation("net.sf.proguard:proguard-gradle:$proguardVersion")
    implementation("com.android.tools.build:gradle:$androidBuildGradleVersion")

	implementation(gradleApi())
	implementation(localGroovy())

    testImplementation(kotlin("test-junit"))
    testImplementation("io.mockk:mockk:1.11.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.suppressWarnings = true
}


tasks {
    val publishJvmPublicationToMavenLocal by creating(Task::class) {
        dependsOn("publishToMavenLocal")
    }

    val publishAllPublicationsToMavenRepositoryOrNull = project.tasks.findByName("publishAllPublicationsToMavenRepository")

    if (publishAllPublicationsToMavenRepositoryOrNull != null) {
        val publishJvmPublicationToMavenRepository by creating(Task::class) {
            dependsOn(publishAllPublicationsToMavenRepositoryOrNull)
        }
    }

    val jvmTest by creating(Task::class) {
        dependsOn("test")
    }
}
