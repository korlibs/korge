package com.soywiz.korge.intellij.module

import com.soywiz.korge.intellij.config.*
import com.soywiz.korio.serialization.xml.*
import org.intellij.lang.annotations.*

@Suppress("unused")
open class KorgeProjectTemplate {
	val versions = Versions()
	val features = Features()
	val files = Files()

	class Versions {
		var versions = arrayListOf<Version>()

		data class Version(var text: String = "") {
			override fun toString(): String = text
		}
	}

	class Features {
		var features = arrayListOf<Feature>()
		val allFeatures by lazy { AllFeatures(features) }

		data class Feature(
			val id: String = "",
			val dependenciesString: String = "",
			val name: String = "",
			val description: String = "",
			val documentation: String = "",
			val group: String = "Features"
		) {
			val dependenciesList: List<String> get() = dependenciesString.split(" ").filter { it.isNotBlank() }
		}

		interface FeatureResolver {
			fun resolve(id: String): Feature?
		}

		class FeatureSet(features: Iterable<Feature>, val resolver: FeatureResolver) {
			val direct: Set<Feature> = features.toSet()
			val all: Set<Feature> = direct.flatMap { it.dependenciesList.map { resolver.resolve(it) } }.filterNotNull().toSet()
			val transitive: Set<Feature> = (all - direct)
		}

		class AllFeatures(val features: Iterable<Feature>) : FeatureResolver {
			val byId = features.associateBy { it.id }
			override fun resolve(id: String): Feature? = byId[id] ?: features.firstOrNull { it.id == id }
		}
	}

	class Files {
		var files = arrayListOf<TFile>()

		data class TFile(
			val path: String = "",
			val encoding: String = "text",
			val content: String = ""
		)
	}

	interface Provider {
		fun invalidate()
		val template: KorgeProjectTemplate
	}

	companion object {
		fun fromXml(@Language("XML") xml: String): KorgeProjectTemplate {
			val out = KorgeProjectTemplate()
			val root = Xml(xml)
			for (version in root["versions"]["version"]) out.versions.versions.add(Versions.Version(version.text))
			for (feature in root["features"]["feature"]) {
				out.features.features.add(Features.Feature(
					id = feature.str("id"),
					dependenciesString = feature.str("dependencies"),
					name = feature.str("name"),
					description = feature.str("description"),
					documentation = feature.str("documentation"),
					group = feature.str("group", "Features")
				))
			}
			for (file in root["files"]["file"]) out.files.files.add(Files.TFile(
				path = file.str("path"),
				encoding = file.str("encoding", "text"),
				content = file.text
			))
			return out
		}

		fun fromEmbeddedResource(): KorgeProjectTemplate =
			fromXml(
				KorgeProjectTemplate::class.java.getResource("/com/soywiz/korge/intellij/korge-templates.xml")?.readText()
					?: error("Can't find ¡korge-templates.xml' from esources")
			)

		fun fromUpToDateTemplate(): KorgeProjectTemplate = fromXml(korgeGlobalSettings.getCachedTemplate())

		fun provider() = object : Provider {
			override fun invalidate() {
				korgeGlobalSettings.invalidateCache()
				_template = null
			}
			private var _template: KorgeProjectTemplate? = null
			override val template: KorgeProjectTemplate get() {
				if (_template == null) {
					_template = fromUpToDateTemplate()
				}
				return _template!!
			}
		}
	}
}