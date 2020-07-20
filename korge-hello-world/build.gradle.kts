import java.net.URLClassLoader

dependencies {
	add("commonMainApi", project(":korge"))
}

open class KorgeJavaExec : JavaExec() {
    init {
        systemProperties = (System.getProperties().toMutableMap() as MutableMap<String, Any>) - "java.awt.headless"
        val useZgc = (System.getenv("JVM_USE_ZGC") == "true") || (javaVersion.majorVersion.toIntOrNull() ?: 8) >= 14

        doFirst {
            if (useZgc) {
                println("Using ZGC")
            }
        }

        if (useZgc) {
            jvmArgs("-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC")
        }
        project.afterEvaluate {
            val jvmCompilation = project.kotlin.targets["jvm"].compilations as NamedDomainObjectSet<*>
            val mainJvmCompilation = jvmCompilation["main"] as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation

            classpath =
                mainJvmCompilation.runtimeDependencyFiles + mainJvmCompilation.compileDependencyFiles + mainJvmCompilation.output.allOutputs + mainJvmCompilation.output.classesDirs

            //if (firstThread == true && OS.isMac) task.jvmArgs("-XstartOnFirstThread")

        }
    }
}

// @TODO: Move to KorGE plugin
tasks {
	val jvmMainClasses by getting
	val runJvm by creating(KorgeJavaExec::class) {
        main = "MainKt"
    }
	val runJs by creating { dependsOn("jsBrowserDevelopmentRun") }

	//val jsRun by creating { dependsOn("jsBrowserDevelopmentRun") } // Already available
	val jvmRun by creating { dependsOn(runJvm) }
	//val run by getting(JavaExec::class)

	val processResourcesKorge by creating {
		dependsOn(jvmMainClasses)
		val processedResourcesKorgeRoot = File(project.buildDir, "processedResourcesKorge")
		val processedResourcesKorgeMain = File(processedResourcesKorgeRoot, "main")
		val processedResourcesKorgeTest = File(processedResourcesKorgeRoot, "test")
		kotlin.jvm().compilations["main"].defaultSourceSet.resources.srcDir(processedResourcesKorgeMain)
		kotlin.jvm().compilations["test"].defaultSourceSet.resources.srcDir(processedResourcesKorgeTest)
		doLast {
			URLClassLoader(runJvm.classpath.toList().map { it.toURL() }.toTypedArray(), project::class.java.classLoader).use { classLoader ->
				val clazz = classLoader.loadClass("com.soywiz.korge.resources.ResourceProcessorRunner")
				val foldersMain = kotlin.jvm().compilations["main"].allKotlinSourceSets.flatMap { it.resources.srcDirs }.filter { it != processedResourcesKorgeMain }.map { it.toString() }
				val foldersTest = kotlin.jvm().compilations["test"].allKotlinSourceSets.flatMap { it.resources.srcDirs }.filter { it != processedResourcesKorgeTest }.map { it.toString() }
				try {
					clazz.methods.first { it.name == "run" }.invoke(null, classLoader, foldersMain, processedResourcesKorgeMain.toString(), foldersTest, processedResourcesKorgeTest.toString())
				} catch (e: java.lang.reflect.InvocationTargetException) {
					val re = (e.targetException ?: e)
					re.printStackTrace()
					System.err.println(re.toString())
				}
			}
			System.gc()
		}
	}

	//val processResources by getting {
	//	dependsOn(processResourcesKorge)
	//}
}

kotlin {
	jvm {
	}
	js {
		browser {
			binaries.executable()
		}
	}
}
