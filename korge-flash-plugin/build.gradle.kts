import org.apache.tools.ant.taskdefs.condition.Os
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/*
buildscript {
	repositories {
		maven {
			setUrl("https://plugins.gradle.org/m2/")
		}
	}
	dependencies {
		classpath("org.fx.gradlefx:gradlefx:1.4.0-SNAPSHOT")
	}
}
*/


val env = System.getenv()

val runtime = Runtime.getRuntime()
val airsdk by lazy {
	env["AIRSDK_HOME"] ?: env["AIRSDK"]
	?: throw IllegalArgumentException("AIRSDK_HOME environment variable not defined!")
}

// https://www.adobeexchange.com/resources/28
// http://www.adobeexchange.com/ExManCmd_win.zip
// http://www.adobeexchange.com/ExManCmd_mac.zip
val exman by lazy {
	env["EXMAN_HOME"] ?: env["EXMAN"] ?: throw IllegalArgumentException("EXMAN_HOME environment variable not defined!")
}

val isWindows = Os.isFamily(Os.FAMILY_WINDOWS)

fun passthru(vararg cmd: String): Boolean {
	val acmd = if (isWindows) listOf("cmd", "/c") + cmd.toList() else cmd.toList()
	println(acmd.joinToString(" "))
	return ProcessBuilder(*acmd.toTypedArray())
		.directory(File("."))
		.redirectOutput(ProcessBuilder.Redirect.INHERIT)
		.redirectError(ProcessBuilder.Redirect.INHERIT)
		.start()
		.waitFor(5, TimeUnit.MINUTES)
}

fun buildZip(items: Map<String, ByteArray>): ByteArray {
	val fout = java.io.ByteArrayOutputStream()
	val zout = ZipOutputStream(fout)
	//zout.setMethod(ZipOutputStream.STORED)
	for ((name, content) in items) {
		val ze = ZipEntry(name)
		ze.compressedSize = content.size.toLong()
		ze.size = content.size.toLong()
		val crc = java.util.zip.CRC32()
		crc.update(content)
		ze.crc = crc.value
		ze.method = ZipOutputStream.STORED
		println("- $name: ${content.size}")
		zout.putNextEntry(ze)
		zout.write(content)
		zout.closeEntry()
	}
	zout.finish()
	zout.close()
	fout.close()
	return fout.toByteArray()
}

fun buildZip(out: File, items: Map<String, ByteArray>) {
	out.writeBytes(buildZip(items))
}

//println(airsdk)


task("build") {
	doLast {
		File("build").mkdirs()
		println("hello")
		passthru("echo", "hello")
		File("build/KorgeEXT.swf").delete()
		File("build/KorgeEXT.zxp").delete()
		passthru(
			"$airsdk/bin/mxmlc",
			//"-debug=true",
			"-debug=false",
			"+configname=air",
			"-swf-version=34",
			//"-swf-version=12",
			//"-player-version=11",
			"-library-path=" + File(project.projectDir, "lib/minimalcomps-1.0.0.swc").absolutePath,
			"-source-path=" + File(project.projectDir, "src").absolutePath,
			File(project.projectDir, "src/Main.as").absolutePath,
			"-output=" + File(project.buildDir, "KorgeEXT.swf").absolutePath
		)

		buildZip(
			File("build/KorgeEXT.zxp"), mapOf(
				"KorgeEXT.mxi" to File(project.projectDir, "resources/KorgeEXT.mxi").readBytes(),
				"KorgeEXT.swf" to File(project.buildDir, "KorgeEXT.swf").readBytes()
			)
		)
	}
}

/*
task("install") {
	dependsOn("build")
	doLast {
		passthru(
			"$exman/ExManCmd",
			"/install",
			File("build/KorgeEXT.zxp").absolutePath
		)
	}
}
*/

task("uninstall") {
	doLast {
		passthru(
			"$exman/ExManCmd",
			"/remove",
			"KorgeEXT"
		)
	}
}

