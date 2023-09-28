import korlibs.korge.gradle.*

apply<korlibs.korge.gradle.KorgeGradlePlugin>()

korge {
    id = "com.sample.clientserver"
    targetJvm()
    serializationJson()
    //targetJs()
}

dependencies {
    add("commonMainImplementation", project(":shared"))
}
