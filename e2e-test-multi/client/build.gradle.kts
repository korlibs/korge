import korlibs.korge.gradle.*

apply<korlibs.korge.gradle.KorgeGradlePlugin>()

korge {
    id = "com.sample.clientserver"
    targetJvm()
    //targetJs()
}

dependencies {
    add("commonMainImplementation", project(":shared"))
}
