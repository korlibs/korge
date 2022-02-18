package com.soywiz.korlibs.modules

import com.soywiz.korlibs.*
import groovy.util.*
import groovy.xml.*
import org.gradle.api.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*
import org.gradle.internal.impldep.com.amazonaws.util.XpathUtils.*
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

fun Project.getCustomProp(name: String, default: String): String? {
    val props = if (extra.has("props")) extra["props"] as? Map<String, String>? else null
    return props?.get(name) ?: (findProperty(name) as? String?) ?: default
}

fun Project.configurePublishing(multiplatform: Boolean = true) {
    val publishUser = project.sonatypePublishUserNull
    val publishPassword = project.sonatypePublishPasswordNull

    plugins.apply("maven-publish")

    val javadocJar = tasks.create<Jar>("javadocJar") {
        classifier = "javadoc"
    }

    val sourcesJar = tasks.create<Jar>("sourceJar") {
        classifier = "sources"
        if (multiplatform) {

            val mySourceSet = gkotlin.sourceSets["jvmMain"]
            for (dep in mySourceSet.dependsOn + mySourceSet) {
                from(dep.kotlin.srcDirs) {
                    into(dep.name)
                }
            }
        } else {
            val kotlinJvm = project.extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class.java)
            from(kotlinJvm.sourceSets["main"].kotlin.srcDirs)
        }
    }

    //val emptyJar = tasks.create<Jar>("emptyJar") {}

    publishing.apply {
        when {
            customMavenUrl != null -> {
                repositories {
                    maven {
                        credentials {
                            username = project.customMavenUser
                            password = project.customMavenPass
                        }
                        url = uri(project.customMavenUrl!!)
                    }
                }
            }
            publishUser == null || publishPassword == null -> {
                println("Publishing is not enabled. Was not able to determine either `publishUser` or `publishPassword`")
            }
            else -> {
                repositories {
                    maven {
                        credentials {
                            username = publishUser
                            password = publishPassword
                        }
                        url = when {
                            version.toString().contains("-SNAPSHOT") -> uri("https://oss.sonatype.org/content/repositories/snapshots/")
                            else -> uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                        }
                    }
                }
            }
        }
        afterEvaluate {
            //println(gkotlin.sourceSets.names)
            publications.withType<MavenPublication> {
                val publication = this
                //println("Publication: $publication : ${publication.name} : ${publication.artifactId}")
                if (publication.name == "kotlinMultiplatform") {
                    //publication.artifact(sourcesJar) {}
                    //publication.artifact(emptyJar) {}
                }

                //val mustIncludeDocs = publication.name != "kotlinMultiplatform"
                val mustIncludeDocs = true

                //if (publication.name == "")
                if (mustIncludeDocs) {
                    publication.artifact(javadocJar)
                }

                if (!multiplatform) {
                    publication.artifact(sourcesJar)
                }

                val isGradlePluginMarker = publication.name.endsWith("PluginMarkerMaven")
                //println("PUBLICATION: ${publication.name}")

                //if (multiplatform) {
                if (!isGradlePluginMarker) {
                    publication.pom.withXml {
                        val baseProjectName = project.name.substringBefore('-')
                        //println("baseProjectName=$baseProjectName")
                        val defaultGitUrl = "https://github.com/korlibs/$baseProjectName"
                        this.asNode().apply {
                            //replaceNode(Node(this, "name", project.name))
                            appendNode("name", project.name)
                            appendNode(
                                "description",
                                project.description ?: project.getCustomProp("project.description", project.description ?: project.name)
                            )
                            appendNode("url", project.getCustomProp("project.scm.url", defaultGitUrl))
                            appendNode("licenses").apply {
                                appendNode("license").apply {
                                    appendNode("name").setValue(project.getCustomProp("project.license.name", "MIT"))
                                    appendNode("url").setValue(
                                        project.getCustomProp(
                                            "project.license.url",
                                            "https://raw.githubusercontent.com/korlibs/$baseProjectName/master/LICENSE"
                                        )
                                    )
                                }
                            }
                            appendNode("developers").apply {
                                appendNode("developer").apply {
                                    appendNode("id").setValue(project.getCustomProp("project.author.id", "soywiz"))
                                    appendNode("name").setValue(
                                        project.getCustomProp("project.author.name", "Carlos Ballesteros Velasco")
                                    )
                                    appendNode("email").setValue(
                                        project.getCustomProp("project.author.email", "soywiz@gmail.com")
                                    )
                                }
                            }
                            appendNode("scm").apply {
                                appendNode("url").setValue(project.getCustomProp("project.scm.url", defaultGitUrl))
                            }

                            // Changes runtime -> compile in Android's AAR publications
                            if (publication.pom.packaging == "aar") {
                                val nodes: NodeList =
                                    this.getAt(QName("dependencies")).getAt("dependency").getAt("scope")
                                for (node in nodes as List<Node>) {
                                    node.setValue("compile")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

val Project.publishing get() = extensions.getByType<PublishingExtension>()
