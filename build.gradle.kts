plugins {
	java
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

subprojects {
	apply(plugin = "kotlin-multiplatform")

	kotlin {
		jvm {
			compilations.all {
				kotlinOptions.jvmTarget = "1.8"
			}
			withJava()
		}
		js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR) {
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
						webpackConfig.cssSupport.enabled = true
					}
				}
			}
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

			// Default source set for JVM-specific sources and dependencies:
			val jvmMain by getting {
				dependsOn(concurrentMain)
				dependsOn(nonNativeCommonMain)
				dependencies {
					implementation(kotlin("stdlib-jdk8"))
				}
			}
			// JVM-specific tests and their dependencies:
			val jvmTest by getting {
				dependsOn(concurrentTest)
				dependsOn(nonNativeCommonTest)
				dependencies {
					implementation(kotlin("test-junit"))
				}
			}

			val jsMain by getting {
				dependsOn(commonMain)
				dependsOn(nonNativeCommonMain)
				dependencies {
					implementation(kotlin("stdlib-js"))
				}
			}
			val jsTest by getting {
				dependsOn(commonTest)
				dependsOn(nonNativeCommonTest)
				dependencies {
					implementation(kotlin("test-js"))
				}
			}
		}
	}
}