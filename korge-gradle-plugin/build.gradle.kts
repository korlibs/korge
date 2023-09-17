plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    //id("com.gradle.plugin-publish")
    id("org.jetbrains.kotlin.jvm")
}

description = "Multiplatform Game Engine written in Kotlin"
group = "com.soywiz.korlibs.korge.plugins"
//group = "korlibs.korge"
//this.name = "korlibs.korge.gradle.plugin"

gradlePlugin {
    website = "https://korge.soywiz.com/"
    vcsUrl = "https://github.com/korlibs/korge"

    plugins {
        create("korge") {
            //PluginDeclaration decl = it
            //id = "korlibs.korge"
            id = "com.soywiz.korge"
            displayName = "Korge"
            description = "Multiplatform Game Engine for Kotlin"
            implementationClass = "korlibs.korge.gradle.KorgeGradlePlugin"
        }
        create("korge-library") {
            //PluginDeclaration decl = it
            //id = "korlibs.korge"
            id = "com.soywiz.korge.library"
            displayName = "Korge"
            description = "Multiplatform Game Engine for Kotlin"
            implementationClass = "korlibs.korge.gradle.KorgeLibraryGradlePlugin"
        }
    }
}

val jversion = libs.versions.javaVersion
//def jversion = korlibs.korge.gradle.targets.android.AndroidKt.GRADLE_JAVA_VERSION_STR


java {
    setSourceCompatibility(jversion.get())
    setTargetCompatibility(jversion.get())
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = jversion.get()
        //sourceCompatibility = jversion
        //apiVersion = "1.7"
        //languageVersion = "1.7"
        //jvmTarget = "1.6"
    }
}

kotlin.sourceSets.main.configure {
    //kotlin.srcDirs(File(buildDir, "srcgen"), File(buildDir, "srcgen2"))
}
kotlin.sourceSets.test.configure {
    //kotlin.srcDirs(File(buildDir, "testgen2"))
}
java.sourceSets.main.configure {
    //resources.srcDirs(File(buildDir, "srcgen2res"))
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization)

    implementation(libs.proguard.gradle)
    implementation(libs.closure.compiler)
    implementation(libs.gson)
    implementation(libs.gradle.publish.plugin)

    implementation(libs.kover)
    implementation(libs.dokka)

    implementation(libs.android.build.gradle)

    implementation(gradleApi())
    implementation(localGroovy())
    //compileOnly(gradleKotlinDsl())

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("io.mockk:mockk:1.11.0")
    testImplementation("junit:junit:4.13.2")

    //implementation(project(":korge-reload-agent"))
}


tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).configureEach {
    kotlinOptions.suppressWarnings = true
}

//def publishAllPublications = false

val publishJvmPublicationToMavenLocal = tasks.register("publishJvmPublicationToMavenLocal", Task::class) {
    group = "publishing"
    //dependsOn(publishAllPublications ? "publishToMavenLocal" : "publishPluginMavenPublicationToMavenLocal")
    dependsOn("publishPluginMavenPublicationToMavenLocal")
    dependsOn("publishKorgePluginMarkerMavenPublicationToMavenLocal")
}

// publishKorgePluginMarkerMavenPublicationToMavenLocal

afterEvaluate {
    //def publishTaskOrNull = tasks.findByName(publishAllPublications ? "publishAllPublicationsToMavenRepository" : "publishPluginMavenPublicationToMavenRepository")

    if (tasks.findByName("publishKorgePluginMarkerMavenPublicationToMavenRepository") != null) {
        val publishJvmPublicationToMavenRepository = tasks.register("publishJvmPublicationToMavenRepository", Task::class) {
            group = "publishing"
            dependsOn("publishPluginMavenPublicationToMavenRepository")
            dependsOn("publishKorgePluginMarkerMavenPublicationToMavenRepository")
        }
    }
}

val jvmTest = tasks.register("jvmTest", Task::class) {
    dependsOn("test")
}

//korlibs.NativeTools.groovyConfigurePublishing(project, false)
//korlibs.NativeTools.groovyConfigureSigning(project)
