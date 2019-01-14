package com.soywiz.korge.gradle

import org.junit.Test
import kotlin.test.*

class CordovaXmlFile {
    val sampleXml = """
		<?xml version='1.0' encoding='utf-8'?>
		<widget id="com.soywiz.sample1" version="1.0.0" xmlns="http://www.w3.org/ns/widgets">
			<name>sample</name>
			<description>sample</description>
			<author email="dev@cordova.apache.org" href="http://cordova.io">Apache Cordova Team</author>
			<content src="index.html" />
			<preference name="Orientation" value="landscape" />
			<preference name="BackgroundColor" value="0xff000000" />
		</widget>
	""".trimIndent()

    @Test
    fun test() {
        val extension = KorgeExtension().apply {
            this.id = "com.soywiz.myid"
            this.version = "0.0.1"
            this.author("demo", "demo@demo.com", "https://demo.com")
            this.orientation = Orientation.PORTRAIT
            this.fullscreen = false
            this.backgroundColor = 0xFFFF3333.toInt()
        }
        assertEquals(
            """
			<?xml version='1.0' encoding='utf-8'?>
			<widget xmlns="http://www.w3.org/ns/widgets" id="com.soywiz.myid" version="0.0.1">
			  <name>unnamed</name>
			  <description>undescripted</description>
			  <author email="demo@demo.com" href="https://demo.com">demo</author>
			  <content src="index.html"/>
			  <preference name="Orientation" value="portrait"/>
			  <preference name="Fullscreen" value="false"/>
			  <preference name="BackgroundColor" value="0xffff3333"/>
			  <icon src="icon.png"/>
			</widget>
		""".trimIndent().trimEnd(), extension.updateCordovaXmlString(sampleXml).trimEnd()
        )
    }

    @Test
    fun test2() {
        val extension = KorgeExtension().apply {
            androidMinSdk = "19"
        }
        assertEquals(
            """
			<?xml version='1.0' encoding='utf-8'?>
			<widget xmlns="http://www.w3.org/ns/widgets" id="com.unknown.unknownapp" version="0.0.1">
			  <name>unnamed</name>
			  <description>undescripted</description>
			  <author email="unknown@unknown" href="http://localhost">unknown</author>
			  <content src="index.html"/>
			  <preference name="Orientation" value="default"/>
			  <preference name="Fullscreen" value="true"/>
			  <preference name="BackgroundColor" value="0xff000000"/>
			  <platform name="android">
			    <preference name="android-minSdkVersion" value="19"/>
			  </platform>
			  <icon src="icon.png"/>
			</widget>
		""".trimIndent().trimEnd(), extension.updateCordovaXmlString(sampleXml).trimEnd()
        )
    }
}

