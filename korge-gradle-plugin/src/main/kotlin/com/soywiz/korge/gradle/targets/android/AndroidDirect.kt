package com.soywiz.korge.gradle.targets.android

import com.android.build.gradle.internal.dsl.*
import com.android.builder.core.*
import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.util.*
import org.gradle.api.*

fun Project.configureAndroidDirect() {
    project.ensureAndroidLocalPropertiesWithSdkDir()

    project.plugins.apply("com.android.application")

    val android = project.extensions.getByType(BaseAppModuleExtension::class.java)
    //val android = project.extensions.getByName("android")

    project.kotlin.android()

    project.afterEvaluate {
        //println("@TODO: Info is not generated")
        writeAndroidManifest(project.rootDir, project.korge, AndroidInfo(null))
    }

    android.apply {
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        adbOptions {
            installOptions = listOf("-r")
            timeOutInMs = (30 * 1000)
        }
        packagingOptions {
            for (pattern in androidExcludePatterns()) {
                this.exclude(pattern)
            }
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
                storeFile = project.file(project.findProperty("RELEASE_STORE_FILE") ?: korge.androidReleaseSignStoreFile)
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD")?.toString() ?: korge.androidReleaseSignStorePassword
                keyAlias = project.findProperty("RELEASE_KEY_ALIAS")?.toString() ?: korge.androidReleaseSignKeyAlias
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD")?.toString() ?: korge.androidReleaseSignKeyPassword
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
        sourceSets {
            maybeCreate("main").also {
                val (resourcesSrcDirs, kotlinSrcDirs) = androidGetResourcesFolders()
                //println("@ANDROID_DIRECT:")
                //println(resourcesSrcDirs.joinToString("\n"))
                //println(kotlinSrcDirs.joinToString("\n"))
                it.assets.srcDirs(*resourcesSrcDirs.map { it.absoluteFile }.toTypedArray())
                it.java.srcDirs(*kotlinSrcDirs.map { it.absoluteFile }.toTypedArray())
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
                    if (isKorlibsDependency(cleanFullName)) {
                        resolvedArtifacts[cleanFullName] = it.requested.version.toString()
                    }
                }
            }

            for ((name, version) in resolvedArtifacts) {
                if (name.startsWith("org.jetbrains.kotlin")) continue
                if (name.contains("-metadata")) continue
                //if (name.startsWith("com.soywiz.korlibs.krypto:krypto")) continue
                if (name.startsWith("com.soywiz.korlibs.korge2:korge")) {
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

    installAndroidRun(listOf(), direct = true)

    //println("android: ${android::class.java}")
    /*
    line("defaultConfig") {
    }
    */
    //project.plugins.apply("kotlin-android")
    //project.plugins.apply("kotlin-android-extensions")
    //for (res in project.getResourcesFolders()) println("- $res")
}
