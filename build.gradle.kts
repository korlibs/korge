import korlibs.root.*
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    //id "com.dorongold.task-tree" version "2.1.1"
    // ./gradlew :kds:compileKotlinJs taskTree
}

korlibs.root.RootKorlibsPlugin.doInit(rootProject)

// Used to verify we are publishing with iOS references even if we do the publishing from another machine like windows or linux
tasks {
    val checkModulePublication by creating {
        doLast {
            val publishedKorgeModule = File("${System.getProperty("user.home")}/.m2/repository/com/soywiz/korge/korge/999.0.0.999/korge-999.0.0.999.module")
            val publishedKorgeModuleText = publishedKorgeModule.readText()
            for (ref in listOf("jvmApiElements", "jsApiElements", "android", "iosArm64")) {
                check (ref in publishedKorgeModuleText) {
                    System.err.println(publishedKorgeModuleText)
                    "Can't find '$ref' on the published '$publishedKorgeModule'"
                }
            }
        }
    }
}

afterEvaluate {
    allprojects {
        val publishing = extensions.findByType(PublishingExtension::class.java)
        if (publishing == null) {
            val copyArtifactsToDirectory by tasks.registering(Task::class) {
            }
        } else {
            val copyArtifactsToDirectory by tasks.registering(Task::class) {
                dependsOn("publishToMavenLocal")

                doLast {
                    val base = rootProject.layout.buildDirectory.dir("artifacts")
                    for (pub in publishing.publications.filterIsInstance<MavenPublication>()) {
                        //println(pub.artifacts.toList())
                        val basePath = pub.groupId.replace(".", "/") + "/" + pub.artifactId + "/" + pub.version
                        val baseDir = File(base.get().asFile, basePath)

                        val m2Dir = File(File(System.getProperty("user.home"), ".m2/repository"), basePath)

                        //println("m2Dir=$m2Dir")
                        // .module
                        copy {
                            from(m2Dir)
                            into(baseDir)
                        }
                    }
                }
            }
        }
    }
}

val mversion = project.getForcedVersion()

tasks {
    val generateArtifactsZip by registering(Zip::class) {
        subprojects {
            dependsOn("${this.path}:copyArtifactsToDirectory")
        }
        from(rootProject.layout.buildDirectory.dir("artifacts"))
        archiveFileName = "korge-$mversion.zip"
        destinationDirectory = rootProject.layout.buildDirectory
    }

    val generateArtifactsTar by registering(Tar::class) {
        subprojects {
            dependsOn("${this.path}:copyArtifactsToDirectory")
        }
        from(rootProject.layout.buildDirectory.dir("artifacts"))
        //compression = Compression.GZIP
        //into(rootProject.layout.buildDirectory)
        archiveFileName = "korge-$mversion.tar"
        destinationDirectory = rootProject.layout.buildDirectory
    }

    // winget install zstd
    val generateArtifactsTarZstd by registering(Exec::class) {
        val rootFile = rootProject.layout.buildDirectory.asFile.get()
        dependsOn(generateArtifactsTar)
        commandLine(
            "zstd", "-z",
            //"--ultra", "-22",
            "-17",
            "-f", File(rootFile, "korge-$mversion.tar").absolutePath,
            "-o", File(rootFile, "korge-$mversion.tar.zstd").absolutePath
        )
    }
}
