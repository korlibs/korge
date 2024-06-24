package korlibs.korge.kotlincompiler.maven

import java.io.*
import java.net.*

object MavenTools {
    fun getMavenArtifacts(artifact: MavenArtifact, explored: MutableSet<MavenArtifact> = mutableSetOf()): Set<File> {
        val explore = ArrayDeque<MavenArtifact>()
        explore += artifact
        val out = mutableSetOf<MavenArtifact>()
        while (explore.isNotEmpty()) {
            val artifact = explore.removeFirst()
            if (artifact in explored) continue
            explored += artifact
            val pom = Pom.parse(getSingleMavenArtifact(artifact.copy(extension = "pom")))
            if (pom.packaging == null || pom.packaging == "jar") {
                out += artifact
            }
            for (dep in pom.deps) {
                explore += dep.artifact
            }
        }
        return out.map { getSingleMavenArtifact(it) }.toSet()
    }

    fun getSingleMavenArtifact(artifact: MavenArtifact): File {
        val file = File(System.getProperty("user.home"), ".m2/repository/${artifact.localPath}")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            val url = URL("https://repo1.maven.org/maven2/${artifact.localPath}")
            println("Downloading $url")
            file.writeBytes(url.readBytes())
        }
        return file
    }

}
