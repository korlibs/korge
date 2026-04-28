package korlibs.modules

import korlibs.*
import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.plugins.signing.*
import org.gradle.plugins.signing.signatory.internal.pgp.*
import org.gradle.plugins.signing.signatory.pgp.*

private fun <T> ExtraPropertiesExtension.getOrSet(key: String, build: () -> T): T {
    if (!has(key)) {
        set(key, build())
    }
    return get(key) as T
}

fun Project.configureSigning() { //= doOncePerProject("configureSigningOnce") {
//fun Project.configureSigning() {
    //println("configureSigning: $this")
	val signingSecretKeyRingFile = System.getenv("ORG_GRADLE_PROJECT_signingSecretKeyRingFile") ?: project.findProperty("signing.secretKeyRingFile")?.toString()

	// gpg --armor --export-secret-keys foobar@example.com | awk 'NR == 1 { print "signing.signingKey=" } 1' ORS='\\n'
	val signingKey: String? = System.getenv("ORG_GRADLE_PROJECT_signingKey") ?: project.findProperty("signing.signingKey")?.toString()
	val signingPassword: String? = System.getenv("ORG_GRADLE_PROJECT_signingPassword") ?: project.findProperty("signing.password")?.toString()

	if (signingSecretKeyRingFile == null && signingKey == null) {
        doOnce("signingWarningLogged") {
            logger.info("WARNING! Signing not configured due to missing properties/environment variables like signing.keyId or ORG_GRADLE_PROJECT_signingKey. This is required for deploying to Maven Central. Check README for details")
        }
	} else {
        plugins.apply("signing")

        val signatories = rootProject.extra.getOrSet("signatories") { CachedInMemoryPgpSignatoryProvider(signingKey, signingPassword) }

        //println("signatories=$signatories")

        afterEvaluate {
            //println("configuring signing for $this")
            signing.apply {
                // This might be duplicated for korge-gradle-plugin? : Signing plugin detected. Will automatically sign the published artifacts.
                try {
                    sign(publishing.publications)
                } catch (e: GradleException) {
                }
                if (signingKey != null) {
                    this.signatories = signatories
                    //useInMemoryPgpKeys(signingKey, signingPassword)
                }
                //project.gradle.taskGraph.whenReady {}
            }
        }
    }
}

open class CachedInMemoryPgpSignatoryProvider(signingKey: String?, signingPassword: String?) : InMemoryPgpSignatoryProvider(signingKey, signingPassword) {
    var cachedPhpSignatory: PgpSignatory? = null
    override fun getDefaultSignatory(project: Project): PgpSignatory? {
        //project.rootProject
        //println("getDefaultSignatory:$project")
        if (cachedPhpSignatory == null) {
            cachedPhpSignatory = super.getDefaultSignatory(project)
        }
        return cachedPhpSignatory
    }
}

val Project.signing get() = extensions.getByType<SigningExtension>()

/**
 * Makes every Sign task in the project a no-op when signing keys are not present.
 * This prevents failures on `publishToMavenLocal` and similar local tasks when
 * com.gradle.plugin-publish (or any other plugin) registers sign tasks unconditionally.
 */
fun Project.makeSigningOptional() {
    val signingKey: String? = System.getenv("ORG_GRADLE_PROJECT_signingKey")
        ?: findProperty("signing.signingKey")?.toString()
    val signingKeyRingFile: String? = System.getenv("ORG_GRADLE_PROJECT_signingSecretKeyRingFile")
        ?: findProperty("signing.secretKeyRingFile")?.toString()
    val hasSigningKeys = signingKey != null || signingKeyRingFile != null

    afterEvaluate {
        tasks.withType(org.gradle.plugins.signing.Sign::class.java).configureEach { signTask ->
            signTask.onlyIf { hasSigningKeys }
        }
    }
}

