package com.soywiz.korge.gradle

import com.android.build.gradle.internal.crash.afterEvaluate
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.builder.core.DefaultApiVersion
import com.soywiz.korge.gradle.targets.android.hasAndroidConfigured
import com.soywiz.korge.gradle.targets.android.tryToDetectAndroidSdkPath
import com.soywiz.korge.gradle.targets.android.writeAndroidManifest
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

open class KorgeWithAndroidGradlePlugin : Plugin<Project> {
	override fun apply(project: Project) {
		if (!hasAndroidConfigured) {
			val path = project.tryToDetectAndroidSdkPath()
			if (path != null) {
				File(project.rootDir, "local.properties").writeText("sdk.dir=$path")
			}
		}

		val korge = project.korge

		KorgeGradleApply(project).apply(includeIndirectAndroid = korge.androidLibrary)

		project.repositories.apply {
			google()
		}
		project.plugins.apply("com.android.application")

		val android = project.extensions.getByType(BaseAppModuleExtension::class.java)
		//val android = project.extensions.getByName("android")

		project.kotlin.android()

		project.afterEvaluate {
			writeAndroidManifest(project.rootDir, project.korge)
		}

		android.apply {
			packagingOptions {
				it.exclude("META-INF/DEPENDENCIES")
				it.exclude("META-INF/LICENSE")
				it.exclude("META-INF/LICENSE.txt")
				it.exclude("META-INF/license.txt")
				it.exclude("META-INF/NOTICE")
				it.exclude("META-INF/NOTICE.txt")
				it.exclude("META-INF/notice.txt")
				it.exclude("META-INF/LGPL*")
				it.exclude("META-INF/AL2.0")
				it.exclude("META-INF/*.kotlin_module")
				it.exclude("**/*.kotlin_metadata")
				it.exclude("**/*.kotlin_builtins")
			}
			compileSdkVersion(28)
			defaultConfig {
				it.multiDexEnabled = true
				it.applicationId = project.korge.id
				it.minSdkVersion = DefaultApiVersion(19)
				it.targetSdkVersion = DefaultApiVersion(28)
				it.versionCode = 1
				it.versionName = "1.0"
				it.testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
				//val manifestPlaceholdersStr = korge.configs.map { it.key + ":" + it.value.quoted }.joinToString(", ")
				//manifestPlaceholders = if (manifestPlaceholdersStr.isEmpty()) "[:]" else "[$manifestPlaceholdersStr]" }
			}
			signingConfigs {
				it.maybeCreate("release").apply {
					storeFile = project.file(project.findProperty("RELEASE_STORE_FILE") ?: "korge.keystore")
					storePassword = project.findProperty("RELEASE_STORE_PASSWORD")?.toString() ?: "password"
					keyAlias = project.findProperty("RELEASE_KEY_ALIAS")?.toString() ?: "korge"
					keyPassword = project.findProperty("RELEASE_KEY_PASSWORD")?.toString() ?: "password"
				}
			}
			buildTypes {
				it.maybeCreate("debug").apply {
					isMinifyEnabled = false
					signingConfig = signingConfigs.getByName("release")
				}
				it.maybeCreate("release").apply {
					isMinifyEnabled = true
					proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
					signingConfig = signingConfigs.getByName("release")
				}
			}
		}
		project.dependencies.apply {
			add("implementation", project.fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
			add("implementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${BuildVersions.KOTLIN}")
			add("implementation", "com.android.support:multidex:1.0.3")

			//line("api 'org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion'")
			project.afterEvaluate {
			//run {

				val resolvedArtifacts = LinkedHashMap<String, String>()

				project.configurations.all {
					it.resolutionStrategy.eachDependency {
						val cleanFullName = "${it.requested.group}:${it.requested.name}".removeSuffix("-js").removeSuffix("-jvm")
						//println("RESOLVE ARTIFACT: ${it.requested}")
						//if (cleanFullName.startsWith("org.jetbrains.intellij.deps:trove4j")) return@eachDependency
						//if (cleanFullName.startsWith("org.jetbrains:annotations")) return@eachDependency
						if (cleanFullName.startsWith("org.jetbrains")) return@eachDependency
						if (cleanFullName.startsWith("junit:junit")) return@eachDependency
						if (cleanFullName.startsWith("org.hamcrest:hamcrest-core")) return@eachDependency
						if (cleanFullName.startsWith("org.jogamp")) return@eachDependency
						resolvedArtifacts[cleanFullName] = it.requested.version.toString()
					}
				}

				for ((name, version) in resolvedArtifacts) {
					if (name.startsWith("org.jetbrains.kotlin")) continue
					if (name.contains("-metadata")) continue
					//if (name.startsWith("com.soywiz.korlibs.krypto:krypto")) continue
					if (name.startsWith("com.soywiz.korlibs.korge:korge")) {
						add("implementation", "$name-android:$version")
					}
				}

				for (dependency in korge.plugins.pluginExts.getAndroidDependencies()) {
					add("implementation", dependency)
				}
			}

			add("implementation", "com.android.support:appcompat-v7:28.0.0")
			add("implementation", "com.android.support.constraint:constraint-layout:1.1.3")
			add("testImplementation", "junit:junit:4.12")
			add("androidTestImplementation", "com.android.support.test:runner:1.0.2")
			add("androidTestImplementation", "com.android.support.test.espresso:espresso-core:3.0.2")
			//line("implementation 'com.android.support:appcompat-v7:28.0.0'")
			//line("implementation 'com.android.support.constraint:constraint-layout:1.1.3'")
			//line("testImplementation 'junit:junit:4.12'")
			//line("androidTestImplementation 'com.android.support.test:runner:1.0.2'")
			//line("androidTestImplementation 'com.android.support.test.espresso:espresso-core:3.0.2'")
		}

		//println("android: ${android::class.java}")
		/*
		line("defaultConfig") {
		}
		*/
		//project.plugins.apply("kotlin-android")
		//project.plugins.apply("kotlin-android-extensions")
		//for (res in project.getResourcesFolders()) println("- $res")
	}
}
