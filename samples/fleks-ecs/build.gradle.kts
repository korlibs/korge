import com.soywiz.korge.gradle.BuildVersions

dependencies {
    add("commonMainApi", project(":korge"))
    // Use Fleks from korge-fleks module (internal version)
    //add("commonMainApi", project(":korge-fleks"))
    // Use Fleks from extern
    //add("commonMainImplementation", "io.github.quillraven.fleks:Fleks:${BuildVersions.FLEKS}")
    // Use snapshot of Fleks from maven local
    add("commonMainImplementation", "io.github.quillraven.fleks:Fleks:1.3.1-KMP-SNAPSHOT")
}
