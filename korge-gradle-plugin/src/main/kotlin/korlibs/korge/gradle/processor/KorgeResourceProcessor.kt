package korlibs.korge.gradle.processor

import java.io.File
import java.util.ServiceLoader
import korlibs.korge.gradle.util.isDescendantOf

data class KorgeResourceProcessorContext(
    val logger: org.slf4j.Logger,
    val generatedFolder: File,
    val resourceFolders: List<File>,
) {
    val skippedFiles = LinkedHashSet<String>()

    /**
     * Prevents copying that [files] or folder to the final executable
     */
    fun skipFiles(vararg files: File) {
        for (file in files) {
            for (folder in resourceFolders) {
                if (file.isDescendantOf(folder)) {
                    skippedFiles += file.relativeTo(folder).path + (if (file.isDirectory) "/" else "")
                    break
                }
            }
        }
    }
}

fun interface KorgeResourceProcessor {
    fun processFolder(context: KorgeResourceProcessorContext)

    companion object {
        fun getAll(): List<KorgeResourceProcessor> {
            return (ServiceLoader.load(KorgeResourceProcessor::class.java).toList() + CatalogGenerator())
        }
    }
}
