package korlibs.korge.gradle.typedresources

import java.net.*

class ResourceTools

fun getResourceBytes(path: String): ByteArray = getResourceURL(path).readBytes()
fun getResourceText(path: String): String = getResourceURL(path).readText()

fun getResourceURL(path: String): URL {
    return ResourceTools::class.java.getResource("/${path.trim('/')}")
        ?: error("Can't find '$path' in class loaders")
}
