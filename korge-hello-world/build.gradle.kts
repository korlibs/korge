dependencies {
	add("commonMainApi", project(":korge"))
}

kotlin {
	js {
		browser {
			binaries.executable()
		}
	}
}
