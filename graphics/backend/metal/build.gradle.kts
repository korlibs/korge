plugins {
    kotlin("multiplatform")
    id("io.kotest.multiplatform") version "5.0.2"
}

kotlin {

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":graphics:foundation"))
            }
        }
        val darwinTest by getting {
            dependencies {
                implementation("io.kotest:kotest-framework-engine:5.0.2")
            }
        }
    }

}
