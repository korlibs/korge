package korlibs.korge.gradle.processor

import korlibs.korge.gradle.util.*

open class CatalogGenerator : KorgeResourceProcessor {
    override fun processFolder(context: KorgeResourceProcessorContext) {
        val map = LinkedHashMap<String, Any?>()
        for (folder in (context.resourceFolders + context.generatedFolder)) {
            for (file in (folder.listFiles()?.toList() ?: emptyList())) {
                if (file.name == "\$catalog.json") continue
                if (file.name.startsWith(".")) continue
                val fileName = if (file.isDirectory) "${file.name}/" else file.name
                map[fileName] = listOf(file.length(), file.lastModified())
            }
        }
        //println("-------- $folders")
        //println("++++++++ $files")
        context.generatedFolder["\$catalog.json"].writeText(Json.stringify(map))
        //println("generatedFolder: $generatedFolder")
    }
}
