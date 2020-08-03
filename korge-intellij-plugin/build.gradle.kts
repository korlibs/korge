/*
buildscript {
	val kotlinVersion: String by project

	repositories {
		mavenLocal()
		mavenCentral()
		jcenter()
		maven { url = uri("https://plugins.gradle.org/m2/") }
		//maven { url = uri("https://dl.bintray.com/kotlin/kotlin-dev") }
	}
	dependencies {
		classpath("com.gradle.publish:plugin-publish-plugin:0.10.1")
		classpath("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:$kotlinVersion")
		classpath("gradle.plugin.org.jetbrains.intellij.plugins:gradle-intellij-plugin:0.4.16")
	}
}
*/
plugins {
	java
	idea
}

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.intellij")

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
	sourceCompatibility = JavaVersion.VERSION_1_8.toString()
	targetCompatibility = JavaVersion.VERSION_1_8.toString()
	kotlinOptions {
		freeCompilerArgs = listOf("-XXLanguage:+InlineClasses")
		apiVersion = "1.4"
		languageVersion = "1.4"
		jvmTarget = "1.8"
	}
}

java {
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
}

sourceSets {
    this.maybeCreate("main").apply {
        java {
            srcDirs("src/main/kotlin")
        }
        resources {
            srcDirs("src/main/resources")
            srcDirs("src/main/resources2")
        }
    }
}

val korgeVersion: String by project
val kotlinVersion: String by project

dependencies {
	//implementation("com.soywiz.korlibs.korge.plugins:korge-build:$korgeVersion")
    //implementation(project(":korte"))
    implementation(project(":korge"))
    implementation(project(":korge-swf"))
    implementation(project(":korge-dragonbones"))
    implementation(project(":korge-spine"))
	implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
	//implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.9.8")
	//implementation("javax.xml.bind:jaxb-api:2.3.1")
	//implementation("com.sun.xml.bind:jaxb-impl:2.3.1")
	//implementation("net.sourceforge.mydoggy:mydoggy:1.4.2")
	//implementation("net.sourceforge.mydoggy:mydoggy-plaf:1.4.2")
	//implementation("net.sourceforge.mydoggy:mydoggy-api:1.4.2")
	//implementation("net.sourceforge.mydoggy:mydoggy-res:1.4.2")
	//implementation(project(":korge-build"))
}

val globalProps = properties

extensions.getByType<org.jetbrains.intellij.IntelliJPluginExtension>().apply {
	//version = "IC-2019.3"; setPlugins("gradle", "java")
    version = "IC-2020.2"; setPlugins("gradle", "java", "platform-images", "Kotlin")

	//version = "IC-2019.3"; setPlugins("gradle")
	//version = "IC-2019.3"; setPlugins("gradle", "java", "Kotlin")
	//version = "IC-2018.3.5"
	//version = "IC-2018.3.4"; setPlugins("gradle", "maven")

	updateSinceUntilBuild = false

	pluginName = "KorgePlugin"
	downloadSources = true
}

tasks {
	val runIde by existing(org.jetbrains.intellij.tasks.RunIdeTask::class) {
		maxHeapSize = "2g"
	}
	val runDebugTilemap by creating(JavaExec::class) {
		//classpath = sourceSets.main.runtimeClasspath
		classpath = sourceSets["main"].runtimeClasspath + configurations["idea"]

		main = "com.soywiz.korge.intellij.editor.tile.MyTileMapEditorFrame"
	}
	val runUISample by creating(JavaExec::class) {
		//classpath = sourceSets.main.runtimeClasspath
		classpath = sourceSets["main"].runtimeClasspath + configurations["idea"]

		main = "com.soywiz.korge.intellij.ui.UIBuilderSample"
	}
	val publishPlugin by existing(org.jetbrains.intellij.tasks.PublishTask::class) {
		if (findProperty("jetbrainsUsername") != null) {
			setUsername(findProperty("jetbrainsUsername"))
			setPassword(findProperty("jetbrainsPassword"))
		}

	}
}
