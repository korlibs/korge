plugins {
	java
	//kotlin("multiplatform") version "1.4-M2"
	kotlin("multiplatform") version "1.4-M3"
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
val isKotlinEap = kotlinVersion.contains("-eap") || kotlinVersion.contains("-M")

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

subprojects {
	apply(plugin = "kotlin-multiplatform")
	apply(plugin = "maven-publish")

	group = "com.soywiz.korlibs.${project.name}"

	kotlin {
		jvm {
			compilations.all {
				kotlinOptions.jvmTarget = "1.8"
			}
			withJava()
		}
		//js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR) {
		js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.LEGACY) {
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

			val nativeCommonMain by creating { dependsOn(concurrentMain) }
			val nativeCommonTest by creating { dependsOn(concurrentTest) }

			val nativePosixMain by creating { dependsOn(nativeCommonMain) }
			val nativePosixTest by creating { dependsOn(nativeCommonTest) }

			val nativePosixNonAppleMain by creating { dependsOn(nativePosixMain) }
			val nativePosixNonAppleTest by creating { dependsOn(nativePosixTest) }

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

			// Default source set for JVM-specific sources and dependencies:
			val jvmMain by getting {
				dependsOn(concurrentMain)
				dependsOn(nonNativeCommonMain)
				dependsOn(nonJsMain)
				dependencies {
					implementation(kotlin("stdlib-jdk8"))
				}
			}
			// JVM-specific tests and their dependencies:
			val jvmTest by getting {
				dependsOn(concurrentTest)
				dependsOn(nonNativeCommonTest)
				dependsOn(nonJsTest)
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
			}
		}
	}
}