package com.soywiz.korge.admob.plugin

import com.soywiz.korge.plugin.KorgePluginExtension
import com.soywiz.korio.lang.quoted

class AdmobKorgePluginExtension : KorgePluginExtension(
	AdmobKorgePluginExtension::ADMOB_APP_ID
) {
	lateinit var ADMOB_APP_ID: String

	override fun getAndroidInit(): String? =
		"""try { com.google.android.gms.ads.MobileAds.initialize(com.soywiz.korio.android.androidContext(), ${ADMOB_APP_ID.quoted}) } catch (e: Throwable) { e.printStackTrace() }"""

	override fun getAndroidManifestApplication(): String? =
		"""<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value=${ADMOB_APP_ID.quoted} />"""

	override fun getAndroidDependencies() =
		listOf("com.google.android.gms:play-services-ads:16.0.0")

}
