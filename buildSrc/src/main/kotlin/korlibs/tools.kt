@file:Suppress("UNCHECKED_CAST")
package korlibs

import groovy.util.*
import groovy.xml.XmlUtil
import org.apache.tools.ant.taskdefs.condition.*
import org.gradle.api.*
import org.gradle.api.artifacts.dsl.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.io.*
import java.net.*
import java.util.zip.*
import org.gradle.internal.extensions.core.extra

fun MutableMap<String, Any>.applyProjectProperties(
    projectUrl: String,
    licenseName: String,
    licenseUrl: String
) {
    put("project.scm.url", projectUrl)
    put("project.license.name", "MIT License")
    put("project.license.url", "https://raw.githubusercontent.com/korlibs/korge/main/LICENSE")
    put("project.author.id", "korge")
    put("project.author.name", "KorGE Team")
    put("project.author.email", "info@korge.org")
}

fun MutableMap<String, Any>.includeKotlinNativeDesktop() {
    this["include.kotlin.native.desktop"] = true
}

// Extensions
operator fun File.get(name: String) = File(this, name)

var File.bytes
    get() = this.readBytes();
    set(value) {
        this.also { it.parentFile.mkdirs() }.writeBytes(value)
    }
var File.text
    get() = this.readText();
    set(value) {
        this.also { it.parentFile.mkdirs() }.writeText(value)
    }

fun File.ensureParents(): File = this.apply { this.parentFile.mkdirs() }

// File and archives
fun Project.downloadFile(url: URL, localFile: File, connectionTimeout: Int = 15_000, readTimeout: Int = 15_000) {
    logger.info("Downloading $url into $localFile ...")
    url.openConnection().also {
        it.connectTimeout = connectionTimeout
        it.readTimeout = readTimeout
    }.getInputStream().use { input ->
        localFile.ensureParents().writeBytes(input.readAllBytes())
        //FileOutputStream(localFile.ensureParents()).use { output -> input.copyTo(output) }
    }
}

fun Project.extractArchive(archive: File, output: File) {
    logger.info("Extracting $archive into $output ...")
    copy {
        when {
            archive.name.endsWith(".tar.gz") -> it.from(tarTree(resources.gzip(archive)))
            archive.name.endsWith(".zip") -> it.from(zipTree(archive))
            else -> error("Unsupported archive $archive")
        }
        it.into(output)
    }
}

// Gradle extensions
operator fun Project.invoke(callback: Project.() -> Unit) = callback(this)
operator fun DependencyHandler.invoke(callback: DependencyHandler.() -> Unit) = callback(this)
operator fun KotlinMultiplatformExtension.invoke(callback: KotlinMultiplatformExtension.() -> Unit) = callback(this)

inline fun <reified T : Any> DomainObjectCollection<T>.withType(): DomainObjectCollection<T> = withType(T::class.java)
inline fun <reified T : Plugin<*>> PluginCollection<T>.withType(): PluginCollection<T> = withType(T::class.java)
inline fun <T : Any> DomainObjectCollection<T>.allThis(noinline block: T.() -> Unit) = this.all(block)

inline fun <reified T: Task> TaskCollection<*>.withType(noinline block: T.() -> Unit) {
    return (this as TaskCollection<T>).withType(T::class.java).configureEach(block)
}
inline fun <reified T : Any> ExtensionContainer.configure(noinline block: T.() -> Unit) {
    return configure(T::class.java, Action { block(it) })
}

operator fun <T : Any> NamedDomainObjectCollection<T>.get(name: String): T = this.getByName(name)
val NamedDomainObjectCollection<KotlinTarget>.js: KotlinOnlyTarget<KotlinJsCompilation> get() = this["js"] as KotlinOnlyTarget<KotlinJsCompilation>
val NamedDomainObjectCollection<KotlinTarget>.jvm: KotlinOnlyTarget<KotlinJvmCompilation> get() = this["jvm"] as KotlinOnlyTarget<KotlinJvmCompilation>
val NamedDomainObjectCollection<KotlinTarget>.metadata: KotlinOnlyTarget<KotlinCommonCompilation> get() = this["metadata"] as KotlinOnlyTarget<KotlinCommonCompilation>
val <T : KotlinCompilation<*>> NamedDomainObjectContainer<T>.main: T get() = this["main"]
val <T : KotlinCompilation<*>> NamedDomainObjectContainer<T>.test: T get() = this["test"]

fun Project.kotlin(callback: KotlinMultiplatformExtension.() -> Unit) = extensions.getByType(KotlinMultiplatformExtension::class.java).apply(callback)

// Groovy tools
fun Node.toXmlString() = XmlUtil.serialize(this)

fun Project.doOnce(uniqueName: String, block: () -> Unit) {
    val key = "doOnce-$uniqueName"
    if (!rootProject.extra.has(key)) {
        rootProject.extra.set(key, true)
        block()
    }
}

fun Project.doOncePerProject(uniqueName: String, block: () -> Unit) {
    val key = "doOnceProject-${project.name}-$uniqueName"
    if (!rootProject.extra.has(key)) {
        rootProject.extra.set(key, true)
        block()
    }
}

fun currentJavaVersion(): Int {
    val versionElements = System.getProperty("java.version").split("\\.".toRegex()).toTypedArray() + arrayOf("-1", "-1")
    val discard = versionElements[0].toInt()
    return if (discard == 1) versionElements[1].toInt() else discard
}

fun unzipTo(output: File, zipFileName: File) {
    ZipFile(zipFileName).use { zip ->
        for (entry in zip.entries().asSequence()) {
            val outFile = File(output, entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                val bytes = zip.getInputStream(entry).use { it.readBytes() }
                outFile.parentFile.mkdirs()
                outFile.writeBytes(bytes)
            }
        }
    }
}
