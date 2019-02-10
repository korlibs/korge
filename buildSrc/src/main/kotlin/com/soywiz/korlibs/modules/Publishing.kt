package com.soywiz.korlibs.modules

import com.soywiz.korlibs.korlibs
import com.soywiz.korlibs.toXmlString
import groovy.util.*
import groovy.xml.*
import org.gradle.api.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*

fun Project.configurePublishing() {
    // Publishing
    val publishUser = (rootProject.findProperty("BINTRAY_USER") ?: project.findProperty("bintrayUser") ?: System.getenv("BINTRAY_USER"))?.toString()
    val publishPassword = (rootProject.findProperty("BINTRAY_KEY") ?: project.findProperty("bintrayApiKey") ?: System.getenv("BINTRAY_API_KEY"))?.toString()

    plugins.apply("maven-publish")

    if (publishUser != null && publishPassword != null) {
        val publishing = extensions.getByType(PublishingExtension::class.java)
        publishing.apply {
            repositories {
                it.maven {
                    it.credentials {
                        it.username = publishUser
                        it.setPassword(publishPassword)
                    }
                    it.url = uri("https://api.bintray.com/maven/soywiz/soywiz/${project.property("project.package")}/")
                }
            }
            afterEvaluate {
                configure(publications) {
                    val publication = it as MavenPublication
                    publication.pom.withXml {
                        it.asNode().apply {
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

                            // Workaround for kotlin-native cinterops without gradle metadata
                            if (korlibs.cinterops.isNotEmpty()) {
                                val dependenciesList = (this.get("dependencies") as NodeList)
                                if (dependenciesList.isNotEmpty()) {
                                    (dependenciesList.first() as Node).apply {
                                        for (cinterop in korlibs.cinterops.filter { it.targets.contains(publication.name) }) {
                                            appendNode("dependency").apply {
                                                appendNode("groupId").setValue("${project.group}")
                                                appendNode("artifactId").setValue("${project.name}-${publication.name.toLowerCase()}")
                                                appendNode("version").setValue("${project.version}")
                                                appendNode("type").setValue("klib")
                                                appendNode("classifier").setValue("cinterop-${cinterop.name}")
                                                appendNode("scope").setValue("compile")
                                                appendNode("exclusions").apply {
                                                    appendNode("exclusion").apply {
                                                        appendNode("artifactId").setValue("*")
                                                        appendNode("groupId").setValue("*")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Changes runtime -> compile in Android's AAR publications
                            if (publication.pom.packaging == "aar") {
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
    } else {
        println("Publishing is not enabled. Was not able to determine either `publishUser` or `publishPassword`")
    }
}
