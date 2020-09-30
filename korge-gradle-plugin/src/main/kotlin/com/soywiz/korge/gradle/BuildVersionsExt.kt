package com.soywiz.korge.gradle

fun BuildVersions.isEAP() = BuildVersions.KOTLIN.contains("eap") || BuildVersions.KOTLIN.contains("-M")
