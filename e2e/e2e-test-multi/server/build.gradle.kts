plugins {
    application
}

apply(plugin = "kotlin")

dependencies {
    add("implementation", project(":shared"))
    add("implementation", libs.ktor.server.netty)
    add("implementation", libs.logback)

}

application {
    mainClass.set("MainKt")
}
