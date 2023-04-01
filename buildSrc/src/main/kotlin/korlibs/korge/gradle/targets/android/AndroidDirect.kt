package korlibs.korge.gradle.targets.android

import com.android.build.api.dsl.*
import com.android.build.gradle.*
import com.android.build.gradle.TestExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.internal.dsl.*
import korlibs.*
import korlibs.korge.gradle.*
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.all.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import java.io.*
import kotlin.collections.*

fun Project.configureAndroidDirect(projectType: ProjectType, isKorge: Boolean) {
    project.ensureAndroidLocalPropertiesWithSdkDir()

    if (projectType.isExecutable) {
        project.plugins.apply("com.android.application")
    } else {
        plugins.apply("com.android.library")
    }

    //val android = project.extensions.getByName("android")

    configureBasicKotlinAndroid()

    //if (isKorge) {
    //    project.afterEvaluate {
    //        //println("@TODO: Info is not generated")
    //        //writeAndroidManifest(project.rootDir, project.korge)
    //    }
    //}

    //val generated = AndroidGenerated(korge)

    configureBasicKotlinAndroid2(isKorge = isKorge, isApp = projectType.isExecutable)

    android.apply {
        namespace = AndroidConfig.getAppId(project, isKorge)

        configureKotlinAndroidSignAndBuildTypes(isKorge = isKorge)
        sourceSets.apply {
            maybeCreate("main").apply {
                val (resourcesSrcDirs, kotlinSrcDirs) = androidGetResourcesFolders()
                //println("@ANDROID_DIRECT:")
                //println(resourcesSrcDirs.joinToString("\n"))
                //println(kotlinSrcDirs.joinToString("\n"))
                assets.srcDirs(*resourcesSrcDirs.map { it.absoluteFile }.toTypedArray())
                java.srcDirs(*kotlinSrcDirs.map { it.absoluteFile }.toTypedArray())
                //manifest.srcFile(File(project.buildDir, "AndroidManifest.xml"))
                //manifest.srcFile(File(project.projectDir, "src/main/AndroidManifest.xml"))
            }
            for (name in listOf("test", "testDebug", "testRelease", "androidTest", "androidTestDebug", "androidTestRelease")) {
                maybeCreate(name).apply {
                    assets.srcDirs("src/commonTest/resources",)
                }
            }
        }

        sourceSets.also {
            it.maybeCreate("main").apply {
                //it.maybeCreate("androidMain").apply {

                manifest.srcFile(AndroidConfig.getAndroidManifestFile(project, isKorge))
                java.srcDirs("${project.buildDir}/androidsrc")
                res.srcDirs("${project.buildDir}/androidres")
                assets.srcDirs(
                    "${project.projectDir}/src/commonMain/resources",
                    "${project.projectDir}/src/androidMain/resources",
                    "${project.projectDir}/src/main/resources",
                    "${project.projectDir}/build/commonMain/korgeProcessedResources/metadata/main",
                )
                //java.srcDirs += ["C:\\Users\\soywi\\projects\\korlibs\\korge-hello-world\\src\\commonMain\\kotlin", "C:\\Users\\soywi\\projects\\korlibs\\korge-hello-world\\src\\androidMain\\kotlin", "C:\\Users\\soywi\\projects\\korlibs\\korge-hello-world\\src\\main\\java"]
            }
        }
    }

    if (projectType.isExecutable) {
        installAndroidRun(listOf(), direct = true, isKorge = isKorge)
    }
}

fun Project.configureKotlinAndroidSignAndBuildTypes(isKorge: Boolean) {
    android.apply {
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
            maybeCreate("debug").apply {
                isMinifyEnabled = false
                signingConfig = signingConfigs.getByName("release")
            }
            maybeCreate("release").apply {
                isMinifyEnabled = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                //proguardFiles(getDefaultProguardFile(ProguardFiles.ProguardFile.OPTIMIZE.fileName), File(rootProject.rootDir, "proguard-rules.pro"))
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

val Project.android: TestedExtension get() = extensions.getByName<TestedExtension>("android")
//val Project.android: BaseAppModuleExtension get() = extensions.getByType(BaseAppModuleExtension::class.java)

fun Project.configureBasicKotlinAndroid2(isKorge: Boolean, isApp: Boolean) {

    dependencies {
        add("androidTestImplementation", "androidx.test:core:1.4.0")
        add("androidTestImplementation", "androidx.test.ext:junit:1.1.2")
        add("androidTestImplementation", "androidx.test.espresso:espresso-core:3.3.0")
        //androidTestImplementation 'com.android.support.test:runner:1.0.2'
    }

    android.apply {
        setCompileSdkVersion(if (isKorge) project.korge.androidCompileSdk else project.getAndroidCompileSdkVersion())
        //buildToolsVersion(project.findProperty("android.buildtools.version")?.toString() ?: "30.0.2")

        (this as CommonExtension<*, *, *, *>).installation.apply {
            // @TODO: Android Build Gradle newer version
            //installOptions = listOf("-r")
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

        defaultConfig.apply {
            multiDexEnabled = true
            if (isApp) {
                applicationId = AndroidConfig.getAppId(project, isKorge)
            }
            minSdk = if (isKorge) project.korge.androidMinSdk else project.getAndroidMinSdkVersion()
            targetSdk = if (isKorge) project.korge.androidTargetSdk else project.getAndroidTargetSdkVersion()
            versionCode = if (isKorge) project.korge.versionCode else 1
            version = if (isKorge) project.korge.version else "1.0"
            //testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
            manifestPlaceholders.clear()
            manifestPlaceholders.putAll(if (isKorge) korge.configs else emptyMap())
        }

        lintOptions.also {
            // @TODO: ../../build.gradle: All com.android.support libraries must use the exact same version specification (mixing versions can lead to runtime crashes). Found versions 28.0.0, 26.1.0. Examples include com.android.support:animated-vector-drawable:28.0.0 and com.android.support:customtabs:26.1.0
            it.disable("GradleCompatible")
        }
    }
}

fun Project.configureBasicKotlinAndroid() {
    project.kotlin.android().apply {
        publishAllLibraryVariants()
        publishLibraryVariantsGroupedByFlavor = true
        //this.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
        compilations.allThis {
            kotlinOptions.jvmTarget = ANDROID_JAVA_VERSION_STR
            compilerOptions.options.freeCompilerArgs.add("-Xno-param-assertions")
        }
    }
}
