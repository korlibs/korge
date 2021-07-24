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

fun Project.configurePublishing() {
    // Publishing
    //val publishUser = project.BINTRAY_USER_null
    //val publishPassword = project.BINTRAY_KEY_null

    val publishUser = project.sonatypePublishUserNull
    val publishPassword = project.sonatypePublishPasswordNull

    plugins.apply("maven-publish")

    val javadocJar = tasks.create<Jar>("javadocJar") {
        classifier = "javadoc"
    }

    val sourcesJar = tasks.create<Jar>("sourceJar") {
        classifier = "sources"
        val mySourceSet = gkotlin.sourceSets["jvmMain"]
        //val mySourceSet = gkotlin.sourceSets["commonMain"]
        for (dep in mySourceSet.dependsOn + mySourceSet) {
            from(dep.kotlin.srcDirs) {
                into(dep.name)
            }
        }
        //from(zipTree((tasks.getByName("jvmSourcesJar") as Jar).outputs))
    }

    //val emptyJar = tasks.create<Jar>("emptyJar") {}

    publishing.apply {
        if (publishUser == null || publishPassword == null) {
            println("Publishing is not enabled. Was not able to determine either `publishUser` or `publishPassword`")
        } else {

            repositories {
                maven {
                    credentials {
                        username = publishUser
                        password = publishPassword
                    }
                    if (version.toString().contains("-SNAPSHOT")) {
                        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
                    } else {
                        url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                    }
                    //it.url = uri(
                    //	"https://api.bintray.com/maven/${project.property("project.bintray.org")}/${
                    //		project.property("project.bintray.repository")
                    //	}/${project.property("project.bintray.package")}/"
                    //)
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

                /*
                val sourcesJar = tasks.create<Jar>("sourcesJar${publication.name.capitalize()}") {
                    classifier = "sources"
                    baseName = publication.name
                    val pname = when (publication.name) {
                        "metadata" -> "common"
                        else -> publication.name
                    }
                    val names = listOf("${pname}Main", pname)
                    val sourceSet = names.mapNotNull { gkotlin.sourceSets.findByName(it) }.firstOrNull() as? KotlinSourceSet

                    sourceSet?.let { from(it.kotlin) }
                    //println("${publication.name} : ${sourceSet?.javaClass}")

                    /*
                    doFirst {
                        println(gkotlin.sourceSets)
                        println(gkotlin.sourceSets.names)
                        println(gkotlin.sourceSets.getByName("main"))
                        //from(sourceSets.main.allSource)
                    }
                    afterEvaluate {
                        println(gkotlin.sourceSets.names)
                    }
                     */
                }
                */

                //val mustIncludeDocs = publication.name != "kotlinMultiplatform"
                val mustIncludeDocs = true

                //if (publication.name == "")
                if (mustIncludeDocs) {
                    publication.artifact(javadocJar)
                }
                publication.pom.withXml {
                    val defaultGitUrl = "https://github.com/korlibs/korge-next"
                    this.asNode().apply {
                        appendNode("name", project.name)
                        appendNode("description", project.getCustomProp("project.description", project.description ?: project.name))
                        appendNode("url", project.getCustomProp("project.scm.url", defaultGitUrl))
                        appendNode("licenses").apply {
                            appendNode("license").apply {
                                appendNode("name").setValue(project.getCustomProp("project.license.name", "MIT"))
                                appendNode("url").setValue(project.getCustomProp("project.license.url", "https://raw.githubusercontent.com/korlibs/korge-next/master/LICENSE"))
                            }
                        }
                        appendNode("developers").apply {
                            appendNode("developer").apply {
                                appendNode("id").setValue(project.getCustomProp("project.author.id", "soywiz"))
                                appendNode("name").setValue(project.getCustomProp("project.author.name", "Carlos Ballesteros Velasco"))
                                appendNode("email").setValue(project.getCustomProp("project.author.email", "soywiz@gmail.com"))
                            }
                        }
                        appendNode("scm").apply {
                            appendNode("url").setValue(project.getCustomProp("project.scm.url", defaultGitUrl))
                        }

                        // Workaround for kotlin-native cinterops without gradle metadata
                        /*
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
                        */

                        // Changes runtime -> compile in Android's AAR publications
                        if (publication.pom.packaging == "aar") {
                            val nodes: NodeList = this.getAt(QName("dependencies")).getAt("dependency").getAt("scope")
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

val Project.publishing get() = extensions.getByType<PublishingExtension>()
