import java.net.URLClassLoader

plugins {
	java
	//kotlin("multiplatform") version "1.4-M2"
	//kotlin("multiplatform") version "1.4-M3"
    kotlin("multiplatform") version "1.4.0-rc"
}

allprojects {
	repositories {
		mavenCentral()
		maven("https://dl.bintray.com/kotlin/kotlin-eap")
		maven("https://kotlin.bintray.com/kotlinx")
	}
}

val kotlinVersion: String by project
val isKotlinDev = kotlinVersion.contains("-release")
val isKotlinEap = kotlinVersion.contains("-eap") || kotlinVersion.contains("-M") || kotlinVersion.contains("-rc")

allprojects {

	repositories {
		mavenCentral()
		jcenter()
		maven { url = uri("https://plugins.gradle.org/m2/") }
		if (isKotlinDev || isKotlinEap) {
			maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
			maven { url = uri("https://dl.bintray.com/kotlin/kotlin-dev") }
		}
	}
}

val enableKotlinNative: String by project
val doEnableKotlinNative get() = enableKotlinNative == "true"

// Required by RC
kotlin {
    jvm { }
}

subprojects {
	apply(plugin = "kotlin-multiplatform")
	apply(plugin = "maven-publish")

	group = "com.soywiz.korlibs.${project.name}"

	kotlin {
		jvm {
			compilations.all {
				kotlinOptions.jvmTarget = "1.8"
			}
		}
		js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR) {
		//js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.LEGACY) {
			browser {
				//binaries.executable()
				/*
				webpackTask {
					cssSupport.enabled = true
				}
				runTask {
					cssSupport.enabled = true
				}
				*/
				testTask {
					useKarma {
						useChromeHeadless()
						//webpackConfig.cssSupport.enabled = true
					}
				}
			}
		}
		if (doEnableKotlinNative) {
			linuxX64()
            mingwX64()
		}

		// common
		//    js
		//    concurrent // non-js
		//      jvmAndroid
		//         android
		//         jvm
		//      native
		//         kotlin-native
		//    nonNative: [js, jvmAndroid]
		sourceSets {
			val commonMain by getting {
				dependencies {
					implementation(kotlin("stdlib-common"))
				}
			}
			val commonTest by getting {
				dependencies {
					implementation(kotlin("test-common"))
					implementation(kotlin("test-annotations-common"))
				}
			}

			val concurrentMain by creating {
				dependsOn(commonMain)
			}
			val concurrentTest by creating {
				dependsOn(commonTest)
			}

			val nonNativeCommonMain by creating {
				dependsOn(commonMain)
			}
			val nonNativeCommonTest by creating {
				dependsOn(commonTest)
			}

			val nonJsMain by creating {
				dependsOn(commonMain)
			}
			val nonJsTest by creating {
				dependsOn(commonTest)
			}

			val nonJvmMain by creating {
				dependsOn(commonMain)
			}
			val nonJvmTest by creating {
				dependsOn(commonTest)
			}

			val jvmAndroidMain by creating {
				dependsOn(commonMain)
			}
			val jvmAndroidTest by creating {
				dependsOn(commonTest)
			}

			// Default source set for JVM-specific sources and dependencies:
			val jvmMain by getting {
				dependsOn(concurrentMain)
				dependsOn(nonNativeCommonMain)
				dependsOn(nonJsMain)
				dependsOn(jvmAndroidMain)
				dependencies {
					implementation(kotlin("stdlib-jdk8"))
				}
			}
			// JVM-specific tests and their dependencies:
			val jvmTest by getting {
				dependsOn(concurrentTest)
				dependsOn(nonNativeCommonTest)
				dependsOn(nonJsTest)
				dependsOn(jvmAndroidTest)
				dependencies {
					implementation(kotlin("test-junit"))
				}
			}

			val jsMain by getting {
				dependsOn(commonMain)
				dependsOn(nonNativeCommonMain)
				dependsOn(nonJvmMain)
				dependencies {
					implementation(kotlin("stdlib-js"))
				}
			}
			val jsTest by getting {
				dependsOn(commonTest)
				dependsOn(nonNativeCommonTest)
				dependsOn(nonJvmTest)
				dependencies {
					implementation(kotlin("test-js"))
				}
			}

			if (doEnableKotlinNative) {
				val nativeCommonMain by creating { dependsOn(concurrentMain) }
				val nativeCommonTest by creating { dependsOn(concurrentTest) }

				val nativePosixMain by creating { dependsOn(nativeCommonMain) }
				val nativePosixTest by creating { dependsOn(nativeCommonTest) }

				val nativePosixNonAppleMain by creating { dependsOn(nativePosixMain) }
				val nativePosixNonAppleTest by creating { dependsOn(nativePosixTest) }

				val linuxX64Main by getting {
					dependsOn(commonMain)
					dependsOn(nativeCommonMain)
					dependsOn(nativePosixMain)
					dependsOn(nativePosixNonAppleMain)
					dependsOn(nonJvmMain)
					dependsOn(nonJsMain)
					dependencies {
				
					}
				}
				val linuxX64Test by getting {
					dependsOn(commonTest)
					dependsOn(nativeCommonTest)
					dependsOn(nativePosixTest)
					dependsOn(nativePosixNonAppleTest)
					dependsOn(nonJvmTest)
					dependsOn(nonJsTest)
					dependencies {
					}
				}

                val mingwX64Main by getting {
                    dependsOn(commonMain)
                    dependsOn(nativeCommonMain)
                    dependsOn(nonJvmMain)
                    dependsOn(nonJsMain)
                    dependencies {

                    }
                }
                val mingwX64Test by getting {
                    dependsOn(commonTest)
                    dependsOn(nativeCommonTest)
                    dependsOn(nonJvmTest)
                    dependsOn(nonJsTest)
                    dependencies {
                    }
                }
			}
		}
	}
}

open class KorgeJavaExec : JavaExec() {
    val jvmCompilation by lazy { project.kotlin.targets["jvm"].compilations as NamedDomainObjectSet<*> }
    val mainJvmCompilation by lazy { jvmCompilation["main"] as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation }
    val korgeClassPath by lazy {
        mainJvmCompilation.runtimeDependencyFiles + mainJvmCompilation.compileDependencyFiles + mainJvmCompilation.output.allOutputs + mainJvmCompilation.output.classesDirs
    }

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
            //if (firstThread == true && OS.isMac) task.jvmArgs("-XstartOnFirstThread")
            classpath = korgeClassPath

        }
    }
}

subprojects {

    fun getKorgeProcessResourcesTaskName(target: org.jetbrains.kotlin.gradle.plugin.KotlinTarget, compilation: org.jetbrains.kotlin.gradle.plugin.KotlinCompilation<*>): String {
        return "korgeProcessedResources${target.name.capitalize()}${compilation.name.capitalize()}"
    }

    if (project.path.startsWith(":samples:")) {
        // @TODO: Move to KorGE plugin
        project.tasks {
            val jvmMainClasses by getting
            val runJvm by creating(KorgeJavaExec::class) {
                group = "run"
                main = "MainKt"
            }
            val runJs by creating {
                group = "run"
                dependsOn("jsBrowserDevelopmentRun")
            }

            //val jsRun by creating { dependsOn("jsBrowserDevelopmentRun") } // Already available
            val jvmRun by creating {
                group = "run"
                dependsOn(runJvm)
            }
            //val run by getting(JavaExec::class)

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
            if (doEnableKotlinNative) {
                linuxX64 {
                    binaries {
                        executable {
                            entryPoint("entrypoint.main")
                        }
                    }
                }
                mingwX64 {
                    binaries {
                        executable {
                            entryPoint("entrypoint.main")
                        }
                    }
                }

                for (target in listOf(linuxX64(), mingwX64())) {
                    for (binary in target.binaries) {
                        val compilation = binary.compilation
                        val copyResourcesTask = tasks.create("copyResources${target.name.capitalize()}${binary.name.capitalize()}", Copy::class) {
                            dependsOn(getKorgeProcessResourcesTaskName(target, compilation))
                            group = "resources"
                            val isDebug = binary.buildType == org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.DEBUG
                            val isTest = binary.outputKind == org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind.TEST
                            val compilation = if (isTest) target.compilations["test"] else target.compilations["main"]
                            //target.compilations.first().allKotlinSourceSets
                            val sourceSet = compilation.defaultSourceSet
                            from(sourceSet.resources)
                            from(sourceSet.dependsOn.map { it.resources })
                            into(binary.outputDirectory)
                        }

                        //compilation.compileKotlinTask.dependsOn(copyResourcesTask)
                        binary.linkTask.dependsOn(copyResourcesTask)
                    }
                }
            }
        }

        project.tasks {
            val runJvm by getting(KorgeJavaExec::class)
            val jvmMainClasses by getting(Task::class)

            //val prepareResourceProcessingClasses = create("prepareResourceProcessingClasses", Copy::class) {
            //    dependsOn(jvmMainClasses)
            //    afterEvaluate {
            //        from(runJvm.korgeClassPath.toList().map { if (it.extension == "jar") zipTree(it) else it })
            //    }
            //    into(File(project.buildDir, "korgeProcessedResources/classes"))
            //}

            for (target in kotlin.targets) {
                for (compilation in target.compilations) {
                    val processedResourcesFolder = File(project.buildDir, "korgeProcessedResources/${target.name}/${compilation.name}")
                    compilation.defaultSourceSet.resources.srcDir(processedResourcesFolder)
                    val korgeProcessedResources = create(getKorgeProcessResourcesTaskName(target, compilation)) {
                        //dependsOn(prepareResourceProcessingClasses)
                        dependsOn(jvmMainClasses)

                        doLast {
                            processedResourcesFolder.mkdirs()
                            //URLClassLoader(prepareResourceProcessingClasses.outputs.files.toList().map { it.toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->
                            URLClassLoader(runJvm.korgeClassPath.toList().map { it.toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->
                                val clazz = classLoader.loadClass("com.soywiz.korge.resources.ResourceProcessorRunner")
                                val folders = compilation.allKotlinSourceSets.flatMap { it.resources.srcDirs }.filter { it != processedResourcesFolder }.map { it.toString() }
                                //println(folders)
                                try {
                                    clazz.methods.first { it.name == "run" }.invoke(null, classLoader, folders, processedResourcesFolder.toString(), compilation.name)
                                } catch (e: java.lang.reflect.InvocationTargetException) {
                                    val re = (e.targetException ?: e)
                                    re.printStackTrace()
                                    System.err.println(re.toString())
                                }
                            }
                            System.gc()
                        }
                    }
                    //println(compilation.compileKotlinTask.name)
                    //println(compilation.compileKotlinTask.name)
                    //compilation.compileKotlinTask.finalizedBy(processResourcesKorge)
                    //println(compilation.compileKotlinTask)
                    //compilation.compileKotlinTask.dependsOn(processResourcesKorge)
                    if (compilation.compileKotlinTask.name != "compileKotlinJvm") {
                        compilation.compileKotlinTask.dependsOn(korgeProcessedResources)
                    } else {
                        compilation.compileKotlinTask.finalizedBy(korgeProcessedResources)
                        getByName("runJvm").dependsOn(korgeProcessedResources)

                    }
                    //println(compilation.output.allOutputs.toList())
                    //println("$target - $compilation")

                }
            }
        }

    }
}
