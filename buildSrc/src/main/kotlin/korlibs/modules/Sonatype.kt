package korlibs.modules

// https://central.sonatype.org/publish/publish-guide/#deployment

import korlibs.korge.gradle.util.*
import org.gradle.api.*
import java.io.*
import java.util.*

val Project.customMavenUser: String? get() = System.getenv("KORLIBS_CUSTOM_MAVEN_USER") ?: rootProject.findProperty("KORLIBS_CUSTOM_MAVEN_USER")?.toString()
val Project.customMavenPass: String? get() = System.getenv("KORLIBS_CUSTOM_MAVEN_PASS") ?: rootProject.findProperty("KORLIBS_CUSTOM_MAVEN_PASS")?.toString()
val Project.customMavenUrl: String? get() = System.getenv("KORLIBS_CUSTOM_MAVEN_URL") ?: rootProject.findProperty("KORLIBS_CUSTOM_MAVEN_URL")?.toString()
val Project.stagedRepositoryId: String? get() =
    System.getenv("stagedRepositoryId")
        ?: rootProject.findProperty("stagedRepositoryId")?.toString()
        ?: File("stagedRepositoryId").takeIfExists()?.readText()


val Project.sonatypePublishUserNull: String? get() = (System.getenv("SONATYPE_USERNAME") ?: rootProject.findProperty("SONATYPE_USERNAME")?.toString() ?: project.findProperty("sonatypeUsername")?.toString())
val Project.sonatypePublishPasswordNull: String? get() = (System.getenv("SONATYPE_PASSWORD") ?: rootProject.findProperty("SONATYPE_PASSWORD")?.toString() ?: project.findProperty("sonatypePassword")?.toString())

val Project.sonatypePublishUser get() = sonatypePublishUserNull ?: error("Can't get SONATYPE_USERNAME/sonatypeUsername")
val Project.sonatypePublishPassword get() = sonatypePublishPasswordNull ?: error("Can't get SONATYPE_PASSWORD/sonatypePassword")

fun Project.configureMavenCentralRelease() {
	if (rootProject.tasks.findByName("releaseMavenCentral") == null) {
        rootProject.tasks.createThis<Task>("releaseMavenCentral").also { task ->
			task.doLast {
				if (!Sonatype.fromProject(rootProject).releaseGroupId(rootProject.group.toString())) {
					error("Can't promote artifacts. Check log for details")
				}
			}
		}
	}

    if (rootProject.tasks.findByName("checkReleasingMavenCentral") == null) {
        rootProject.tasks.createThis<Task>("checkReleasingMavenCentral").also { task ->
            task.doLast {
                println("stagedRepositoryId=${rootProject.stagedRepositoryId}")
                if (rootProject.stagedRepositoryId.isNullOrEmpty()) {
                    error("Couldn't find 'stagedRepositoryId' aborting...")
                }
            }
        }
    }
    if (rootProject.tasks.findByName("startReleasingMavenCentral") == null) {
        rootProject.tasks.createThis<Task>("startReleasingMavenCentral").also { task ->
            task.doLast {
                val sonatype = Sonatype.fromProject(rootProject)
                val profileId = sonatype.findProfileIdByGroupId("com.soywiz")
                val stagedRepositoryId = sonatype.startStagedRepository(profileId)
                println("profileId=$profileId")
                println("stagedRepositoryId=$stagedRepositoryId")
                GithubCI.setOutput("stagedRepositoryId", stagedRepositoryId)
                File("stagedRepositoryId").writeText(stagedRepositoryId)
            }
        }
    }
}

open class Sonatype(
	val user: String,
	val pass: String,
	val BASE: String = DEFAULT_BASE
) {
	companion object {
		val DEFAULT_BASE = "https://oss.sonatype.org/service/local/staging"
		private val BASE = DEFAULT_BASE

		fun fromGlobalConfig(): Sonatype {
			val props = Properties().also { it.load(File(System.getProperty("user.home") + "/.gradle/gradle.properties").readText().reader()) }
			return Sonatype(props["sonatypeUsername"].toString(), props["sonatypePassword"].toString(), DEFAULT_BASE)
		}

		fun fromProject(project: Project): Sonatype {
			return Sonatype(project.sonatypePublishUser, project.sonatypePublishPassword)
		}

		@JvmStatic
		fun main(args: Array<String>) {
			val sonatype = fromGlobalConfig()
			sonatype.releaseGroupId("korlibs")
		}
	}

	fun releaseGroupId(groupId: String = "korlibs"): Boolean {
		println("Trying to release groupId=$groupId")
		val profileId = findProfileIdByGroupId(groupId)
		println("Determined profileId=$profileId")
		val repositoryIds = findProfileRepositories(profileId).toMutableList()
		if (repositoryIds.isEmpty()) {
			println("Can't find any repositories for profileId=$profileId for groupId=$groupId. Artifacts weren't upload?")
			return false
		}
		val totalRepositories = repositoryIds.size
		var promoted = 0
		var stepCount = 0
		var retryCount = 0
		process@while (true) {
			stepCount++
			if (stepCount > 200) {
				error("Too much steps. stepCount=$stepCount")
			}
			repo@for (repositoryId in repositoryIds.toList()) {
				val state = try {
					getRepositoryState(repositoryId)
				} catch (e: SimpleHttpException) {
					when (e.responseCode) {
						404 -> {
							println("Can't find $repositoryId anymore. Probably released. Stopping")
							repositoryIds.remove(repositoryId)
							continue@repo
						}
						// Server error
						// @TODO: We should handle retrying on other operations too
						in 500..599 -> { // Sometimes  HTTP Error 502 Bad Gateway
							e.printStackTrace()
							println("Retrying...")
							Thread.sleep(15_000L)
							retryCount++
							continue@repo
						}
						else -> {
							throw e
						}
					}
				}
				when {
					state.transitioning -> {
						println("Waiting transition $state")
					}
					// Even if open, if there are notifications we should drop it
					state.notifications > 0 -> {
						println("Dropping release because of error state.notifications=$state")
                        println(" - activity: " + getRepositoryActivity(repositoryId))
						repositoryDrop(repositoryId)
						repositoryIds.remove(repositoryId)
					}
					state.isOpen -> {
						println("Closing open repository $state")
                        println(" - activity: " + getRepositoryActivity(repositoryId))
						repositoryClose(repositoryId)
					}
					else -> {
						println("Promoting repository $state")
                        println(" - activity: " + getRepositoryActivity(repositoryId))
						repositoryPromote(repositoryId)
						promoted++
					}
				}
			}
			if (repositoryIds.isEmpty()) {
				println("Completed promoted=$promoted, totalRepositories=$totalRepositories, retryCount=$retryCount")
				break@process
			}
			Thread.sleep(30_000L)
		}

		return promoted == totalRepositories
	}

	open val client = SimpleHttpClient(user, pass)

	fun getRepositoryState(repositoryId: String): RepoState {
		val info = client.request("${BASE}/repository/$repositoryId")
		//println("info: ${info.toStringPretty()}")
		return RepoState(
			repositoryId = repositoryId,
			type = info["type"].asString,
			notifications = info["notifications"].asInt,
			transitioning = info["transitioning"].asBoolean,
		)
	}

    fun getRepositoryActivity(repositoryId: String): String {
        val info = client.request("${BASE}/repository/$repositoryId/activity")
        //println("info: ${info.toStringPretty()}")
        return info.toStringPretty()
    }

	data class RepoState(
		val repositoryId: String,
		// "open" or "closed"
		val type: String,
		val notifications: Int,
		val transitioning: Boolean
	) {
		val isOpen get() = type == "open"
	}

	private fun getDataMapForRepository(repositoryId: String): Map<String, Map<*, *>> {
		return mapOf(
			"data" to mapOf(
				"stagedRepositoryIds" to listOf(repositoryId),
				"description" to "",
				"autoDropAfterRelease" to true,
			)
		)
	}

	fun repositoryClose(repositoryId: String) {
		client.request("${BASE}/bulk/close", getDataMapForRepository(repositoryId))
	}

	fun repositoryPromote(repositoryId: String) {
		client.request("${BASE}/bulk/promote", getDataMapForRepository(repositoryId))
	}

	fun repositoryDrop(repositoryId: String) {
		client.request("${BASE}/bulk/drop", getDataMapForRepository(repositoryId))
	}

	fun findProfileRepositories(profileId: String): List<String> {
		return client.request("${BASE}/profile_repositories")["data"].list
			.filter { it["profileId"].asString == profileId }
			.map { it["repositoryId"].asString }
	}

	fun findProfileIdByGroupId(groupId: String): String {
		val profiles = client.request("$BASE/profiles")["data"].list
		return profiles
			.filter { groupId.startsWith(it["name"].asString) }
			.map { it["id"].asString }
			.firstOrNull() ?: error("Can't find profile with group id '$groupId'")
	}

	fun startStagedRepository(profileId: String): String {
		return client.request("${BASE}/profiles/$profileId/start", mapOf(
			"data" to mapOf("description" to "Explicitly created by easy-kotlin-mpp-gradle-plugin")
		))["data"]["stagedRepositoryId"].asString
	}

	// Example:
	// https://oss.sonatype.org/service/local/repositories/comsoywiz-1229/content/korlibs/easy-kotlin-mpp-gradle-plugin/
	/*
	<content>
	  <data>
		<content-item>
		  <resourceURI>https://oss.sonatype.org/service/local/repositories/comsoywiz-1229/content/korlibs/easy-kotlin-mpp-gradle-plugin/maven-metadata.xml.sha512</resourceURI>
		  <relativePath>/korlibs/easy-kotlin-mpp-gradle-plugin/maven-metadata.xml.sha512</relativePath>
		  <text>maven-metadata.xml.sha512</text>
		  <leaf>true</leaf>
		  <lastModified>2021-03-11 00:17:49.0 UTC</lastModified>
		  <sizeOnDisk>128</sizeOnDisk>
		</content-item>
		<content-item>
		  <resourceURI>https://oss.sonatype.org/service/local/repositories/comsoywiz-1229/content/korlibs/easy-kotlin-mpp-gradle-plugin/0.13.999/</resourceURI>
		  <relativePath>/korlibs/easy-kotlin-mpp-gradle-plugin/0.13.999/</relativePath>
		  <text>0.13.999</text>
		  <leaf>false</leaf>
		  <lastModified>2021-03-11 00:17:48.0 UTC</lastModified>
		  <sizeOnDisk>-1</sizeOnDisk>
		</content-item>
		<content-item>
		  <resourceURI>https://oss.sonatype.org/service/local/repositories/comsoywiz-1229/content/korlibs/easy-kotlin-mpp-gradle-plugin/maven-metadata.xml</resourceURI>
		  <relativePath>/korlibs/easy-kotlin-mpp-gradle-plugin/maven-metadata.xml</relativePath>
		  <text>maven-metadata.xml</text>
		  <leaf>true</leaf>
		  <lastModified>2021-03-11 00:17:48.0 UTC</lastModified>
		  <sizeOnDisk>376</sizeOnDisk>
		</content-item>
		<content-item>
		  <resourceURI>https://oss.sonatype.org/service/local/repositories/comsoywiz-1229/content/korlibs/easy-kotlin-mpp-gradle-plugin/maven-metadata.xml.sha256</resourceURI>
		  <relativePath>/korlibs/easy-kotlin-mpp-gradle-plugin/maven-metadata.xml.sha256</relativePath>
		  <text>maven-metadata.xml.sha256</text>
		  <leaf>true</leaf>
		  <lastModified>2021-03-11 00:17:49.0 UTC</lastModified>
		  <sizeOnDisk>64</sizeOnDisk>
		</content-item>
		<content-item>
		  <resourceURI>https://oss.sonatype.org/service/local/repositories/comsoywiz-1229/content/korlibs/easy-kotlin-mpp-gradle-plugin/maven-metadata.xml.sha1</resourceURI>
		  <relativePath>/korlibs/easy-kotlin-mpp-gradle-plugin/maven-metadata.xml.sha1</relativePath>
		  <text>maven-metadata.xml.sha1</text>
		  <leaf>true</leaf>
		  <lastModified>2021-03-11 00:17:49.0 UTC</lastModified>
		  <sizeOnDisk>40</sizeOnDisk>
		</content-item>
		<content-item>
		  <resourceURI>https://oss.sonatype.org/service/local/repositories/comsoywiz-1229/content/korlibs/easy-kotlin-mpp-gradle-plugin/maven-metadata.xml.md5</resourceURI>
		  <relativePath>/korlibs/easy-kotlin-mpp-gradle-plugin/maven-metadata.xml.md5</relativePath>
		  <text>maven-metadata.xml.md5</text>
		  <leaf>true</leaf>
		  <lastModified>2021-03-11 00:17:49.0 UTC</lastModified>
		  <sizeOnDisk>32</sizeOnDisk>
		</content-item>
	  </data>
	</content>
	 */
}
