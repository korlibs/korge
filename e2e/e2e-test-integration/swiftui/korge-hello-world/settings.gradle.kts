pluginManagement { repositories {  mavenLocal(); mavenCentral(); google(); gradlePluginPortal()  }  }

plugins {
    //id("com.soywiz.kproject.settings") version "0.0.1-SNAPSHOT"
    id("com.soywiz.kproject.settings") version "0.3.1"
}

kproject("./deps")
