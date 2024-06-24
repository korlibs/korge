package korlibs.korge.kotlincompiler.maven

import org.w3c.dom.*
import java.io.*
import javax.xml.*
import javax.xml.parsers.*

class Pom(
    val packaging: String? = null,
    val deps: List<MavenDependency> = emptyList(),
) {
    companion object {
        fun parse(file: File): Pom = parse(file.readText())
        fun parse(text: String): Pom {
            val db = DocumentBuilderFactory.newInstance().also { it.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }.newDocumentBuilder()
            val doc = db.parse(text.byteInputStream())
            val out = arrayListOf<MavenDependency>()
            val node = doc.getElementsByTagName("packaging").toList().firstOrNull()
            for (e in doc.getElementsByTagName("dependency").toList()) {
                val groupId = e.findChildByTagName("groupId").firstOrNull()?.textContent?.trim() ?: error("Missing groupId")
                val artifactId = e.findChildByTagName("artifactId").firstOrNull()?.textContent?.trim() ?: error("Missing artifactId")
                val scope = e.findChildByTagName("scope").firstOrNull()?.textContent?.trim()
                if (scope == "test" || scope == null) continue
                val version = e.findChildByTagName("version").firstOrNull()?.textContent?.trim() ?: error("Missing version for $groupId:$artifactId in $text")
                if (version.contains("\$")) continue
                out += MavenDependency(MavenArtifact(groupId, artifactId, version), scope ?: "compile")
                //println("DEP: $groupId:$artifactId:$version  :: $scope")
            }
            return Pom(packaging = node?.textContent, deps = out)
        }

        private fun NodeList.toList(): List<Node> = (0 until length).map { item(it) }
        private fun Node.findChildByTagName(tagName: String): List<Node> = childNodes.toList().filter { it.nodeName.equals(tagName, ignoreCase = true) }
    }
}
