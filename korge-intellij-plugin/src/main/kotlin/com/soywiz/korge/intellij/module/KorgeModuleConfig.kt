package com.soywiz.korge.intellij.module

import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.util.*
import com.soywiz.korio.util.*
import com.soywiz.korio.util.encoding.*

class KorgeModuleConfig {
	var artifactGroup = "com.example"
	var artifactId = "example"
	var artifactVersion = "0.0.1"
	var projectType = ProjectType.Gradle
	var featuresToInstall: List<KorgeProjectTemplate.Features.Feature> = listOf()
	var korgeVersion = KorgeProjectTemplate.Versions.Version("1.5.0d")

	fun generate(template: KorgeProjectTemplate): Map<String, ByteArray> = LinkedHashMap<String, ByteArray>().apply {
		fun putTextFile(name: String, text: String) {
			put(name, text.toByteArray(Charsets.UTF_8))
		}

		fun putTextFile(name: String, file: Indenter.() -> Unit) {
			putTextFile(name, Indenter().also { file(it) }.toString())
		}

		val templateContext = mapOf(
			"korgeVersion" to korgeVersion,
			"artifactGroup" to artifactGroup,
			"artifactId" to artifactId,
			"features" to featuresToInstall.toSet()
		) + featuresToInstall.associate { "feature_${it.id}" to true }

		fun getFileFromGenerator(path: String): ByteArray = KorgeResources.getBytes("/com/soywiz/korge/intellij/generator/$path")

		println(templateContext)

		put("gradlew", getFileFromGenerator("gradlew"))
		put("gradlew_linux", getFileFromGenerator("gradlew_linux"))
		put("gradlew_win", getFileFromGenerator("gradlew_win"))
		put("gradlew_wine", getFileFromGenerator("gradlew_wine"))
		put("gradlew.bat", getFileFromGenerator("gradlew.bat"))
		put("gradle/wrapper/gradle-wrapper.jar", getFileFromGenerator("gradle/wrapper/gradle-wrapper.jar"))
		put("gradle/wrapper/gradle-wrapper.properties", getFileFromGenerator("gradle/wrapper/gradle-wrapper.properties"))

		for (file in template.files.files) {
			when (file.encoding) {
				"base64" -> {
					put(file.path, file.content.fromBase64IgnoreSpaces())
				}
				"", "text" -> {
					putTextFile(file.path, renderTemplate(file.content.trimIndent().trim(), templateContext))
				}
			}
		}
	}
}

enum class ProjectType(val id: String) {
	Gradle("gradle")
	//, GradleKotlinDsl("gradle-kotlin-dsl")
	;

	companion object {
		val BY_ID = values().associateBy { it.id }
		val BY_NAME = values().associateBy { it.name }
		val BY = BY_ID + BY_NAME

		operator fun invoke(name: String): ProjectType = BY[name] ?: error("Unknown project type $name")
	}
}

/*
data class KorgeVersion(
	val version: String,
	val kotlinVersion: String,
	val extraRepos: List<String> = listOf()
) : Comparable<KorgeVersion> {
	val semVersion = SemVer(version)
	val semKotlinVersion = SemVer(kotlinVersion)

	override fun compareTo(other: KorgeVersion): Int = this.semVersion.compareTo(other.semVersion)
	override fun toString(): String = semVersion.toString()
}

class SemVer(val version: String) : Comparable<SemVer> {
	private val parts1 = version.split('-', limit = 2)
	private val parts2 = parts1.first().split('.')

	val major = parts2.getOrNull(0)?.toIntOrNull() ?: 0
	val minor = parts2.getOrNull(1)?.toIntOrNull() ?: 0
	val patch = parts2.getOrNull(2)?.toIntOrNull() ?: 0
	val info = parts1.getOrNull(1) ?: ""

	override fun compareTo(other: SemVer): Int {
		return this.major.compareTo(other.major).takeIf { it != 0 }
			?: this.minor.compareTo(other.minor).takeIf { it != 0 }
			?: this.patch.compareTo(other.patch).takeIf { it != 0 }
			?: this.info.compareTo(other.info).takeIf { it != 0 }
			?: 0
	}

	override fun toString(): String = version
}
*/
