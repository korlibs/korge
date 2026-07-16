package korlibs.korge.gradle.targets.android

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.gradle.tasks.MergeSourceSetFolders
import korlibs.korge.gradle.korge
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.ProjectType
import korlibs.korge.gradle.targets.all.AddFreeCompilerArgs
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

fun Project.configureAndroidDirect(projectType: ProjectType, isKorge: Boolean) {
    if (!AndroidSdk.hasAndroidSdk(this)) {
        logger.info("Couldn't find ANDROID SDK, do not configuring android")
        return
    }

    project.ensureAndroidLocalPropertiesWithSdkDir()

    if (!projectType.isExecutable) configureAndroidLibrary(isKorge)

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

private fun Project.configureAndroidLibrary(isKorge: Boolean) {
    plugins.apply("com.android.kotlin.multiplatform.library")

    project.kotlin.targets.getByName("android").apply {
        compilations.all {
            compileTaskProvider.configure {
                (this as KotlinJvmCompile).compilerOptions {
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

    val android = extensions.getByType(KotlinMultiplatformExtension::class.java).targets.findByName("android") as KotlinMultiplatformAndroidLibraryTarget

    android.apply {
        val androidGenerated = project.toAndroidGenerated(isKorge)
        namespace = androidGenerated.getNamespace(isKorge)

        compileSdk = if (isKorge) project.korge.androidCompileSdk else project.getAndroidCompileSdkVersion()
    }
}
