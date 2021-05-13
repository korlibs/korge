package com.soywiz.korge.gradle.targets.android

import com.soywiz.korge.gradle.util.*
import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.jvm.KorgeJavaExec
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.GradleBuild
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.File
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

fun Project.configureAndroidIndirect() {
	val resolvedKorgeArtifacts = LinkedHashMap<String, String>()
	val resolvedOtherArtifacts = LinkedHashMap<String, String>()
	val resolvedModules = LinkedHashMap<String, String>()

	val parentProjectName = parent?.name
	val allModules: Map<String, Project> = parent?.childProjects?.filter { (_, u) ->
		name != u.name
	}.orEmpty()
	val topLevelDependencies = mutableListOf<String>()

	configurations.all { conf ->
		if (conf.attributes.getAttribute(KotlinPlatformType.attribute)?.name == "jvm") {
			conf.resolutionStrategy.eachDependency { dep ->
				if (topLevelDependencies.isEmpty() && !conf.name.removePrefix("jvm").startsWith("Test")) {
					topLevelDependencies.addAll(conf.incoming.dependencies.map { "${it.group}:${it.name}" })
				}
				val cleanFullName = "${dep.requested.group}:${dep.requested.name}"
				//println("RESOLVE ARTIFACT: ${it.requested}")
				//if (cleanFullName.startsWith("org.jetbrains.intellij.deps:trove4j")) return@eachDependency
				//if (cleanFullName.startsWith("org.jetbrains:annotations")) return@eachDependency
                if (isKorlibsDependency(cleanFullName) && !cleanFullName.contains("-metadata")) {
                    when {
                        dep.requested.group == parentProjectName && allModules.contains(dep.requested.name) -> {
                            resolvedModules[dep.requested.name] = ":${parentProjectName}:${dep.requested.name}"
                        }
                        cleanFullName.startsWith("com.soywiz.korlibs.") -> {
                            resolvedKorgeArtifacts[cleanFullName.removeSuffix("-jvm")] = dep.requested.version.toString()
                        }
                        topLevelDependencies.contains(cleanFullName) -> {
                            resolvedOtherArtifacts[cleanFullName] = dep.requested.version.toString()
                        }
                    }
                }
			}
		}
	}

	//val androidPackageName = "com.example.myapplication"
	//val androidAppName = "My Awesome APP Name"

    val runJvm by lazy { (tasks["runJvm"] as KorgeJavaExec) }

    val prepareAndroidBootstrap = tasks.create("prepareAndroidBootstrap") { task ->
		task.dependsOn("compileTestKotlinJvm") // So artifacts are resolved
        task.dependsOn("jvmMainClasses")
		task.apply {
			val overwrite = korge.overwriteAndroidFiles
			val outputFolder = File(buildDir, "platforms/android")
			doLast {
				val androidPackageName = korge.id
				val androidAppName = korge.name

				val DOLLAR = "\\$"
				val ifNotExists = !overwrite
				//File(outputFolder, "build.gradle").conditionally(ifNotExists) {
				//	ensureParents().writeText("""
				//		// Top-level build file where you can add configuration options common to all sub-projects/modules.
				//		buildscript {
				//			repositories { google(); jcenter() }
				//			dependencies { classpath 'com.android.tools.build:gradle:3.3.0'; classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion" }
				//		}
				//		allprojects {
				//			repositories {
				//				mavenLocal(); maven { url = "https://dl.bintray.com/korlibs/korlibs" }; google(); jcenter()
				//			}
				//		}
				//		task clean(type: Delete) { delete rootProject.buildDir }
				//""".trimIndent())
				//}
                ensureAndroidLocalPropertiesWithSdkDir(outputFolder)
				//File(outputFolder, "settings.gradle").conditionally(ifNotExists) {
                File(outputFolder, "settings.gradle").always {
					ensureParents().writeTextIfChanged(Indenter {
                        line("rootProject.name = ${project.name.quoted}")
						if (parentProjectName != null && resolvedModules.isNotEmpty()) this@configureAndroidIndirect.parent?.projectDir?.let { projectFile ->
                            val projectPath = projectFile.absolutePath
							line("include(\":$parentProjectName\")")
							line("project(\":$parentProjectName\").projectDir = file(${projectPath.quoted})")
							resolvedModules.forEach { (name, path) ->
                                val subProjectPath = projectFile[name].absolutePath
								line("include(\"$path\")")
								line("project(\"$path\").projectDir = file(${subProjectPath.quoted})")
							}
						}
					})
				}
				File(
					outputFolder,
					"proguard-rules.pro"
				).conditionally(ifNotExists) { ensureParents().writeTextIfChanged("#Rules here\n") }

				outputFolder["gradle"].mkdirs()
				rootDir["gradle"].copyRecursively(outputFolder["gradle"], overwrite = true) { f, e -> OnErrorAction.SKIP }

                File(outputFolder, "build.extra.gradle").conditionally(ifNotExists) {
                    ensureParents().writeTextIfChanged(Indenter {
                        line("// When this file exists, it won't be overriden")
                    })
                }

                val info = AndroidInfo(executeInPlugin(runJvm.korgeClassPath, "com.soywiz.korge.plugin.KorgePluginExtensions", "getAndroidInfo", throws = true) { classLoader ->
                    listOf(classLoader, project.korge.configs)
                } as Map<String, Any?>?)

                File(outputFolder, "build.gradle").always {
					ensureParents().writeTextIfChanged(Indenter {
                        line("// File autogenerated do not modify!")
						line("buildscript") {
							//line("repositories { google(); jcenter(); }")
                            line("repositories { google() }")
                            if (isKotlinDevOrEap) {
                                line("repositories { maven { url = uri(\"https://dl.bintray.com/kotlin/kotlin-eap\") }; maven { url = uri(\"https://dl.bintray.com/kotlin/kotlin-dev\") } }")
                            }
							line("dependencies { classpath 'com.android.tools.build:gradle:$androidBuildGradleVersion'; classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion' }")
						}
						line("repositories") {
							line("mavenLocal()")
                            //line("jcenter()")
							line("google()")
                            if (isKotlinDevOrEap) {
                                line("maven { url = uri(\"https://dl.bintray.com/kotlin/kotlin-eap\") }")
                                line("maven { url = uri(\"https://dl.bintray.com/kotlin/kotlin-dev\") }")
                            }
                            if (checkBintrayArtifacts) {
                                line("maven { url = 'https://dl.bintray.com/korlibs/korlibs' }")
                            }
						}

						if (korge.androidLibrary) {
							line("apply plugin: 'com.android.library'")
						} else {
							line("apply plugin: 'com.android.application'")
						}
						line("apply plugin: 'kotlin-android'")
						line("apply plugin: 'kotlin-android-extensions'")

						line("android") {
                            line("compileOptions") {
                                line("sourceCompatibility JavaVersion.VERSION_1_8")
                                line("targetCompatibility JavaVersion.VERSION_1_8")
                            }
                            line("adbOptions") {
                                line("installOptions = [\"-r\"]")
                                line("timeOutInMs = 30 * 1000")
                            }
                            line("lintOptions") {
                                line("// @TODO: ../../build.gradle: All com.android.support libraries must use the exact same version specification (mixing versions can lead to runtime crashes). Found versions 28.0.0, 26.1.0. Examples include com.android.support:animated-vector-drawable:28.0.0 and com.android.support:customtabs:26.1.0")
                                line("disable(\"GradleCompatible\")")
                            }
                            line("kotlinOptions") {
                                line("jvmTarget = \"1.8\"")
                                line("freeCompilerArgs += \"-Xmulti-platform\"")
                            }
                            line("packagingOptions") {
                                for (pattern in androidExcludePatterns()) {
                                    line("exclude '$pattern'")
                                }
                            }
							line("compileSdkVersion ${korge.androidCompileSdk}")
							line("defaultConfig") {
								if (korge.androidMinSdk < 21)
									line("multiDexEnabled true")

								if (!korge.androidLibrary) {
									line("applicationId '$androidPackageName'")
								}

								line("minSdkVersion ${korge.androidMinSdk}")
								line("targetSdkVersion ${korge.androidTargetSdk}")
								line("versionCode 1")
								line("versionName '1.0'")
//								line("buildConfigField 'boolean', 'FULLSCREEN', '${korge.fullscreen}'")
								line("testInstrumentationRunner 'android.support.test.runner.AndroidJUnitRunner'")
                                val manifestPlaceholdersStr = korge.configs.map { it.key + ":" + it.value.quoted }.joinToString(", ")
								line("manifestPlaceholders = ${if (manifestPlaceholdersStr.isEmpty()) "[:]" else "[$manifestPlaceholdersStr]" }")
							}
							line("signingConfigs") {
								line("release") {
									line("storeFile file(findProperty('RELEASE_STORE_FILE') ?: ${korge.androidReleaseSignStoreFile.quoted})")
									line("storePassword findProperty('RELEASE_STORE_PASSWORD') ?: ${korge.androidReleaseSignStorePassword.quoted}")
									line("keyAlias findProperty('RELEASE_KEY_ALIAS') ?: ${korge.androidReleaseSignKeyAlias.quoted}")
									line("keyPassword findProperty('RELEASE_KEY_PASSWORD') ?: ${korge.androidReleaseSignKeyPassword.quoted}")
								}
							}
							line("buildTypes") {
								line("debug") {
									line("minifyEnabled false")
									line("signingConfig signingConfigs.release")
								}
								line("release") {
									//line("minifyEnabled false")
									line("minifyEnabled true")
									line("proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'")
									line("signingConfig signingConfigs.release")
								}
							}
							line("sourceSets") {
								line("main") {
									// @TODO: Use proper source sets of the app


                                    //println("mainSourceSets: $mainSourceSets")
                                    //println("resourcesSrcDirsBase: $resourcesSrcDirsBase")
                                    //println("resourcesSrcDirsBundle: $resourcesSrcDirsBundle")
                                    //println("kotlinSrcDirsBase: $kotlinSrcDirsBase")
                                    //println("kotlinSrcDirsBundle: $kotlinSrcDirsBundle")

                                    val (resourcesSrcDirs, kotlinSrcDirs) = androidGetResourcesFolders()
                                    line("assets.srcDirs += [${resourcesSrcDirs.joinToString(", ") { it.absolutePath.quoted }}]")
                                    line("java.srcDirs += [${kotlinSrcDirs.joinToString(", ") { it.absolutePath.quoted }}]")
								}
							}
						}

						line("dependencies") {
							line("implementation fileTree(dir: 'libs', include: ['*.jar'])")

							if (parentProjectName != null) {
								for ((_, path) in resolvedModules) {
									line("implementation project(\'$path\')")
								}
							}

							line("implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion'")
							if (korge.androidMinSdk < 21)
								line("implementation 'com.android.support:multidex:1.0.3'")

							//line("api 'org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion'")
							for ((name, version) in resolvedKorgeArtifacts) {
//								if (name.startsWith("org.jetbrains.kotlin")) continue
//								if (name.contains("-metadata")) continue
                                //if (name.startsWith("com.soywiz.korlibs.krypto:krypto")) continue
                                //if (name.startsWith("com.soywiz.korlibs.korge2:korge")) {
								val rversion = getModuleVersion(name, version)
								line("implementation '$name-android:$rversion'")
							}

							for ((name, version) in resolvedOtherArtifacts) {
                                if (name.startsWith("net.java.dev.jna")) continue
                                line("implementation '$name:$version'")
                            }

							for (dependency in korge.plugins.pluginExts.getAndroidDependencies() + info.androidDependencies) {
								line("implementation ${dependency.quoted}")
							}

                            for (bundle in korge.bundles.bundles) {
                                //println("FOR BUNDLE: $bundle")
                                for (dependency in bundle.dependenciesForSourceSet(setOf("androidMainApi", "commonMainApi"))) {
                                    //println("  DEPENDENCY: $dependency")
                                    line("implementation ${dependency.artifactPath.quoted}")
                                }
                            }

							line("implementation 'com.android.support:appcompat-v7:28.0.0'")
							line("implementation 'com.android.support.constraint:constraint-layout:1.1.3'")
							line("testImplementation 'junit:junit:4.12'")
							line("androidTestImplementation 'com.android.support.test:runner:1.0.2'")
							line("androidTestImplementation 'com.android.support.test.espresso:espresso-core:3.0.2'")
						}

						line("configurations") {
							line("androidTestImplementation.extendsFrom(commonMainApi)")
						}

                        line("apply from: 'build.extra.gradle'")
					}.toString())
				}

				writeAndroidManifest(outputFolder, korge, info)

				File(outputFolder, "gradle.properties").conditionally(ifNotExists) {
					ensureParents().writeTextIfChanged(
                        listOf(
                            "org.gradle.jvmargs=-Xmx1536m",
                            "android.useAndroidX=true",
                            "android.enableJetifier=true",
                        ).joinToString("\n")
                    )
				}
			}
		}
	}

	val bundleAndroid = tasks.create("bundleAndroid", GradleBuild::class.java) { task ->
		task.apply {
			group = GROUP_KORGE_INSTALL
			dependsOn(prepareAndroidBootstrap)
			buildFile = File(buildDir, "platforms/android/build.gradle")
			version = "4.10.1"
			tasks = listOf("bundleDebugAar")
		}
	}

	val buildAndroidAar = tasks.create("buildAndroidAar", GradleBuild::class.java) { task ->
		task.dependsOn(bundleAndroid)
	}

    installAndroidRun(listOf(prepareAndroidBootstrap.name), direct = false)
}

fun writeAndroidManifest(outputFolder: File, korge: KorgeExtension, info: AndroidInfo) {
	val androidPackageName = korge.id
	val androidAppName = korge.name
	val ifNotExists = korge.overwriteAndroidFiles
	File(outputFolder, "src/main/AndroidManifest.xml").also { it.parentFile.mkdirs() }.conditionally(ifNotExists) {
		ensureParents().writeTextIfChanged(Indenter {
			line("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
			line("<manifest")
            indent {
                //line("xmlns:tools=\"http://schemas.android.com/tools\"")
                line("xmlns:android=\"http://schemas.android.com/apk/res/android\"")
                line("package=\"$androidPackageName\"")
            }
            line(">")
			indent {
                line("<uses-feature android:name=\"android.hardware.touchscreen\" android:required=\"false\" />")
                line("<uses-feature android:name=\"android.software.leanback\" android:required=\"false\" />")

				line("<application")
				indent {
					line("")
                    //line("tools:replace=\"android:appComponentFactory\"")
					line("android:allowBackup=\"true\"")

					if (!korge.androidLibrary) {
						line("android:label=\"$androidAppName\"")
						line("android:icon=\"@mipmap/icon\"")
						// // line("android:icon=\"@android:drawable/sym_def_app_icon\"")
						line("android:roundIcon=\"@android:drawable/sym_def_app_icon\"")
						line("android:theme=\"@android:style/Theme.Holo.NoActionBar\"")
					}


					line("android:supportsRtl=\"true\"")
				}
				line(">")
				indent {
					for (text in korge.plugins.pluginExts.getAndroidManifestApplication() + info.androidManifest) {
						line(text)
					}
					for (text in korge.androidManifestApplicationChunks) {
						line(text)
					}

					line("<activity android:name=\".MainActivity\"")
					indent {
                        line("android:banner=\"@drawable/app_banner\"")
                        line("android:icon=\"@drawable/app_icon\"")
                        line("android:label=\"$androidAppName\"")
                        line("android:logo=\"@drawable/app_icon\"")
						when (korge.orientation) {
							Orientation.LANDSCAPE -> line("android:screenOrientation=\"landscape\"")
							Orientation.PORTRAIT -> line("android:screenOrientation=\"portrait\"")
                            Orientation.DEFAULT -> Unit
						}
					}
					line(">")

					if (!korge.androidLibrary) {
						indent {
							line("<intent-filter>")
							indent {
								line("<action android:name=\"android.intent.action.MAIN\"/>")
								line("<category android:name=\"android.intent.category.LAUNCHER\"/>")
							}
							line("</intent-filter>")
						}
					}
					line("</activity>")
				}
				line("</application>")
				for (text in korge.androidManifestChunks) {
					line(text)
				}
			}
			line("</manifest>")
		}.toString())
	}
	File(outputFolder, "korge.keystore").conditionally(ifNotExists) {
		ensureParents().writeBytesIfChanged(getResourceBytes("korge.keystore"))
	}
	File(outputFolder, "src/main/res/mipmap-mdpi/icon.png").conditionally(ifNotExists) {
		ensureParents().writeBytesIfChanged(korge.getIconBytes())
	}
    File(outputFolder, "src/main/res/drawable/app_icon.png").conditionally(ifNotExists) {
        ensureParents().writeBytesIfChanged(korge.getIconBytes())
    }
    File(outputFolder, "src/main/res/drawable/app_banner.png").conditionally(ifNotExists) {
        ensureParents().writeBytesIfChanged(korge.getBannerBytes(432, 243))
    }
	File(outputFolder, "src/main/java/MainActivity.kt").conditionally(ifNotExists) {
		ensureParents().writeTextIfChanged(Indenter {
			line("package $androidPackageName")

			line("import com.soywiz.korio.android.withAndroidContext")
			line("import com.soywiz.korgw.KorgwActivity")
			line("import ${korge.realEntryPoint}")

			line("class MainActivity : KorgwActivity()") {
				line("override suspend fun activityMain()") {
					//line("withAndroidContext(this)") { // @TODO: Probably we should move this to KorgwActivity itself
						for (text in korge.plugins.pluginExts.getAndroidInit() + info.androidInit) {
							line(text)
						}
						line("${korge.realEntryPoint}()")
					//}
				}
			}
		}.toString())
	}
}

class AndroidInfo(val map: Map<String, Any?>?) {
    //init { println("AndroidInfo: $map") }
    val androidInit: List<String> = (map?.get("androidInit") as? List<String?>?)?.filterNotNull() ?: listOf()
    val androidManifest: List<String> = (map?.get("androidManifest") as? List<String?>?)?.filterNotNull() ?: listOf()
    val androidDependencies: List<String> = (map?.get("androidDependencies") as? List<String>?)?.filterNotNull() ?: listOf()
}

private var _tryAndroidSdkDirs: List<File>? = null
val tryAndroidSdkDirs: List<File> get() {
    if (_tryAndroidSdkDirs == null) {
        _tryAndroidSdkDirs = listOf(
            File(System.getProperty("user.home"), "/Library/Android/sdk"), // MacOS
            File(System.getProperty("user.home"), "/Android/Sdk"), // Linux
            File(System.getProperty("user.home"), "/AppData/Local/Android/Sdk") // Windows
        )
    }
    return _tryAndroidSdkDirs!!
}

val prop_sdk_dir: String? get() = System.getProperty("sdk.dir")
val prop_ANDROID_HOME: String? get() = System.getenv("ANDROID_HOME")
private var _hasAndroidConfigured: Boolean? = null
var hasAndroidConfigured: Boolean
    set(value) {
        _hasAndroidConfigured = value
    }
    get() {
        if (_hasAndroidConfigured == null) {
            _hasAndroidConfigured = ((prop_sdk_dir != null) || (prop_ANDROID_HOME != null))
        }
        return _hasAndroidConfigured!!
    }

fun Project.tryToDetectAndroidSdkPath(): File? {
	for (tryAndroidSdkDirs in tryAndroidSdkDirs) {
		if (tryAndroidSdkDirs.exists()) {
			return tryAndroidSdkDirs.absoluteFile
		}
	}
	return null
}
