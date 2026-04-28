pluginManagement { repositories {  mavenLocal(); mavenCentral(); google(); gradlePluginPortal()  }  }

plugins {
    //id("org.korge.engine.settings") version "0.0.1-SNAPSHOT"
    id("org.korge.engine.settings") version "0.3.1"
}

kproject("./deps")
