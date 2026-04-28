package korlibs.modules

// Publishing is now handled by com.vanniktech.maven.publish targeting Sonatype Central Portal.
// The old OSSRH staging REST API (oss.sonatype.org) is no longer needed.

import korlibs.korge.gradle.util.*
import org.gradle.api.*

val Project.customMavenUser: String? get() = System.getenv("KORLIBS_CUSTOM_MAVEN_USER") ?: rootProject.findProperty("KORLIBS_CUSTOM_MAVEN_USER")?.toString()
val Project.customMavenPass: String? get() = System.getenv("KORLIBS_CUSTOM_MAVEN_PASS") ?: rootProject.findProperty("KORLIBS_CUSTOM_MAVEN_PASS")?.toString()
val Project.customMavenUrl: String? get() = System.getenv("KORLIBS_CUSTOM_MAVEN_URL") ?: rootProject.findProperty("KORLIBS_CUSTOM_MAVEN_URL")?.toString()

// Legacy helpers kept for compatibility; unused when publishing via Central Portal
val Project.sonatypePublishUserNull: String? get() =
    System.getenv("SONATYPE_USERNAME")
        ?: rootProject.findProperty("SONATYPE_USERNAME")?.toString()
        ?: project.findProperty("sonatypeUsername")?.toString()

val Project.sonatypePublishPasswordNull: String? get() =
    System.getenv("SONATYPE_PASSWORD")
        ?: rootProject.findProperty("SONATYPE_PASSWORD")?.toString()
        ?: project.findProperty("sonatypePassword")?.toString()

/**
 * Previously this registered tasks to manually close/promote OSSRH staging repositories.
 * With the new Sonatype Central Portal + vanniktech plugin, release is fully automatic
 * (automaticRelease = true). This function is kept as a stub so existing CI scripts that
 * reference these task names don't break; the tasks simply become no-ops.
 */
fun Project.configureMavenCentralRelease() {
    if (rootProject.tasks.findByName("releaseMavenCentral") == null) {
        rootProject.tasks.register("releaseMavenCentral") {
            it.group = "publishing"
            it.description = "No-op: release is handled automatically by the vanniktech plugin (automaticRelease=true)."
            it.doLast { logger.lifecycle("releaseMavenCentral: release is handled automatically by vanniktech/Central Portal.") }
        }
    }
    if (rootProject.tasks.findByName("checkReleasingMavenCentral") == null) {
        rootProject.tasks.register("checkReleasingMavenCentral") {
            it.group = "publishing"
            it.description = "No-op: staging check is not required for Sonatype Central Portal."
            it.doLast { logger.lifecycle("checkReleasingMavenCentral: no staging check needed with Central Portal.") }
        }
    }
    if (rootProject.tasks.findByName("startReleasingMavenCentral") == null) {
        rootProject.tasks.register("startReleasingMavenCentral") {
            it.group = "publishing"
            it.description = "No-op: no manual staging start required for Sonatype Central Portal."
            it.doLast { logger.lifecycle("startReleasingMavenCentral: no manual staging start needed with Central Portal.") }
        }
    }
}
