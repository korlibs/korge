import korlibs.korge.gradle.targets.android.GRADLE_JAVA_VERSION_STR
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.kotlin.jvm")
}

description = "Multiplatform Game Engine written in Kotlin"
group = "com.soywiz.korlibs.korge.plugins"
//group = "korlibs.korge"
//this.name = "korlibs.korge.gradle.plugin"

gradlePlugin {
    website = "https://korge.soywiz.com/"
    vcsUrl = "https://github.com/korlibs/korge-plugins"
    //tags = ["korge", "game", "engine", "game engine", "multiplatform", "kotlin"]

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

/*
afterEvaluate {
    GenerateMavenPom generatePomFileForKorgePluginMarkerMavenPublication = tasks.findByName("generatePomFileForKorgePluginMarkerMavenPublication")
    generatePomFileForKorgePluginMarkerMavenPublication.pom.licenses {
        it.license {
            it.name = "MIT"
        }
    }
}
*/

val jversion = GRADLE_JAVA_VERSION_STR

java {
    sourceCompatibility = jversion
    targetCompatibility = jversion
}

tasks.withType(KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = jversion.majorVersion
//        sourceCompatibility = jversion
        apiVersion = "1.7"
        languageVersion = "1.7"
		//jvmTarget = "1.6"
    }
}

kotlin.sourceSets.main.map {
    it.kotlin.srcDirs(File(buildDir, "srcgen"), File(buildDir, "srcgen2"))
}
kotlin.sourceSets.test.map {
    it.kotlin.srcDirs(File(buildDir, "testgen2"))
}
java.sourceSets.main.map {
    it.resources.srcDirs(File(buildDir, "srcgen2res"))
}

korlibs.NativeTools.groovyConfigurePublishing(project, false)
korlibs.NativeTools.groovyConfigureSigning(project)

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

tasks.withType(KotlinCompile::class) {
    kotlinOptions.suppressWarnings = true
}

//val publishAllPublications = false

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
