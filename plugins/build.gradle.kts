import groovy.util.Node
import groovy.xml.QName
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.util.Properties

buildscript {
	val kotlinVersion = project.properties["kotlinVersion"]?.toString() ?: ""
	val isKotlinDev = kotlinVersion.contains("-release")
	repositories {
		maven { url = uri("https://plugins.gradle.org/m2/") }
		if (isKotlinDev) {
			maven { url = uri("https://dl.bintray.com/kotlin/kotlin-dev") }
		}
	}
	dependencies {
		classpath("com.gradle.publish:plugin-publish-plugin:0.10.1")
		classpath("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:$kotlinVersion")
	}
}

val kotlinVersion = project.properties["kotlinVersion"]?.toString() ?: ""
val isKotlinDev = kotlinVersion.contains("-release")


plugins {
	id("com.moowork.node") version "1.3.1"
}

allprojects {
	repositories {
		mavenLocal().apply {
			content {
				excludeGroup("Kotlin/Native")
			}
		}
		maven {
			url = uri("https://dl.bintray.com/soywiz/soywiz")
			content {
				includeGroup("com.soywiz")
				excludeGroup("Kotlin/Native")
			}
		}
		jcenter() {
			content {
				excludeGroup("Kotlin/Native")
			}
		}
		google().apply {
			content {
				excludeGroup("Kotlin/Native")
			}
		}
		if (isKotlinDev) {
			maven { url = uri("https://dl.bintray.com/kotlin/kotlin-dev") }
		}
	}
}

val gradleProperties = Properties().apply { this.load(File(rootDir, "../gradle.properties").readText().reader()) }

fun version(name: String) = when (name) {
	"korge" -> gradleProperties["version"]
	else -> gradleProperties["${name}Version"]
}

//new File("korge-build/src/main/kotlin/com/soywiz/korge/build/BuildVersions.kt").write("""
File(rootDir, "korge-gradle-plugin/src/main/kotlin/com/soywiz/korge/gradle/BuildVersions.kt").writeText("""
package com.soywiz.korge.gradle

object BuildVersions {
	const val KLOCK = "${version("klock")}"
	const val KDS = "${version("kds")}"
	const val KMEM = "${version("kmem")}"
	const val KORMA = "${version("korma")}"
	const val KORIO = "${version("korio")}"
	const val KORIM = "${version("korim")}"
	const val KORAU = "${version("korau")}"
	const val KORGW = "${version("korgw")}"
	const val KORGE = "${version("korge")}"
	const val KOTLIN = "${KotlinCompilerVersion.VERSION}"
}
""")

subprojects {
	repositories {
		mavenLocal()
		jcenter()
		maven { url = uri("https://plugins.gradle.org/m2/") }
		maven { url = uri("https://dl.bintray.com/korlibs/korlibs") }
	}

	apply(plugin = "maven")
	apply(plugin = "maven-publish")

	// Publishing
	val publishUser = (rootProject.findProperty("BINTRAY_USER") ?: project.findProperty("bintrayUser") ?: System.getenv("BINTRAY_USER"))?.toString()
	val publishPassword = (rootProject.findProperty("BINTRAY_KEY") ?: project.findProperty("bintrayApiKey") ?: System.getenv("BINTRAY_API_KEY"))?.toString()

	//println("project: ${project.name}")

	if (publishUser != null && publishPassword != null) {
		extensions.getByType<PublishingExtension>().apply {
			repositories {
				maven {
					credentials {
						username = publishUser
						setPassword(publishPassword)
					}
					url = uri("https://api.bintray.com/maven/soywiz/soywiz/${project.property("project.package")}/")
				}
			}
			afterEvaluate {
				configure(publications) {
					this as MavenPublication
					//println("MavenPublication: ${project.name}: $this")
					pom.withXml {
						this.asNode().apply {
							appendNode("name", project.name)
							appendNode("description", project.property("project.description"))
							appendNode("url", project.property("project.scm.url"))
							appendNode("licenses").apply {
								appendNode("license").apply {
									appendNode("name").setValue(project.property("project.license.name"))
									appendNode("url").setValue(project.property("project.license.url"))
								}
							}
							appendNode("scm").apply {
								appendNode("url").setValue(project.property("project.scm.url"))
							}

							// Changes runtime -> compile in Android's AAR publications
							if (pom.packaging == "aar") {
								val nodes = this.getAt(QName("dependencies")).getAt("dependency").getAt("scope")
								for (node in nodes) {
									(node as Node).setValue("compile")
								}
							}
						}
					}
				}
			}
		}
	}
}
