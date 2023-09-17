package korlibs.korge.gradle

import org.gradle.api.Project

val Project.korteVersion get() = findProperty("korteVersion") ?: BuildVersions.KORTE
val Project.klockVersion get() = findProperty("klockVersion") ?: BuildVersions.KLOCK
val Project.kmemVersion get() = findProperty("kmemVersion") ?: BuildVersions.KMEM
val Project.kryptoVersion get() = findProperty("kryptoVersion") ?: BuildVersions.KRYPTO
val Project.kdsVersion get() = findProperty("kdsVersion") ?: BuildVersions.KDS
val Project.korioVersion get() = findProperty("korioVersion") ?: BuildVersions.KORIO
val Project.kormaVersion get() = findProperty("kormaVersion") ?: BuildVersions.KORMA
val Project.korauVersion get() = findProperty("korauVersion") ?: BuildVersions.KORAU
val Project.korimVersion get() = findProperty("korimVersion") ?: BuildVersions.KORIM
//val Project.koruiVersion get() = findProperty("koruiVersion") ?: BuildVersions.KORUI
//val Project.korevVersion get() = findProperty("korevVersion") ?: BuildVersions.KOREV
val Project.korgwVersion get() = findProperty("korgwVersion") ?: BuildVersions.KORGW
val Project.jnaVersion get() = findProperty("jnaVersion") ?: BuildVersions.JNA
val Project.korgeVersion get() = findProperty("korgeVersion") ?: BuildVersions.KORGE
val Project.kotlinVersion: String get() = findProperty("kotlinVersion")?.toString() ?: BuildVersions.KOTLIN
val Project.kotlinVersionIsDev: Boolean get() = kotlinVersion.contains("-release") || kotlinVersion.contains("-eap") || kotlinVersion.contains("-M") || kotlinVersion.contains("-RC")
val Project.androidBuildGradleVersion get() = findProperty("androidBuildGradleVersion") ?: BuildVersions.ANDROID_BUILD
val Project.coroutinesVersion get() = findProperty("coroutinesVersion") ?: BuildVersions.COROUTINES

fun Project.getModuleVersion(name: String, defaultVersion: Any): Any {
	return when (name.split(':').last().trim().toLowerCase().split('-').first().trim()) {
        "krypto" -> kryptoVersion
		"klock" -> klockVersion
		"kmem" -> kmemVersion
		"kds" -> kdsVersion
        "jna" -> jnaVersion
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
