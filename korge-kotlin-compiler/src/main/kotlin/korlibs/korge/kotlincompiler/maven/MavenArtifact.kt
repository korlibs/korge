package korlibs.korge.kotlincompiler.maven

data class MavenArtifact(val group: String, val name: String, val version: String, val classifier: String? = null, val extension: String = "jar") {
    val groupSeparator by lazy { group.replace(".", "/") }
    val localPath by lazy { "$groupSeparator/$name/$version/$name-$version.$extension" }
}
