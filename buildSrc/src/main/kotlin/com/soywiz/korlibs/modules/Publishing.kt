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
                doOnce("publishingWarningLogged") {
                    logger.info("Publishing is not enabled. Was not able to determine either `publishUser` or `publishPassword`")
                }
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
                            project.stagedRepositoryId != null -> uri("https://oss.sonatype.org/service/local/staging/deployByRepositoryId/${project.stagedRepositoryId}/")
                            else -> uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                        }
                        doOnce("showDeployTo") { logger.info("DEPLOY mavenRepository: $url") }
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
                //if (!isGradlePluginMarker) {
                run {
                    val baseProjectName = project.name.substringBefore('-')
                    val defaultGitUrl = "https://github.com/korlibs/$baseProjectName"
                    publication.pom.also { pom ->
                        pom.name.set(project.name)
                        pom.description.set(project.description ?: project.getCustomProp("project.description", project.description ?: project.name))
                        pom.url.set(project.getCustomProp("project.scm.url", defaultGitUrl))
                        pom.licenses {
                            license {
                                this.name.set(project.getCustomProp("project.license.name", "MIT"))
                                this.url.set(project.getCustomProp("project.license.url", "https://raw.githubusercontent.com/korlibs/$baseProjectName/master/LICENSE"))
                            }
                        }
                        pom.developers {
                            developer {
                                this.id.set(project.getCustomProp("project.author.id", "soywiz"))
                                this.name.set(project.getCustomProp("project.author.name", "Carlos Ballesteros Velasco"))
                                this.email.set(project.getCustomProp("project.author.email", "soywiz@gmail.com"))
                            }
                        }
                        pom.scm {
                            this.url.set(project.getCustomProp("project.scm.url", defaultGitUrl))
                        }
                    }
                    if (publication.pom.packaging == "aar") {
                        publication.pom.withXml {
                            //println("baseProjectName=$baseProjectName")
                            this.asNode().apply {
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
