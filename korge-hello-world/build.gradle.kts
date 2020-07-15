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

	//val jsRun by creating { dependsOn("jsBrowserDevelopmentRun") } // Already available
	val jvmRun by creating { dependsOn("run") }
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
