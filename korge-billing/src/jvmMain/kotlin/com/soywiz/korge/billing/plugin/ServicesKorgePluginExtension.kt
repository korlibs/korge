package com.soywiz.korge.services.plugin

import com.soywiz.korge.plugin.KorgePluginExtension
import com.soywiz.korio.lang.quoted

class ServicesKorgePluginExtension : KorgePluginExtension(
    ServicesKorgePluginExtension::GOOGLE_APP_ID
) {
	lateinit var GOOGLE_APP_ID: String

	override fun getAndroidInit(): String? =
		""""""

	override fun getAndroidManifestApplication(): String? =
		""""""

	override fun getAndroidDependencies() =
		listOf(
            "com.google.android.gms:play-services-games:20.0.1",
            "com.google.android.gms:play-services-auth:18.1.0"
        )
}
