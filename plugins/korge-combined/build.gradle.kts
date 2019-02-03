import java.util.*

apply(plugin = "kotlin")
apply(plugin = "maven")
apply(plugin = "maven-publish")

val gradleProperties = Properties().apply { this.load(File(rootDir, "../gradle.properties").readText().reader()) }

fun version(name: String) = when (name) {
    "korge" -> gradleProperties["projectVersion"]
    else -> gradleProperties["${name}Version"]
} ?: error("Can't find version for '$name'")

configurations {
    maybeCreate("rtArtifacts")
}
dependencies {
    add("rtArtifacts", "com.soywiz:korge-jvm:${version("korge")}")
    add("rtArtifacts", "com.soywiz:korge-swf-jvm:${version("korge")}")
    add("rtArtifacts", "com.soywiz:klogger-jvm:${version("klogger")}")
    add("rtArtifacts", "com.soywiz:korio-jvm:${version("korio")}")
    add("rtArtifacts", "com.soywiz:korau-jvm:${version("korau")}")
    add("rtArtifacts", "com.soywiz:kds-jvm:${version("kds")}")
    add("rtArtifacts", "com.soywiz:kmem-jvm:${version("kmem")}")
    add("rtArtifacts", "com.soywiz:korim-jvm:${version("korim")}")
    add("rtArtifacts", "com.soywiz:korma-jvm:${version("korma")}")
    add("rtArtifacts", "com.soywiz:korinject-jvm:${version("korinject")}")
    add("rtArtifacts", "com.soywiz:kgl-jvm:${version("kgl")}")
    add("rtArtifacts", "com.soywiz:korag-jvm:${version("korag")}")
    //add("rtArtifacts", "com.soywiz:korui-jvm:${version("korui")}")
    add("rtArtifacts", "com.soywiz:krypto-jvm:${version("krypto")}")
    add("rtArtifacts", "com.soywiz:korev-jvm:${version("korev")}")
    add("rtArtifacts", "com.soywiz:korgw-jvm:${version("korgw")}")
    add("rtArtifacts", "com.soywiz:klock-jvm:${version("klock")}")
}

val processRtArtifacts = tasks.create<Copy>("processRtArtifacts") {
    for (file in configurations["rtArtifacts"]) {
        if (!file.absolutePath.contains("kotlin-stdlib")) {
            //println(file)
            from(zipTree(file))
        }
    }
    into("build/genresources")
}

//kotlin.sourceSets.main.resources.srcDirs(project.file("build/genresources"))

val jar = tasks.getByName<Jar>("jar")
jar.dependsOn(processRtArtifacts)
jar.from(project.file("build/genresources"))

//println()
//processResources.dependsOn(processRtArtifacts)

println("---------------------- A")
publishing {
    println("---------------------- B")
    publications {
        //maven(MavenPublication) {
        configure(publications) {
            println("---------------------- C")
            this as MavenPublication
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            println("components: $components")
            from(components["java"])
        }
    }
}
