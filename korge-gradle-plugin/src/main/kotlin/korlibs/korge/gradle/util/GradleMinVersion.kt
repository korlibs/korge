package korlibs.korge.gradle.util

import korlibs.korge.gradle.ext
import org.gradle.api.Project

// TODO Either remove this check or move the expected gradle version to global variables to not lose track
fun Project.checkGradleVersion() {
    val currentGradleVersion = SemVer(project.gradle.gradleVersion)
    val expectedGradleVersion = SemVer("9.4.1")
    val korgeCheckGradleVersion = (project.ext.properties["korgeCheckGradleVersion"] as? Boolean) ?: true

    if (korgeCheckGradleVersion) require(currentGradleVersion >= expectedGradleVersion) {
        "Korge requires at least Gradle $expectedGradleVersion, but running on Gradle $currentGradleVersion. Please, update your current gradle version to 9.4.1 or later"
    }
}
