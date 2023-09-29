import korlibs.korge.gradle.targets.android.*
import korlibs.root.*

plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.kotlin.jvm")
}

description = "Multiplatform Game Engine written in Kotlin"
group = RootKorlibsPlugin.KORGE_GRADLE_PLUGIN_GROUP
//group = "korlibs.korge"
//this.name = "korlibs.korge.gradle.plugin"

dependencies {
    implementation(project(":korge-gradle-plugin-common"))
}

gradlePlugin {
    website.set("https://korge.soywiz.com/")
    vcsUrl.set("https://github.com/korlibs/korge-plugins")
    //tags = ["korge", "game", "engine", "game engine", "multiplatform", "kotlin"]

	plugins {
        val korge by creating {
            //PluginDeclaration decl = it
			//id = "korlibs.korge"
            id = "com.soywiz.korge"
			displayName = "Korge"
			description = "Multiplatform Game Engine for Kotlin"
			implementationClass = "korlibs.korge.gradle.KorgeGradlePlugin"
		}
        val `korge-library` by creating {
            //PluginDeclaration decl = it
            //id = "korlibs.korge"
            id = "com.soywiz.korge.library"
            displayName = "Korge"
            description = "Multiplatform Game Engine for Kotlin"
            implementationClass = "korlibs.korge.gradle.KorgeLibraryGradlePlugin"
        }

        val kproject by creating {
            id = "com.soywiz.kproject"
            displayName = "kproject"
            description = "Allows to use sourcecode & git-based dependencies"
            implementationClass = "com.soywiz.kproject.KProjectPlugin"
        }
        val kprojectRoot by creating {
            id = "com.soywiz.kproject.root"
            displayName = "kproject"
            description = "Allows to use sourcecode & git-based dependencies"
            implementationClass = "com.soywiz.kproject.KProjectRootPlugin"
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
    setSourceCompatibility(jversion)
    setTargetCompatibility(jversion)
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = jversion
        apiVersion = "1.7"
        languageVersion = "1.7"
    }
}

kotlin.sourceSets.main.configure {
    kotlin.srcDirs(File(projectDir, "build/srcgen"), File(projectDir, "build/srcgen2"))
}
kotlin.sourceSets.test.configure {
    kotlin.srcDirs(File(projectDir, "build/testgen2"))
}
java.sourceSets.main.configure {
    resources.srcDirs(File(projectDir, "build/srcgen2res"))
}

dependencies {
    //implementation(project(":korge-gradle-plugin-common"))

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
    testImplementation(libs.junit)

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
        @Suppress("UNUSED_VARIABLE")
        val publishJvmPublicationToMavenRepository = tasks.register("publishJvmPublicationToMavenRepository", Task::class) {
            group = "publishing"
            dependsOn("publishPluginMavenPublicationToMavenRepository")
            dependsOn("publishKorgePluginMarkerMavenPublicationToMavenRepository")
            dependsOn("publishKorgeLibraryPluginMarkerMavenPublicationToMavenRepository")
            dependsOn("publishKprojectPluginMarkerMavenPublicationToMavenRepository")
            dependsOn("publishKprojectRootPluginMarkerMavenPublicationToMavenRepository")
        }
    }
}

val jvmTest = tasks.register("jvmTest", Task::class) {
    dependsOn("test")
}

korlibs.NativeTools.groovyConfigurePublishing(project, false)
korlibs.NativeTools.groovyConfigureSigning(project)
