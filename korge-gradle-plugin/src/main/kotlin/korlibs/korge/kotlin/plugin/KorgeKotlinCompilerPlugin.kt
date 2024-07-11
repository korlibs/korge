package korlibs.korge.kotlin.plugin

import korlibs.korge.gradle.plugin.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.model.*
import org.gradle.api.provider.*
import org.jetbrains.kotlin.gradle.plugin.*

class KorgeKotlinCompilerPlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.extensions.create("korgeplugin", KorgeKotlinPluginGradleExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = BuildConfig.KOTLIN_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.KOTLIN_PLUGIN_GROUP,
        artifactId = BuildConfig.KOTLIN_PLUGIN_NAME,
        version = BuildConfig.KOTLIN_PLUGIN_VERSION
    )

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(KorgeKotlinPluginGradleExtension::class.java)
        return project.provider {
            listOf(
                //SubpluginOption(key = "string", value = extension.stringProperty.get()),
                //SubpluginOption(key = "file", value = extension.fileProperty.get().asFile.path),
            )
        }
    }
}

open class KorgeKotlinPluginGradleExtension(objects: ObjectFactory) {
    //val stringProperty: Property<String> = objects.property(String::class.java)
    //val fileProperty: RegularFileProperty = objects.fileProperty()
}
