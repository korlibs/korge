package korlibs.korge.gradle.targets.jvm

import korlibs.korge.gradle.*
import korlibs.korge.gradle.gkotlin
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.util.*
import korlibs.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.tasks.testing.*
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import proguard.gradle.*
import java.io.File

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
            add(KORGE_RELOAD_AGENT_CONFIGURATION_NAME, "com.soywiz.korlibs.korge.reloadagent:korge-reload-agent:${BuildVersions.KORGE}")
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
internal val Project.mainJvmCompilation: KotlinJvmCompilation get() = jvmCompilation.getByName("main") as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation

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

open class PatchedProGuardTask : ProGuardTask() {
    @Internal override fun getadaptresourcefilenames(): Any = super.getadaptresourcefilenames()
    @Internal override fun getadaptresourcefilecontents(): Any = super.getadaptresourcefilecontents()
    @Internal override fun getallowaccessmodification(): Any = super.getallowaccessmodification()
    @Internal override fun getaddconfigurationdebugging(): Any = super.getaddconfigurationdebugging()
    @Internal override fun getadaptclassstrings(): Any = super.getadaptclassstrings()
    @Internal override fun getdontoptimize(): Any = super.getdontoptimize()
    @Internal override fun getdontobfuscate(): Any = super.getdontobfuscate()
    @Internal override fun getdontwarn(): Any = super.getdontwarn()
    @Internal override fun getdontnote(): Any = super.getdontnote()
    @Internal override fun getConfigurationFiles(): MutableList<Any?> = super.getConfigurationFiles()
    @Internal override fun getandroid(): Any = super.getandroid()
    @Internal override fun getdontusemixedcaseclassnames(): Any = super.getdontusemixedcaseclassnames()
    @Internal override fun getdontskipnonpubliclibraryclassmembers(): Any = super.getdontskipnonpubliclibraryclassmembers()
    @Internal override fun getdontshrink(): Any = super.getdontshrink()
    @Internal override fun getdontpreverify(): Any = super.getdontpreverify()
    @Internal override fun getInJarCounts(): MutableList<Any?> = super.getInJarCounts()
    @Internal override fun getignorewarnings(): Any = super.getignorewarnings()
    @Internal override fun getflattenpackagehierarchy(): Any = super.getflattenpackagehierarchy()
    @Internal override fun getdump(): Any = super.getdump()
    @Internal override fun getkeepdirectories(): Any = super.getkeepdirectories()
    @Internal override fun getkeepattributes(): Any = super.getkeepattributes()
    @Internal override fun getInJarFiles(): MutableList<Any?> = super.getInJarFiles()
    @Internal override fun getInJarFilters(): MutableList<Any?> = super.getInJarFilters()
    @Internal override fun getforceprocessing(): Any = super.getforceprocessing()
    @Internal override fun getmergeinterfacesaggressively(): Any = super.getmergeinterfacesaggressively()
    @Internal override fun getLibraryJarFilters(): MutableList<Any?> = super.getLibraryJarFilters()
    @Internal override fun getLibraryJarFiles(): MutableList<Any?> = super.getLibraryJarFiles()
    @Internal override fun getskipnonpubliclibraryclasses(): Any = super.getskipnonpubliclibraryclasses()
    @Internal override fun getprintseeds(): Any = super.getprintseeds()
    @Internal override fun getprintusage(): Any = super.getprintusage()
    @Internal override fun getprintmapping(): Any = super.getprintmapping()
    @Internal override fun getoverloadaggressively(): Any = super.getoverloadaggressively()
    @Internal override fun getuseuniqueclassmembernames(): Any = super.getuseuniqueclassmembernames()
    @Internal override fun getkeeppackagenames(): Any = super.getkeeppackagenames()
    @Internal override fun getrepackageclasses(): Any = super.getrepackageclasses()
    @Internal override fun getkeepparameternames(): Any = super.getkeepparameternames()
    @Internal override fun getrenamesourcefileattribute(): Any = super.getrenamesourcefileattribute()
    @Internal override fun getmicroedition(): Any = super.getmicroedition()
    @Internal override fun getverbose(): Any = super.getverbose()
    @Internal override fun getprintconfiguration(): Any = super.getprintconfiguration()
    @Internal override fun getOutJarFiles(): MutableList<Any?> = super.getOutJarFiles()
    @Internal override fun getOutJarFilters(): MutableList<Any?> = super.getOutJarFilters()
}

private fun Project.addProguard() {
	// packageJvmFatJar
	val packageJvmFatJar = project.tasks.createThis<org.gradle.jvm.tasks.Jar>("packageJvmFatJar") {
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
        doFirst {
            from(project.files().from(project.getCompilationKorgeProcessedResourcesFolder(mainJvmCompilation)))
        }
		project.afterEvaluate {
			manifest { manifest ->
				manifest.attributes(
					mapOf(
						"Implementation-Title" to korge.realJvmMainClassName,
						"Implementation-Version" to project.version.toString(),
						"Main-Class" to korge.realJvmMainClassName
					)
				)
			}
			//it.from()
			//fileTree()
			from(closure {
				project.gkotlin.targets.jvm.compilations.main.runtimeDependencyFiles.map { if (it.isDirectory) it else project.zipTree(it) as Any }
				//listOf<File>()
			})
			with(project.getTasksByName("jvmJar", true).first() as CopySpec)
		}
	}

	project.tasks.createThis<PatchedProGuardTask>("packageJvmFatJarProguard") {
        dependsOn(packageJvmFatJar)
		group = GROUP_KORGE_PACKAGE
		project.afterEvaluate {
			libraryjars("${System.getProperty("java.home")}/lib/rt.jar")
			// Support newer java versions that doesn't have rt.jar
			libraryjars(project.fileTree("${System.getProperty("java.home")}/jmods/") {
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

			keepnames("class com.sun.jna.** { *; }")
			keepnames("class * extends com.sun.jna.** { *; }")
			//task.keepnames("class org.jcodec.** { *; }")
			keepattributes()
			keep("class * implements com.sun.jna.** { *; }")
			keep("class com.sun.jna.** { *; }")
			keep("class ${project.korge.realJvmMainClassName} { *; }")
			keep("class org.jcodec.** { *; }")

			if (korge.realJvmMainClassName.isNotBlank()) {
				keep("""public class ${korge.realJvmMainClassName} { public static void main(java.lang.String[]); }""")
			}
		}

	}
}
