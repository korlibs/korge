import groovy.util.*
import groovy.xml.*

val playServicesVersion = "16.0.0"

dependencies {
	//add("androidMainApi", "com.google.android.gms:play-services-ads:17.1.2")
	//add("androidMainApi", "com.google.android.gms:play-services-ads:11.0.4")
	add("androidMainApi", "com.google.android.gms:play-services-ads:$playServicesVersion")
}

afterEvaluate {
	extensions.getByType<PublishingExtension>().apply {
		val publication = publications["kotlinMultiplatform"] as MavenPublication
		publication.artifact(File(buildDir, "korge-plugin.korge-plugin").apply {
			val node = Node(null, "korge-plugin").apply {
				appendNode("name").setValue("korge-admob")
				appendNode("version").setValue(version)
				appendNode("variables", mapOf("ADMOB_APP_ID" to "string"))
				appendNode("android").apply {
					appendNode("init", mapOf("require" to "ADMOB_APP_ID")).setValue("try { com.google.android.gms.ads.MobileAds.initialize(com.soywiz.korio.android.androidContext(), \"\${ADMOB_APP_ID}\") } catch (e: Throwable) { e.printStackTrace() }")
					appendNode("manifest-application", mapOf("require" to "ADMOB_APP_ID")).setValue("<meta-data android:name=\"com.google.android.gms.ads.APPLICATION_ID\" android:value=\"\${ADMOB_APP_ID}\" />")

					appendNode("dependencies").apply {
						appendNode("dependency", mapOf("require" to "ADMOB_APP_ID")).setValue("com.google.android.gms:play-services-ads:$playServicesVersion")
					}
					//appendNode("manifest").setValue("""
					//	<manifest>
					//		<application>
					//			<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="${'$'}ADMOB_APP_ID"/>
					//		</application>
					//	</manifest>
					//""".trimIndent())
				}
				appendNode("cordova").apply {
					appendNode("plugins").apply {
						appendNode("cordova-plugin-admob-free", mapOf("ADMOB_APP_ID" to "\${ADMOB_APP_ID}"))
					}
				}
			}
			writeText(XmlUtil.serialize(node))
		})
	}
}
