plugins {
    base
    java
    `java-gradle-plugin`
    //kotlin("jvm") version "1.3.72"
    kotlin("jvm") version "1.4.0-rc"
    //maven
    //`maven-publish`
    //id("com.gradle.plugin-publish")
    //id 'base'
    //id 'java'
    //java
    //kotlin("multiplatform") version "1.4-M2"
    //id 'org.jetbrains.kotlin.multiplatform' version "1.4.0-rc"
    //id "org.jetbrains.kotlin.multiplatform" version "1.4.0-rc"
    //id "org.jetbrains.kotlin.jvm" version "1.4.0-rc"
}

repositories {
    mavenCentral()
    maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
    maven { url = uri("https://kotlin.bintray.com/kotlinx") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0-rc")
    //implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.72")
    implementation("net.sf.proguard:proguard-gradle:6.2.2")
    //implementation("com.android.tools.build:gradle:$androidBuildGradleVersion")

    implementation(gradleApi())
    implementation(localGroovy())
}
