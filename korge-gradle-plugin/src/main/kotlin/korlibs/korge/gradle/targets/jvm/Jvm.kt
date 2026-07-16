package korlibs.korge.gradle.targets.jvm

import java.util.Locale.getDefault
import korlibs.invoke
import korlibs.jvm
import korlibs.korge.gradle.KORGE_RELOAD_AGENT_GROUP
import korlibs.korge.gradle.KorgeExtension
import korlibs.korge.gradle.getCompilationKorgeProcessedResourcesFolder
import korlibs.korge.gradle.gkotlin
import korlibs.korge.gradle.korge
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.GROUP_KORGE_PACKAGE
import korlibs.korge.gradle.targets.GROUP_KORGE_RUN
import korlibs.korge.gradle.targets.ProjectType
import korlibs.korge.gradle.targets.desktop.DesktopJreBundler
import korlibs.korge.gradle.util.KorgeReloadNotifier
import korlibs.korge.gradle.util.closure
import korlibs.korge.gradle.util.createThis
import korlibs.korge.gradle.util.get
import korlibs.korge.gradle.util.writeTextIfChanged
import korlibs.main
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.ZipEntryCompression
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import proguard.gradle.ProGuardTask

const val KORGE_RELOAD_AGENT_CONFIGURATION_NAME = "KorgeReloadAgent"
const val httpPort = 22011

fun Project.configureJvm(projectType: ProjectType) {
    if (gkotlin.targets.findByName("jvm") != null) return

    val jvmTarget = gkotlin.jvm()
    gkotlin.targets.add(jvmTarget)

    gkotlin.jvm {
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    project.tasks.withType(KotlinJvmCompile::class.java)
        .configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.fromTarget(korge.jvmTarget))
        }

    if (projectType.isExecutable) {
        configureJvmRunJvm(isRootKorlibs = false)
    }
    addProguard()
    configureJvmTest()

    val jvmProcessResources = tasks.findByName("jvmProcessResources") as? Copy?
    jvmProcessResources?.duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

fun Project.configureJvmRunJvm(isRootKorlibs: Boolean) {
    val project = this

    val timeBeforeCompilationFile = project.layout.buildDirectory.file("timeBeforeCompilation").get().asFile

    project.tasks.createThis<Task>("compileKotlinJvmAndNotifyBefore") {
        doFirst {
            KorgeReloadNotifier.beforeBuild(timeBeforeCompilationFile)
        }
    }
    afterEvaluate {
        tasks.findByName("compileKotlinJvm")?.mustRunAfter("compileKotlinJvmAndNotifyBefore")
    }
    project.tasks.createThis<Task>("compileKotlinJvmAndNotify") {
        dependsOn("compileKotlinJvmAndNotifyBefore", "compileKotlinJvm")
        doFirst {
            KorgeReloadNotifier.afterBuild(timeBeforeCompilationFile, httpPort)
        }
    }

    fun generateEntryPoint(entry: KorgeExtension.Entrypoint) {
        val capitalizedEntryName =
            entry.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
        project.tasks.createThis<KorgeJavaExec>("runJvm${capitalizedEntryName}") {
            group = GROUP_KORGE_RUN
            dependsOn("jvmMainClasses")
            project.afterEvaluate {
                mainClass.set(entry.jvmMainClassName())
                val beforeJava9 = JvmAddOpens.beforeJava9
                if (!beforeJava9) jvmArgs(project.korge.javaAddOpens)
            }
        }
        for (enableRedefinition in listOf(false)) {
            val taskName = when (enableRedefinition) {
                false -> "runJvm${capitalizedEntryName}Autoreload"
                true -> "runJvm${capitalizedEntryName}AutoreloadWithRedefinition"
            }
            project.tasks.createThis<KorgeJavaExecWithAutoreload>(taskName) {
                this.enableRedefinition = enableRedefinition
                group = GROUP_KORGE_RUN
                dependsOn("jvmMainClasses", "compileKotlinJvm")
                project.afterEvaluate {
                    if (isRootKorlibs) {
                        dependsOn(":korge-reload-agent:jar")
                    }
                    val beforeJava9 = JvmAddOpens.beforeJava9
                    if (!beforeJava9) jvmArgs(project.korge.javaAddOpens)
                    mainClass.set(korge.jvmMainClassName)
                }
            }
        }
    }

    if (!isRootKorlibs) {
        project.configurations
            .create(KORGE_RELOAD_AGENT_CONFIGURATION_NAME)
        project.dependencies {

            add(
                KORGE_RELOAD_AGENT_CONFIGURATION_NAME,
                "${KORGE_RELOAD_AGENT_GROUP}:korge-reload-agent:${project.version}"
            )
        }
    }

    // runJvm, runJvmAutoreload and variants for entrypoints
    // Immediately generate `runJvm`
    generateEntryPoint(korge.getDefaultEntryPoint())
    project.afterEvaluate {
        // And after the first evaluation when `korge {}` block must have been executed, generate the rest of the entrypoints
        // https://www.baeldung.com/java-instrumentation
        for (entry in korge.extraEntryPoints) {
            generateEntryPoint(entry)
        }
    }

    project.tasks.findByName("jvmJar")?.let {
        (it as Jar).apply {
            entryCompression = ZipEntryCompression.STORED
        }
    }
}

internal val Project.jvmCompilation: NamedDomainObjectSet<*> get() = kotlin.targets.getByName("jvm").compilations as NamedDomainObjectSet<*>
internal val Project.mainJvmCompilation: KotlinJvmCompilation
    get() = jvmCompilation.getByName("main") as? KotlinJvmCompilation?
        ?: error("Can't find main jvm compilation")

private fun Project.configureJvmTest() {
    val jvmTest = (tasks.findByName("jvmTest") as Test)
    jvmTest.classpath += project.files()
        .from(project.getCompilationKorgeProcessedResourcesFolder(mainJvmCompilation))
    jvmTest.jvmArgs = (jvmTest.jvmArgs ?: listOf()) + listOf("-Djava.awt.headless=true")

    val jvmTestFix = tasks.createThis<Test>("jvmTestFix") {
        group = "verification"
        environment("UPDATE_TEST_REF", "true")
        testClassesDirs = jvmTest.testClassesDirs
        classpath = jvmTest.classpath
        bootstrapClasspath = jvmTest.bootstrapClasspath
        systemProperty("java.awt.headless", "true")
    }
}

private fun Project.addProguard() {
    val packageJvmFatJar = project.tasks.createThis<Jar>("packageJvmFatJar") {
        dependsOn("jvmJar")
        archiveBaseName.set("${project.name}-all")
        group = GROUP_KORGE_PACKAGE
        exclude(
            "com/sun/jna/aix-ppc/**",
            "com/sun/jna/aix-ppc64/**",
            "com/sun/jna/freebsd-x86/**",
            "com/sun/jna/freebsd-x86-64/**",
            "com/sun/jna/linux-ppc/**",
            "com/sun/jna/linux-ppc64le/**",
            "com/sun/jna/linux-s390x/**",
            "com/sun/jna/linux-mips64el/**",
            "com/sun/jna/openbsd-x86/**",
            "com/sun/jna/openbsd-x86-64/**",
            "com/sun/jna/sunos-sparc/**",
            "com/sun/jna/sunos-sparcv9/**",
            "com/sun/jna/sunos-x86/**",
            "com/sun/jna/sunos-x86-64/**",
            "natives/macosx64/**",
            "natives/macosarm64/**",
            "META-INF/*.kotlin_module",
        )
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        project.afterEvaluate {
            manifest {
                manifest.attributes(
                    mapOf(
                        "Implementation-Title" to korge.realJvmMainClassName,
                        "Implementation-Version" to project.version.toString(),
                        "Main-Class" to korge.realJvmMainClassName,
                        "Add-Opens" to JvmAddOpens.jvmAddOpensList().joinToString(" "),
                    )
                )
            }
            from(closure {
                project.gkotlin.targets.jvm.compilations.main.runtimeDependencyFiles.map {
                    if (it.isDirectory) it else project.zipTree(
                        it
                    ) as Any
                }
            })
            val jvmJarTask = project.getTasksByName("jvmJar", false).first() as Jar
            with(jvmJarTask)
            from(
                project.files()
                    .from(project.getCompilationKorgeProcessedResourcesFolder(mainJvmCompilation))
            )
        }
    }

    val packageJvmFatJarProguard =
        project.tasks.createThis<ProGuardTask>("packageJvmFatJarProguard") {
            dependsOn(packageJvmFatJar)
            group = GROUP_KORGE_PACKAGE
            val serializationProFile = layout.buildDirectory.file("serialization.pro").get().asFile

            doFirst {
                serializationProFile.writeTextIfChanged(
                    $$"""
                # Keep `Companion` object fields of serializable classes.
                # This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
                -if @kotlinx.serialization.Serializable class **
                -keepclassmembers class <1> {
                    static <1>$Companion Companion;
                }

                # Keep `serializer()` on companion objects (both default and named) of serializable classes.
                -if @kotlinx.serialization.Serializable class ** {
                    static **$* *;
                }
                -keepclassmembers class <2>$<3> {
                    kotlinx.serialization.KSerializer serializer(...);
                }

                # Keep `INSTANCE.serializer()` of serializable objects.
                -if @kotlinx.serialization.Serializable class ** {
                    public static ** INSTANCE;
                }
                -keepclassmembers class <1> {
                    public static <1> INSTANCE;
                    kotlinx.serialization.KSerializer serializer(...);
                }

                # @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
                -keepattributes RuntimeVisibleAnnotations,AnnotationDefault

                # Don't print notes about potential mistakes or omissions in the configuration for kotlinx-serialization classes
                # See also https://github.com/Kotlin/kotlinx.serialization/issues/1900
                -dontnote kotlinx.serialization.**

                # Serialization core uses `java.lang.ClassValue` for caching inside these specified classes.
                # If there is no `java.lang.ClassValue` (for example, in Android), then R8/ProGuard will print a warning.
                # However, since in this case they will not be used, we can disable these warnings
                -dontwarn kotlinx.serialization.internal.ClassValueReferences
            """.trimIndent()
                )
            }
            project.afterEvaluate {
                val javaHome = System.getProperty("java.home")
                libraryjars("$javaHome/lib/rt.jar")
                // Support newer java versions that doesn't have rt.jar
                libraryjars(project.fileTree("$javaHome/jmods/") {
                    include("**/java.*.jmod")
                })
                injars(packageJvmFatJar.outputs.files.toList())
                outjars(layout.buildDirectory.file("/libs/${project.name}-all-proguard.jar").get().asFile)
                dontwarn()
                ignorewarnings()
                if (!project.korge.proguardObfuscate) {
                    dontobfuscate()
                }
                assumenosideeffects(
                    """
                class kotlin.jvm.internal.Intrinsics {
                    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
                }
            """.trimIndent()
                )

                configuration(serializationProFile)

                keepnames("class com.sun.jna.** { *; }")
                keepnames("class * extends com.sun.jna.** { *; }")
                keepnames("class * implements com.sun.jna.Library { *; }")
                keepnames("class * extends korlibs.ffi.FFILib { *; }")
                keepnames("class * extends korlibs.korge.scene.Scene { *; }")
                keepnames("@korlibs.io.annotations.Keep class * { *; }")
                keepnames("@korlibs.annotations.Keep class * { *; }")
                keepnames("@korlibs.annotations.KeepNames class * { *; }")
                keepnames("@kotlinx.serialization class * { *; }")
                keepclassmembernames("class * { @korlibs.io.annotations.Keep *; }")
                keepclassmembernames("@korlibs.io.annotations.Keep class * { *; }")
                keepclassmembernames("@korlibs.io.annotations.Keep interface * { *; }")
                keepclassmembernames("@korlibs.annotations.Keep class * { *; }")
                keepclassmembernames("@korlibs.annotations.Keep interface * { *; }")
                keepclassmembernames("@korlibs.annotations.KeepNames class * { *; }")
                keepclassmembernames("@korlibs.annotations.KeepNames interface * { *; }")
                keepclassmembernames("enum * { public *; }")

                keepattributes()
                keep("class * extends korlibs.ffi.FFILib { *; }")
                keep("class * implements com.sun.jna.** { *; }")
                keep("class com.sun.jna.** { *; }")
                keep("class org.jcodec.** { *; }")
                keep("@korlibs.io.annotations.Keep class * { *; }")
                keep("@korlibs.annotations.Keep class * { *; }")
                keep("@kotlinx.serialization class * { *; }")
                keep("class * implements korlibs.korge.ViewsCompleter { *; }")
                keep("public class kotlin.reflect.jvm.internal.impl.serialization.deserialization.builtins.* { public *; }")
                keep("class kotlin.reflect.jvm.internal.impl.load.** { *; }")


                if (korge.realJvmMainClassName.isNotBlank()) {
                    keep("class ${project.korge.realJvmMainClassName} { *; }")
                    keep("""public class ${korge.realJvmMainClassName} { public static void main(java.lang.String[]); }""")
                }
            }
        }

    val packageFatJar = packageJvmFatJar

    project.tasks.createThis<Task>("packageJvmLinuxApp") {
        dependsOn(packageFatJar)
        group = GROUP_KORGE_PACKAGE
        doLast {
            DesktopJreBundler.createLinuxBundle(project, packageFatJar.outputs.files.first())
        }
    }

    project.tasks.createThis<Task>("packageJvmWindowsApp") {
        dependsOn(packageFatJar)
        group = GROUP_KORGE_PACKAGE
        doLast {
            DesktopJreBundler.createWin32Bundle(project, packageFatJar.outputs.files.first())
        }
    }

    project.tasks.createThis<Task>("packageJvmMacosApp") {
        dependsOn(packageFatJar)
        group = GROUP_KORGE_PACKAGE
        doLast {
            DesktopJreBundler.createMacosApp(project, packageFatJar.outputs.files.first())
        }
    }
}
