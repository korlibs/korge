package korlibs.modules

import korlibs.korge.gradle.util.*
import korlibs.*
import groovy.util.*
import groovy.xml.*
import org.gradle.api.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*
import org.gradle.jvm.tasks.*

fun Project.getCustomProp(name: String, default: String): String? {
    val props = if (extra.has("props")) extra["props"] as? Map<String, String>? else null
    return props?.get(name) ?: (findProperty(name) as? String?) ?: default
}

fun Project.configurePublishing(multiplatform: Boolean = true) {
    val publishUser = project.sonatypePublishUserNull
    val publishPassword = project.sonatypePublishPasswordNull

    plugins.apply("maven-publish")

    val isGradlePlugin = project.name.contains("gradle-plugin")
    val sourcesJar = if (!multiplatform && !isGradlePlugin) {
        tasks.createThis<Jar>("sourceJar") {
            archiveClassifier.set("sources")
            val kotlinJvm = project.extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class.java)
            from(kotlinJvm.sourceSets["main"].kotlin.srcDirs)
        }
    } else {
        null
    }

    //tasks.getByName("publishKorgePluginMarkerMavenPublicationToMavenLocal")

    //val emptyJar = tasks.createThis<Jar>("emptyJar") {}

    publishing.apply {
        when {
            customMavenUrl != null -> {
                repositories {
                    it.maven {
                        it.credentials {
                            it.username = project.customMavenUser
                            it.password = project.customMavenPass
                        }
                        it.url = uri(project.customMavenUrl!!)
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
                    it.maven {
                        it.credentials {
                            it.username = publishUser
                            it.password = publishPassword
                        }
                        it.url = when {
                            version.toString().contains("-SNAPSHOT") -> uri("https://oss.sonatype.org/content/repositories/snapshots/")
                            project.stagedRepositoryId != null -> uri("https://oss.sonatype.org/service/local/staging/deployByRepositoryId/${project.stagedRepositoryId}/")
                            else -> uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                        }
                        doOnce("showDeployTo") { logger.info("DEPLOY mavenRepository: ${it.url}") }
                    }
                }
            }
        }
        afterEvaluate {
            //println(gkotlin.sourceSets.names)
            publications.withType(MavenPublication::class.java, Action { publication ->
                val jarTaskName = "${publication.name}JavadocJar"
                //println(jarTaskName)
                val javadocJar = tasks.createThis<Jar>(jarTaskName) {
                    archiveClassifier.set("javadoc")
                    archiveBaseName.set(jarTaskName)
                }
                val isGradlePluginMarker = publication.name.endsWith("PluginMarkerMaven")

                if (!isGradlePluginMarker && !isGradlePlugin) {
                    publication.artifact(javadocJar)
                }
                if (sourcesJar != null) {
                    publication.artifact(sourcesJar)
                }

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
                            it.license {
                                it.name.set(project.getCustomProp("project.license.name", "MIT"))
                                it.url.set(project.getCustomProp("project.license.url", "https://raw.githubusercontent.com/korlibs/$baseProjectName/master/LICENSE"))
                            }
                        }
                        pom.developers {
                            it.developer {
                                it.id.set(project.getCustomProp("project.author.id", "soywiz"))
                                it.name.set(project.getCustomProp("project.author.name", "Carlos Ballesteros Velasco"))
                                it.email.set(project.getCustomProp("project.author.email", "soywiz@gmail.com"))
                            }
                        }
                        pom.scm {
                            it.url.set(project.getCustomProp("project.scm.url", defaultGitUrl))
                        }
                    }
                    publication.pom.withXml {
                        if (publication.pom.packaging == "aar") {
                            //println("baseProjectName=$baseProjectName")
                            it.asNode().apply {
                                val nodes: NodeList = this.getAt(QName("dependencies")).getAt("dependency").getAt("scope")
                                for (node in nodes as List<Node>) {
                                    node.setValue("compile")
                                }
                            }
                        }
                    }
                }
            })
        }
    }
}

val Project.publishing get() = extensions.getByType<PublishingExtension>()
