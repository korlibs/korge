package korlibs.modules

import com.vanniktech.maven.publish.*
import korlibs.korge.gradle.util.*
import korlibs.*
import org.gradle.api.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*

fun Project.getCustomProp(name: String, default: String): String? {
    val props = if (extra.has("props")) extra["props"] as? Map<String, String>? else null
    return props?.get(name) ?: (findProperty(name) as? String?) ?: default
}

fun Project.configurePublishing(multiplatform: Boolean = true) {
    val isGradlePlugin = project.name.contains("gradle-plugin")

    plugins.apply("com.vanniktech.maven.publish")

    val mavenPublishing = extensions.getByType(MavenPublishBaseExtension::class.java)

    mavenPublishing.apply {
        // For plain JVM modules (not KMP, not Gradle plugin) declare the component type explicitly
        if (!multiplatform && !isGradlePlugin) {
            configure(JavaLibrary(javadocJar = JavadocJar.Empty(), sourcesJar = true))
        } else if (multiplatform && !isGradlePlugin) {
            // Explicitly configure KMP with Empty javadoc to avoid wiring in slow/broken Dokka tasks
            configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty()))
        }
        // For Gradle plugins: vanniktech auto-detects via java-gradle-plugin

        if (customMavenUrl != null) {
            // Custom Maven repo – add as an additional publishing repository below;
            // don't also configure Central Portal to avoid unintended uploads.
        } else {
            // Map existing SONATYPE_* credentials to the names vanniktech expects
            val user = System.getenv("SONATYPE_USERNAME")
                ?: rootProject.findProperty("SONATYPE_USERNAME")?.toString()
                ?: rootProject.findProperty("sonatypeUsername")?.toString()
            val pass = System.getenv("SONATYPE_PASSWORD")
                ?: rootProject.findProperty("SONATYPE_PASSWORD")?.toString()
                ?: rootProject.findProperty("sonatypePassword")?.toString()
            if (user != null) project.extensions.extraProperties["mavenCentralUsername"] = user
            if (pass != null) project.extensions.extraProperties["mavenCentralPassword"] = pass

            publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
        }

        val baseProjectName = project.name.substringBefore('-')
        val defaultGitUrl = "https://github.com/korlibs/$baseProjectName"

        pom { pom ->
            pom.name.set(project.name)
            pom.description.set(
                project.description
                    ?: project.getCustomProp("project.description", project.name)
                    ?: project.name
            )
            pom.url.set(project.getCustomProp("project.scm.url", defaultGitUrl))
            pom.licenses { licenseSpec ->
                licenseSpec.license { license ->
                    license.name.set(project.getCustomProp("project.license.name", "MIT") ?: "MIT")
                    license.url.set(
                        project.getCustomProp(
                            "project.license.url",
                            "https://raw.githubusercontent.com/korlibs/$baseProjectName/master/LICENSE"
                        ) ?: ""
                    )
                }
            }
            pom.developers { devSpec ->
                devSpec.developer { dev ->
                    dev.id.set(project.getCustomProp("project.author.id", "korge") ?: "korge")
                    dev.name.set(project.getCustomProp("project.author.name", "Korge Team") ?: "")
                    dev.email.set(project.getCustomProp("project.author.email", "info@korge.org") ?: "")
                }
            }
            pom.scm { scm ->
                scm.url.set(project.getCustomProp("project.scm.url", defaultGitUrl))
            }
        }
    }

    // Add custom Maven repository when KORLIBS_CUSTOM_MAVEN_* vars are set
    if (customMavenUrl != null) {
        afterEvaluate {
            extensions.getByType(PublishingExtension::class.java).repositories {
                it.maven { repo ->
                    repo.credentials {
                        it.username = project.customMavenUser
                        it.password = project.customMavenPass
                    }
                    repo.url = uri(project.customMavenUrl!!)
                }
            }
        }
    }
}

val Project.publishing get() = extensions.getByType<PublishingExtension>()
