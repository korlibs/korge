plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(projects.shared)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback)
}

application {
    mainClass.set("MainKt")
}
