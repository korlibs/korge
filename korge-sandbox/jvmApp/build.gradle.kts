plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

description = "Korge Application – JVM desktop entry point"
group = "org.korge.application"
version = rootProject.libs.versions.korge.get()

dependencies {
    implementation(project(":korge"))
    implementation(project(":korge-sandbox:shared"))
    implementation(projects.korge)
//    implementation(projects.korgeApplication.shared)
}

application {
    mainClass.set("org.korge.application.JvmMain")
}

tasks.withType<JavaExec>().configureEach {
    // Required arguments for the AWT/OpenGL Korge backend
    jvmArgs(
        "--add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
        "--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED",
    )
}
