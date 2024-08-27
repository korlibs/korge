package korlibs.modules

// https://central.sonatype.org/publish/publish-guide/#deployment

import com.google.gson.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import java.io.*

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
				if (!Sonatype.fromProject(rootProject).releaseRepositoryID(rootProject.stagedRepositoryId)) {
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

        //fun fromGlobalConfig(): Sonatype {
        //    val props = Properties().also { it.load(File(System.getProperty("user.home") + "/.gradle/gradle.properties").readText().reader()) }
        //    return Sonatype(props["sonatypeUsername"].toString(), props["sonatypePassword"].toString(), DEFAULT_BASE)
        //}

        fun fromProject(project: Project): Sonatype {
            return Sonatype(project.sonatypePublishUser, project.sonatypePublishPassword)
        }

        //@JvmStatic
        //fun main(args: Array<String>) {
        //    val sonatype = fromGlobalConfig()
        //    sonatype.releaseGroupId("korlibs")
        //}
    }

    fun releaseGroupId(groupId: String = "korlibs"): Boolean {
        println("Trying to release groupId=$groupId")
        val profileId = findProfileIdByGroupId(groupId)
        println("Determined profileId=$profileId")
        val repositoryIds = findProfileRepositories(profileId)
        if (repositoryIds.isEmpty()) {
            println("Can't find any repositories for profileId=$profileId for groupId=$groupId. Artifacts weren't upload?")
            return false
        }
        return releaseRepositoryIDs(repositoryIds)
    }

    fun releaseRepositoryID(repositoryId: String?): Boolean {
        val repositoryIds = listOfNotNull(repositoryId)
        if (repositoryIds.isEmpty()) return false
        return releaseRepositoryIDs(repositoryIds)
    }

    fun releaseRepositoryIDs(repositoryIds: List<String>): Boolean {
        val repositoryIds = repositoryIds.toMutableList()
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
                        else -> throw e
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

    private val client get() = SimpleHttpClient(user, pass)

    fun getRepositoryState(repositoryId: String): RepoState {
        val info = client.requestWithRetry("${BASE}/repository/$repositoryId")
        //println("info: ${info.toStringPretty()}")
        return RepoState(
            repositoryId = repositoryId,
            type = info["type"].asString,
            notifications = info["notifications"].asInt,
            transitioning = info["transitioning"].asBoolean,
        )
    }

    fun getRepositoryActivity(repositoryId: String): String {
        val info = client.requestWithRetry("${BASE}/repository/$repositoryId/activity")
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
        client.requestWithRetry("${BASE}/bulk/close", getDataMapForRepository(repositoryId))
    }

    fun repositoryPromote(repositoryId: String) {
        client.requestWithRetry("${BASE}/bulk/promote", getDataMapForRepository(repositoryId))
    }

    fun repositoryDrop(repositoryId: String) {
        client.requestWithRetry("${BASE}/bulk/drop", getDataMapForRepository(repositoryId))
    }

    fun findProfileRepositories(profileId: String): List<String> {
        return client.requestWithRetry("${BASE}/profile_repositories")["data"].list
            .filter { it["profileId"].asString == profileId }
            .map { it["repositoryId"].asString }
    }

    fun findProfileIdByGroupId(groupId: String): String {
        val profiles = client.requestWithRetry("$BASE/profiles")["data"].list
        return profiles
            .filter { groupId.startsWith(it["name"].asString) }
            .map { it["id"].asString }
            .firstOrNull() ?: error("Can't find profile with group id '$groupId'")
    }

    fun startStagedRepository(profileId: String): String {
        return client.requestWithRetry("${BASE}/profiles/$profileId/start", mapOf(
            "data" to mapOf("description" to "Explicitly created by easy-kotlin-mpp-gradle-plugin")
        ))["data"]["stagedRepositoryId"].asString
    }

    operator fun JsonElement.get(key: String): JsonElement = asJsonObject.get(key)
    val JsonElement.list: JsonArray get() = asJsonArray
    fun JsonElement.toStringPretty() = GsonBuilder().setPrettyPrinting().create().toJson(this)
}
