package korlibs.korge.gradle.targets.android

import com.android.build.api.dsl.*
import com.android.build.gradle.TestedExtension
import korlibs.*
import korlibs.korge.gradle.*
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.all.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import java.io.*

fun Project.configureAndroidDirect(projectType: ProjectType, isKorge: Boolean) {
    project.ensureAndroidLocalPropertiesWithSdkDir()

    if (projectType.isExecutable) {
        project.plugins.apply("com.android.application")
    } else {
        plugins.apply("com.android.library")
    }

    //val android = project.extensions.getByName("android")

    project.kotlin.android().apply {
        publishAllLibraryVariants()
        publishLibraryVariantsGroupedByFlavor = true
        //this.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
        compilations.allThis {
            kotlinOptions.jvmTarget = ANDROID_JAVA_VERSION_STR
            compilerOptions.options.freeCompilerArgs.add("-Xno-param-assertions")
        }
    }

    //if (isKorge) {
    //    project.afterEvaluate {
    //        //println("@TODO: Info is not generated")
    //        //writeAndroidManifest(project.rootDir, project.korge)
    //    }
    //}
    //val generated = AndroidGenerated(korge)

    dependencies {
        add("androidTestImplementation", "androidx.test:core:1.4.0")
        add("androidTestImplementation", "androidx.test.ext:junit:1.1.2")
        add("androidTestImplementation", "androidx.test.espresso:espresso-core:3.3.0")
        //androidTestImplementation 'com.android.support.test:runner:1.0.2'
    }

    val android = extensions.getByName<TestedExtension>("android")
    android.apply {
        namespace = AndroidConfig.getNamespace(project, isKorge)

        setCompileSdkVersion(if (isKorge) project.korge.androidCompileSdk else project.getAndroidCompileSdkVersion())
        //buildToolsVersion(project.findProperty("android.buildtools.version")?.toString() ?: "30.0.2")

        (this as CommonExtension<*, *, *, *>).installation.apply {
            // @TODO: Android Build Gradle newer version
            installOptions = listOf("-r")
            timeOutInMs = project.korge.androidTimeoutMs
        }

        compileOptions.apply {
            sourceCompatibility = ANDROID_JAVA_VERSION
            targetCompatibility = ANDROID_JAVA_VERSION
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
            if (projectType.isExecutable) {
                it.applicationId = AndroidConfig.getAppId(project, isKorge)
            }
            it.minSdk = if (isKorge) project.korge.androidMinSdk else project.getAndroidMinSdkVersion()
            it.targetSdk = if (isKorge) project.korge.androidTargetSdk else project.getAndroidTargetSdkVersion()
            it.versionCode = if (isKorge) project.korge.versionCode else 1
            it.versionName = if (isKorge) project.korge.version else "1.0"
            it.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            it.manifestPlaceholders.clear()
            it.manifestPlaceholders.putAll(if (isKorge) korge.configs else emptyMap())
        }

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
            //this.single().
            if (projectType.isExecutable) {
                maybeCreate("debug").apply {
                    isMinifyEnabled = false
                    signingConfig = signingConfigs.getByName("release")
                }
            }
            maybeCreate("release").apply {
                isMinifyEnabled = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                File(rootDir, "proguard-rules.pro").takeIfExists()?.also {
                    proguardFile(it)
                }
                //proguardFiles(getDefaultProguardFile(ProguardFiles.ProguardFile.OPTIMIZE.fileName), File(rootProject.rootDir, "proguard-rules.pro"))
                signingConfig = signingConfigs.getByName("release")
            }
        }

        sourceSets.apply {
            maybeCreate("main").apply {
                //assets.srcDirs("src/commonMain/resources",)
                //val (resourcesSrcDirs, kotlinSrcDirs) = androidGetResourcesFolders()
                //println("@ANDROID_DIRECT:")
                //println(resourcesSrcDirs.joinToString("\n"))
                //println(kotlinSrcDirs.joinToString("\n"))
                manifest.srcFile(AndroidConfig.getAndroidManifestFile(project, isKorge = isKorge))
                java.srcDirs(AndroidConfig.getAndroidSrcFolder(project, isKorge = isKorge))
                res.srcDirs(AndroidConfig.getAndroidResFolder(project, isKorge = isKorge))
                assets.srcDirs(
                    "${project.projectDir}/src/commonMain/resources",
                    "${project.projectDir}/src/androidMain/resources",
                    "${project.projectDir}/src/main/resources",
                    "${project.projectDir}/build/commonMain/korgeProcessedResources/metadata/main",
                )
                //assets.srcDirs(*resourcesSrcDirs.map { it.absoluteFile }.toTypedArray())
                //java.srcDirs(*kotlinSrcDirs.map { it.absoluteFile }.toTypedArray())
                //manifest.srcFile(File(project.buildDir, "AndroidManifest.xml"))
                //manifest.srcFile(File(project.projectDir, "src/main/AndroidManifest.xml"))
            }
            for (name in listOf("test", "testDebug", "testRelease", "androidTest", "androidTestDebug", "androidTestRelease")) {
                maybeCreate(name).apply {
                    assets.srcDirs("src/commonTest/resources")
                }
            }
        }
    }

    if (projectType.isExecutable) {
        installAndroidRun(listOf(), direct = true, isKorge = isKorge)
    }
}
