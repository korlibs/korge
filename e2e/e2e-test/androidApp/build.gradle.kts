plugins {
    alias(libs.plugins.android.application)
}

dependencies {
    implementation(projects.shared)
    implementation(libs.korge)
    implementation(libs.korge.core)
}

android {
    namespace = "org.korge.e2e"
    compileSdk = libs.versions.compileSdk.get().toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    defaultConfig {
        applicationId = "org.korge.e2e.test"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // TODO Consider moving this to the shared module or the korge plugin configurations
    sourceSets {
        getByName("main") {
            assets.directories.add("../shared/src/commonMain/resources")
        }
    }

    // Ignore most of the configuration like packaging and signing, as we do not care in e2e tests
    packaging {
        resources {
            excludes.addAll(
                setOf(
                    "META-INF/DEPENDENCIES",
                    "META-INF/LICENSE",
                    "META-INF/LICENSE.txt",
                    "META-INF/license.txt",
                    "META-INF/NOTICE",
                    "META-INF/NOTICE.txt",
                    "META-INF/notice.txt",
                    "META-INF/LGPL*",
                    "META-INF/AL2.0",
                    "**/androidsupportmultidexversion.txt",
                    "META-INF/versions/9/previous-compilation-data.bin",
                )
            )
        }
    }
}

// TODO See if we have to add processed resources to sourcesets
