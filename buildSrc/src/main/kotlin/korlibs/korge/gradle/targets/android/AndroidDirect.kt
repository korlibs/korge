package korlibs.korge.gradle.targets.android

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationVariantDimension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.TestExtension
import com.android.build.gradle.tasks.*
import java.io.File
import korlibs.*
import korlibs.korge.gradle.*
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.all.*
import korlibs.korge.gradle.targets.jvm.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

fun Project.configureAndroidDirect(projectType: ProjectType, isKorge: Boolean) {
    if (!AndroidSdk.hasAndroidSdk(this)) {
        logger.info("Couldn't find ANDROID SDK, do not configuring android")
        return
    }

    project.ensureAndroidLocalPropertiesWithSdkDir()

    if (projectType.isExecutable) configureAndroidExecutable(isKorge)
    else configureAndroidLibrary(isKorge)

    if (projectType.isExecutable) {
        installAndroidRun(listOf(), direct = true, isKorge = isKorge)
    }

    afterEvaluate {
        val jvmProcessResources = tasks.findByName("jvmProcessResources") as? Copy?
        if (jvmProcessResources != null) {
            jvmProcessResources.duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE
            val packageDebugAssets = tasks.findByName("packageDebugAssets") as MergeSourceSetFolders?
            val packageReleaseAssets = tasks.findByName("packageReleaseAssets") as MergeSourceSetFolders?

            // @TODO: Why is this required with Gradle 8.1.1?
            packageDebugAssets?.dependsOn(jvmProcessResources) // @TODO: <-- THIS
            packageReleaseAssets?.dependsOn(jvmProcessResources) // @TODO: <-- THIS
        }
        val compileDebugJavaWithJavac = project.tasks.findByName("compileDebugJavaWithJavac") as? org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile?
        compileDebugJavaWithJavac?.compilerOptions?.jvmTarget?.set(ANDROID_JVM_TARGET)
    }
}

private fun Project.configureAndroidExecutable(isKorge: Boolean) {
    project.plugins.apply("com.android.application")
    val android = extensions.findByType(ApplicationExtension::class.java)

    println("Application: $android, ${extensions}")
    android?.apply {
        val androidGenerated = project.toAndroidGenerated(isKorge)
        namespace = androidGenerated.getNamespace(isKorge)

        compileSdk = if (isKorge) project.korge.androidCompileSdk else project.getAndroidCompileSdkVersion()

//        (this as CommonExtension).installation.apply {
//            // @TODO: Android Build Gradle newer version
//            installOptions("-r")
//            timeOutInMs = project.korge.androidTimeoutMs
//        }

        compileOptions.apply {
            sourceCompatibility = ANDROID_JAVA_VERSION
            targetCompatibility = ANDROID_JAVA_VERSION
        }

        buildFeatures.apply {
            if (project.name == "korlibs-platform") {
                buildConfig = true
            }
        }

        packagingOptions.also {
            for (pattern in when {
                isKorge -> project.korge.androidExcludePatterns
                else -> KorgeExtension.DEFAULT_ANDROID_EXCLUDE_PATTERNS - KorgeExtension.DEFAULT_ANDROID_INCLUDE_PATTERNS_LIBS
            }) {
                it.resources.excludes.add(pattern)
            }
        }

        defaultConfig.also {
            it.multiDexEnabled = true
            it.applicationId = androidGenerated.getAppId(isKorge)
            it.minSdk = if (isKorge) project.korge.androidMinSdk else project.getAndroidMinSdkVersion()
            it.targetSdk = if (isKorge) project.korge.androidTargetSdk else project.getAndroidTargetSdkVersion()
            it.versionCode = if (isKorge) project.korge.versionCode else 1
            it.versionName = if (isKorge) project.korge.version else "1.0"
            it.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            it.manifestPlaceholders.clear()
            it.manifestPlaceholders.putAll(if (isKorge) korge.configs else emptyMap())
        }

        lintOptions.apply {
            checkOnly()
            //checkReleaseBuilds = false
        }

        // TODO Remove this lint option configuration or remove with above
        lintOptions.also {
            // @TODO: ../../build.gradle: All com.android.support libraries must use the exact same version specification (mixing versions can lead to runtime crashes). Found versions 28.0.0, 26.1.0. Examples include com.android.support:animated-vector-drawable:28.0.0 and com.android.support:customtabs:26.1.0
            it.disable("GradleCompatible")
        }

        signingConfigs.apply {
            maybeCreate("release").apply {
                storeFile = if (isKorge) project.file(project.findProperty("RELEASE_STORE_FILE") ?: korge.androidReleaseSignStoreFile) else File(korgeGradlePluginResources, "korge.keystore")
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD")?.toString() ?: (if (isKorge) korge.androidReleaseSignStorePassword else "password")
                keyAlias = project.findProperty("RELEASE_KEY_ALIAS")?.toString() ?: (if (isKorge) korge.androidReleaseSignKeyAlias else "korge")
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD")?.toString() ?: (if (isKorge) korge.androidReleaseSignKeyPassword else "password")
            }
        }
        buildTypes.apply {
            maybeCreate("debug").apply {
                (this as ApplicationVariantDimension).signingConfig = signingConfigs.getByName("release")
                isMinifyEnabled = false
            }
            maybeCreate("release").apply {
                (this as ApplicationVariantDimension).signingConfig = signingConfigs.getByName("release")
                isMinifyEnabled = true // for libraries, this would make the library to be empty
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                File(rootDir, "proguard-rules.pro").takeIfExists()?.also {
                    proguardFile(it)
                }
            }
        }

        sourceSets.apply {
            maybeCreate("main").apply {
                manifest.srcFile(androidGenerated.getAndroidManifestFile(isKorge = isKorge))
                java.srcDirs(androidGenerated.getAndroidSrcFolder(isKorge = isKorge))
                res.srcDirs(androidGenerated.getAndroidResFolder(isKorge = isKorge))
                assets.srcDirs(
                    "${project.buildDir}/processedResources/jvm/main",
                )
            }
            for (name in listOf("test", "testDebug", "testRelease", "androidTest", "androidTestDebug", "androidTestRelease")) {
                maybeCreate(name).apply {
                    assets.srcDirs("src/commonTest/resources")
                }
            }
        }
    }
}

private fun Project.configureAndroidLibrary(isKorge: Boolean) {
    plugins.apply("com.android.kotlin.multiplatform.library")

    project.kotlin.targets.getByName("android").apply {
        compilations.allThis {
            compileTaskProvider.configure {
                (it as KotlinJvmCompile).compilerOptions {
                    jvmTarget.set(JvmTarget.fromTarget(ANDROID_JAVA_VERSION_STR))
                }
            }
        }
        AddFreeCompilerArgs.addFreeCompilerArgs(project, this)
        // TODO See if this is still required, should already be applied though
//        (this as? KotlinMultiplatformAndroidLibraryTarget)?.apply {
//            withHostTest {}
//            withDeviceTest {}
//        }
    }

    ensureSourceSetsConfigure("common", "android")

    // TODO Let the module itself configure the dependencies and remove this block
    dependencies {
        if (SemVer(BuildVersions.KOTLIN) >= SemVer("2.3.20")) {
            add("androidTestImplementation", "org.jetbrains.kotlin:kotlin-test")
        }
        add("androidTestImplementation", "org.jetbrains.kotlin:kotlin-test")

        add("androidTestImplementation", "org.jetbrains.kotlin:kotlin-test")
        add("androidTestImplementation", "androidx.test:core:1.7.0")
        add("androidTestImplementation", "androidx.test.ext:junit:1.3.0")
        add("androidTestImplementation", "androidx.test.espresso:espresso-core:3.7.0")
    }
    val android = extensions.getByType(KotlinMultiplatformExtension::class.java).targets.findByName("android") as KotlinMultiplatformAndroidLibraryTarget

    android.apply {
        val androidGenerated = project.toAndroidGenerated(isKorge)
        namespace = androidGenerated.getNamespace(isKorge)

        compileSdk = if (isKorge) project.korge.androidCompileSdk else project.getAndroidCompileSdkVersion()
    }
}
