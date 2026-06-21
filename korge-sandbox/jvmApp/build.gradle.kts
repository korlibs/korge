plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

description = "Korge Sandbox – JVM desktop entry point"
group = "org.korge.sandbox"
version = rootProject.libs.versions.korge.get()

dependencies {
    implementation(projects.korgeSandbox.shared)
}

application {
    mainClass.set("org.korge.sandbox.JvmMain")
}

tasks.withType<JavaExec>().configureEach {
    // Required arguments for the AWT/OpenGL Korge backend
    jvmArgs(
        "--add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
        "--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED",
    )
}
