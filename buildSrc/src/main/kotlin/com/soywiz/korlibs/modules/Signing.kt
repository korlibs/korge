package com.soywiz.korlibs.modules

import org.gradle.api.*
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.*

fun Project.configureSigning() {
	plugins.apply("signing")

	val signingSecretKeyRingFile = System.getenv("ORG_GRADLE_PROJECT_signingSecretKeyRingFile") ?: project.findProperty("signing.secretKeyRingFile")?.toString()

	// gpg --armor --export-secret-keys foobar@example.com | awk 'NR == 1 { print "signing.signingKey=" } 1' ORS='\\n'
	val signingKey = System.getenv("ORG_GRADLE_PROJECT_signingKey") ?: project.findProperty("signing.signingKey")?.toString()
	val signingPassword = System.getenv("ORG_GRADLE_PROJECT_signingPassword") ?: project.findProperty("signing.password")?.toString()

	if (signingSecretKeyRingFile == null && signingKey == null) {
		logger.warn("WARNING! Signing not configured due to missing properties/environment variables like signing.keyId or ORG_GRADLE_PROJECT_signingKey. This is required for deploying to Maven Central. Check README for details")
		return
	}

	afterEvaluate {
		signing.apply {
			sign(publishing.publications)
			if (signingKey != null) {
				useInMemoryPgpKeys(signingKey, signingPassword)
			}
			project.gradle.taskGraph.whenReady {

			}
		}
	}
}

val Project.signing get() = extensions.getByType<SigningExtension>()
