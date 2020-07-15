plugins {
	java
	`java-gradle-plugin`
	kotlin("jvm")
	maven
	`maven-publish`
	id("com.gradle.plugin-publish")
}

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
        create("korge-android") {
            id = "com.soywiz.korge.android"
            displayName = "KorgeAndroid"
            description = "Multiplatform Game Engine for Kotlin with integrated android support"
            implementationClass = "com.soywiz.korge.gradle.KorgeWithAndroidGradlePlugin"
        }
	}
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        //jvmTarget = "1.8"
		jvmTarget = "1.6"
    }
}

val kotlinVersion: String by project
val androidBuildGradleVersion: String by project

dependencies {
	implementation(project(":korge-build"))

	implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
	implementation("net.sf.proguard:proguard-gradle:6.2.2")
    implementation("com.android.tools.build:gradle:$androidBuildGradleVersion")

	implementation(gradleApi())
	implementation(localGroovy())
}
