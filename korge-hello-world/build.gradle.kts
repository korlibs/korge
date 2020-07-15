plugins {
	application
}

dependencies {
	add("commonMainApi", project(":korge"))
}

application {
	mainClassName = "MainKt"
}

tasks {
	val runJvm by creating { dependsOn("run") }
	val runJs by creating { dependsOn("jsBrowserDevelopmentRun") }
}

kotlin {
	jvm {
	}
	js {
		browser {
			binaries.executable()
		}
	}
}
