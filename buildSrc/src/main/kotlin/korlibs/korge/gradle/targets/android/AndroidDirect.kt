package korlibs.korge.gradle.targets.android

import com.android.build.api.dsl.*
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.tasks.*
import korlibs.*
import korlibs.korge.gradle.*
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.all.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import java.io.*

fun Project.configureAndroidDirect(projectType: ProjectType, isKorge: Boolean) {
    if (!AndroidSdk.hasAndroidSdk(this)) {
        logger.info("Couldn't find ANDROID SDK, do not configuring android")
        return
    }

    project.ensureAndroidLocalPropertiesWithSdkDir()

    if (projectType.isExecutable) {
        project.plugins.apply("com.android.application")
    } else {
        plugins.apply("com.android.library")
    }

    //val android = project.extensions.getByName("android")
    //project.kotlin.jvmToolchain(11)

    project.kotlin.androidTarget().apply {
        //project.kotlin.android().apply {
        publishAllLibraryVariants()
        publishLibraryVariantsGroupedByFlavor = true
        //this.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
        compilations.allThis {
            kotlinOptions.jvmTarget = ANDROID_JAVA_VERSION_STR
        }
        AddFreeCompilerArgs.addFreeCompilerArgs(project, this)
    }

    //if (isKorge) {
    //    project.afterEvaluate {
    //        //println("@TODO: Info is not generated")
    //        //writeAndroidManifest(project.rootDir, project.korge)
    //    }
    //}
    //val generated = AndroidGenerated(korge)

    dependencies {
        if (SemVer(BuildVersions.KOTLIN) >= SemVer("1.9.0")) {
            add("androidUnitTestImplementation", "org.jetbrains.kotlin:kotlin-test")
        }
        add("androidTestImplementation", "org.jetbrains.kotlin:kotlin-test")

        add("androidTestImplementation", "org.jetbrains.kotlin:kotlin-test")
        add("androidTestImplementation", "androidx.test:core:1.4.0")
        add("androidTestImplementation", "androidx.test.ext:junit:1.1.2")
        add("androidTestImplementation", "androidx.test.espresso:espresso-core:3.3.0")
        //androidTestImplementation 'com.android.support.test:runner:1.0.2'
    }

    val android = extensions.getByName<TestedExtension>("android")
    android.apply {
        val androidGenerated = project.toAndroidGenerated(isKorge)
        namespace = androidGenerated.getNamespace(isKorge)

        setCompileSdkVersion(if (isKorge) project.korge.androidCompileSdk else project.getAndroidCompileSdkVersion())
        //buildToolsVersion(project.findProperty("android.buildtools.version")?.toString() ?: "30.0.2")

        (this as CommonExtension<*, *, *, *, *>).installation.apply {
            // @TODO: Android Build Gradle newer version
            installOptions("-r")
            timeOutInMs = project.korge.androidTimeoutMs
        }

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
            if (projectType.isExecutable) {
                it.applicationId = androidGenerated.getAppId(isKorge)
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
                storeFile = if (isKorge) project.file(
                    project.findProperty("RELEASE_STORE_FILE") ?: korge.androidReleaseSignStoreFile
                ) else File(korgeGradlePluginResources, "korge.keystore")
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD")?.toString()
                    ?: (if (isKorge) korge.androidReleaseSignStorePassword else "password")
                keyAlias = project.findProperty("RELEASE_KEY_ALIAS")?.toString()
                    ?: (if (isKorge) korge.androidReleaseSignKeyAlias else "korge")
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD")?.toString()
                    ?: (if (isKorge) korge.androidReleaseSignKeyPassword else "password")
            }
        }
        buildTypes.apply {
            //this.single().
            if (projectType.isExecutable) {
                maybeCreate("debug").apply {
                    signingConfig = signingConfigs.getByName("release")
                    isMinifyEnabled = false
                }
            }
            maybeCreate("release").apply {
                signingConfig = signingConfigs.getByName("release")
                if (projectType.isExecutable) {
                    isMinifyEnabled = true // for libraries, this would make the library to be empty
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                    File(rootDir, "proguard-rules.pro").takeIfExists()?.also {
                        proguardFile(it)
                    }
                    //proguardFiles(getDefaultProguardFile(ProguardFiles.ProguardFile.OPTIMIZE.fileName), File(rootProject.rootDir, "proguard-rules.pro"))
                } else {
                    isMinifyEnabled = false
                }
            }
        }

        sourceSets.apply {
            maybeCreate("main").apply {
                //assets.srcDirs("src/commonMain/resources",)
                //val (resourcesSrcDirs, kotlinSrcDirs) = androidGetResourcesFolders()
                //println("@ANDROID_DIRECT:")
                //println(resourcesSrcDirs.joinToString("\n"))
                //println(kotlinSrcDirs.joinToString("\n"))
                manifest.srcFile(androidGenerated.getAndroidManifestFile(isKorge = isKorge))
                java.srcDirs(androidGenerated.getAndroidSrcFolder(isKorge = isKorge))
                res.srcDirs(androidGenerated.getAndroidResFolder(isKorge = isKorge))
                assets.srcDirs(
                    "${project.buildDir}/processedResources/jvm/main",
                    //"${project.projectDir}/src/commonMain/resources",
                    //"${project.projectDir}/src/androidMain/resources",
                    //"${project.projectDir}/src/main/resources",
                    //"${project.projectDir}/build/commonMain/korgeProcessedResources/metadata/main",
                    //"${project.projectDir}/build/korgeProcessedResources/android/main",
                )
                //assets.srcDirs(*resourcesSrcDirs.map { it.absoluteFile }.toTypedArray())
                //java.srcDirs(*kotlinSrcDirs.map { it.absoluteFile }.toTypedArray())
                //manifest.srcFile(File(project.buildDir, "AndroidManifest.xml"))
                //manifest.srcFile(File(project.projectDir, "src/main/AndroidManifest.xml"))
            }
            for (name in listOf(
                "test",
                "testDebug",
                "testRelease",
                "androidTest",
                "androidTestDebug",
                "androidTestRelease"
            )) {
                maybeCreate(name).apply {
                    assets.srcDirs("src/commonTest/resources")
                }
            }
        }
    }

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
            //println("${project.path} :: $packageDebugAssets dependsOn $jvmProcessResources")
            packageDebugAssets?.dependsOn(jvmProcessResources) // @TODO: <-- THIS
            packageReleaseAssets?.dependsOn(jvmProcessResources) // @TODO: <-- THIS

            // @TODO: Why is this required with Gradle 8.1.1?
            //packageDebugAssets?.mustRunAfter(jvmProcessResources) // @TODO: <-- THIS
            //packageReleaseAssets?.mustRunAfter(jvmProcessResources) // @TODO: <-- THIS
        }
        val compileDebugJavaWithJavac =
            project.tasks.findByName("compileDebugJavaWithJavac") as? org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile?
        compileDebugJavaWithJavac?.compilerOptions?.jvmTarget?.set(ANDROID_JVM_TARGET)

        //tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile::class.java).configureEach {
        //    it.compilerOptions.jvmTarget.set(ANDROID_JVM_TARGET)
        //    //it.jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.WARNING)
        //}
        //val compileDebugJavaWithJavac = tasks.findByName("compileDebugJavaWithJavac")
        //println("compileDebugJavaWithJavac=$compileDebugJavaWithJavac : ${compileDebugJavaWithJavac!!::class}")
    }
}
