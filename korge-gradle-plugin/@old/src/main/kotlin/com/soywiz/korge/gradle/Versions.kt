package com.soywiz.korge.gradle

import org.gradle.api.Project

val Project.klockVersion get() = findProperty("klockVersion") ?: BuildVersions.KLOCK
val Project.kmemVersion get() = findProperty("kmemVersion") ?: BuildVersions.KMEM
val Project.kdsVersion get() = findProperty("kdsVersion") ?: BuildVersions.KDS
val Project.korioVersion get() = findProperty("korioVersion") ?: BuildVersions.KORIO
val Project.kormaVersion get() = findProperty("kormaVersion") ?: BuildVersions.KORMA
val Project.korauVersion get() = findProperty("korauVersion") ?: BuildVersions.KORAU
val Project.korimVersion get() = findProperty("korimVersion") ?: BuildVersions.KORIM
//val Project.koruiVersion get() = findProperty("koruiVersion") ?: BuildVersions.KORUI
//val Project.korevVersion get() = findProperty("korevVersion") ?: BuildVersions.KOREV
val Project.korgwVersion get() = findProperty("korgwVersion") ?: BuildVersions.KORGW
val Project.korgeVersion get() = findProperty("korgeVersion") ?: BuildVersions.KORGE
val Project.kotlinVersion get() = findProperty("kotlinVersion") ?: BuildVersions.KOTLIN
val Project.androidBuildGradleVersion get() = findProperty("androidBuildGradleVersion") ?: BuildVersions.ANDROID_BUILD
val Project.coroutinesVersion get() = findProperty("coroutinesVersion") ?: BuildVersions.COROUTINES

fun Project.getModuleVersion(name: String, defaultVersion: Any): Any {
	return when (name.split(':').last().trim().toLowerCase().split('-').first().trim()) {
		"klock" -> klockVersion
		"kmem" -> kmemVersion
		"kds" -> kdsVersion
		"korio" -> korioVersion
		"korma" -> kormaVersion
		"korau" -> korauVersion
		"korim" -> korimVersion
		"korgw" -> korgwVersion
		"korge" -> korgeVersion
		"kotlin" -> kotlinVersion
		"kotlinx.coroutines" -> coroutinesVersion
		else -> defaultVersion
	}
}