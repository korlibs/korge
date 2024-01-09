package korlibs.korge.gradle.targets.jvm

import korlibs.*
import korlibs.korge.gradle.*
import korlibs.korge.gradle.gkotlin
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.desktop.*
import korlibs.korge.gradle.util.*
import korlibs.root.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.tasks.testing.*
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import proguard.gradle.*
import java.io.*

val KORGE_RELOAD_AGENT_CONFIGURATION_NAME = "KorgeReloadAgent"
val httpPort = 22011

fun Project.configureJvm(projectType: ProjectType) {
    if (gkotlin.targets.findByName("jvm") != null) return

    val jvmTarget = gkotlin.jvm()
	gkotlin.targets.add(jvmTarget)

    project.korge.addDependency("jvmMainImplementation", "net.java.dev.jna:jna:$jnaVersion")
    project.korge.addDependency("jvmMainImplementation", "net.java.dev.jna:jna-platform:$jnaVersion")
	project.korge.addDependency("jvmMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    gkotlin.jvm {
        testRuns["test"].executionTask.configure {
            it.useJUnit()
            //it.useJUnitPlatform()
        }
    }
	project.tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).allThis {
        kotlinOptions {
            this.jvmTarget = korge.jvmTarget
        }
    }

    if (projectType.isExecutable) {
        configureJvmRunJvm(isRootKorlibs = false)
    }
	addProguard()
	configureJvmTest()

    val jvmProcessResources = tasks.findByName("jvmProcessResources") as? Copy?
    jvmProcessResources?.duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE
}

fun Project.configureJvmRunJvm(isRootKorlibs: Boolean) {
    val project = this

    val timeBeforeCompilationFile = File(project.buildDir, "timeBeforeCompilation")

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
        val capitalizedEntryName = entry.name.capitalize()
        project.tasks.createThis<KorgeJavaExec>("runJvm${capitalizedEntryName}") {
            group = GROUP_KORGE_RUN
            dependsOn("jvmMainClasses")
            project.afterEvaluate {
                mainClass.set(entry.jvmMainClassName())
                val beforeJava9 = JvmAddOpens.beforeJava9
                if (!beforeJava9) jvmArgs(project.korge.javaAddOpens)
            }
        }
        //for (enableRedefinition in listOf(false, true)) {
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
        //.setVisible(false)
        //.setTransitive(true)
        //.setDescription("korge-reload-agent to be downloaded and used for this project.")
        project.dependencies {

            add(KORGE_RELOAD_AGENT_CONFIGURATION_NAME, "${RootKorlibsPlugin.KORGE_RELOAD_AGENT_GROUP}:korge-reload-agent:${BuildVersions.KORGE}")
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
internal val Project.mainJvmCompilation: KotlinJvmCompilation get() = jvmCompilation.getByName("main") as? org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation? ?: error("Can't find main jvm compilation")

private fun Project.configureJvmTest() {
	val jvmTest = (tasks.findByName("jvmTest") as Test)
    jvmTest.classpath += project.files().from(project.getCompilationKorgeProcessedResourcesFolder(mainJvmCompilation))
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
	// packageJvmFatJar
	val packageJvmFatJar = project.tasks.createThis<org.gradle.jvm.tasks.Jar>("packageJvmFatJar") {
        dependsOn("jvmJar")
        //entryCompression = ZipEntryCompression.STORED
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
			manifest { manifest ->
				manifest.attributes(
					mapOf(
						"Implementation-Title" to korge.realJvmMainClassName,
						"Implementation-Version" to project.version.toString(),
						"Main-Class" to korge.realJvmMainClassName,
                        "Add-Opens" to JvmAddOpens.jvmAddOpensList().joinToString(" "),
					)
				)
			}
			//it.from()
			//fileTree()
			from(closure {
				project.gkotlin.targets.jvm.compilations.main.runtimeDependencyFiles.map { if (it.isDirectory) it else project.zipTree(it) as Any }
				//listOf<File>()
			})
            //val jvmJarTask = project.getTasksByName("jvmJar", true).first { it.project == project } as CopySpec
            val jvmJarTask = project.getTasksByName("jvmJar", false).first() as Jar
            //jvmJarTask.entryCompression = ZipEntryCompression.STORED
            with(jvmJarTask)
            from(project.files().from(project.getCompilationKorgeProcessedResourcesFolder(mainJvmCompilation)))
            //println("jvmJarTask=$jvmJarTask")
		}
	}

	val packageJvmFatJarProguard = project.tasks.createThis<ProGuardTask>("packageJvmFatJarProguard") {
        dependsOn(packageJvmFatJar)
		group = GROUP_KORGE_PACKAGE
        val serializationProFile = File(buildDir, "/serialization.pro")

        doFirst {
            serializationProFile.writeTextIfChanged("""
                # Keep `Companion` object fields of serializable classes.
                # This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
                -if @kotlinx.serialization.Serializable class **
                -keepclassmembers class <1> {
                    static <1>${'$'}Companion Companion;
                }

                # Keep `serializer()` on companion objects (both default and named) of serializable classes.
                -if @kotlinx.serialization.Serializable class ** {
                    static **${'$'}* *;
                }
                -keepclassmembers class <2>${'$'}<3> {
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
            """.trimIndent())
        }
		project.afterEvaluate {
			val javaHome = System.getProperty("java.home")
            libraryjars("$javaHome/lib/rt.jar")
			// Support newer java versions that doesn't have rt.jar
			libraryjars(project.fileTree("$javaHome/jmods/") {
                it.include("**/java.*.jmod")
			})
			//println(packageJvmFatJar.outputs.files.toList())
			injars(packageJvmFatJar.outputs.files.toList())
			outjars(File(buildDir, "/libs/${project.name}-all-proguard.jar"))
			dontwarn()
			ignorewarnings()
			if (!project.korge.proguardObfuscate) {
				dontobfuscate()
			}
			assumenosideeffects("""
                class kotlin.jvm.internal.Intrinsics {
                    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
                }
            """.trimIndent())

            this.configuration(serializationProFile)

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
            //keepnames("@korlibs.io.annotations.Keep interface *")
            //keepnames("class korlibs.render.platform.INativeGL")


			//task.keepnames("class org.jcodec.** { *; }")
			keepattributes()
            keep("class * extends korlibs.ffi.FFILib { *; }")
			keep("class * implements com.sun.jna.** { *; }")
			keep("class com.sun.jna.** { *; }")
			keep("class org.jcodec.** { *; }")
            //keep("class korlibs.ffi.** { *; }")
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
    //val packageFatJar = packageJvmFatJarProguard


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
