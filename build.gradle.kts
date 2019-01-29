import com.moowork.gradle.node.*
import com.moowork.gradle.node.npm.*
import com.moowork.gradle.node.task.*
import groovy.util.*
import groovy.xml.*
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.io.*

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.3.0")
        classpath(kotlin("gradle-plugin", version = "1.3.20"))
    }
}

var hasAndroid: Boolean by extra
hasAndroid = (System.getProperty("sdk.dir") != null) || (System.getenv("ANDROID_HOME") != null)
if (!hasAndroid) {
	val trySdkDir = File(System.getProperty("user.home") + "/Library/Android/sdk")
	if (trySdkDir.exists()) {
		File(rootDir, "local.properties").writeText("sdk.dir=${trySdkDir.absolutePath}")
		hasAndroid = true
	}
}

plugins {
    id("kotlin-multiplatform").version("1.3.20")
    id("com.moowork.node").version("1.2.0")
}

allprojects {
    repositories {
        mavenLocal().apply {
            content {
                excludeGroup("Kotlin/Native")
            }
        }
        maven {
            url = uri("https://dl.bintray.com/soywiz/soywiz")
            content {
                includeGroup("com.soywiz")
                excludeGroup("Kotlin/Native")
            }
        }
        jcenter() {
            content {
                excludeGroup("Kotlin/Native")
            }
        }
        google().apply {
            content {
                excludeGroup("Kotlin/Native")
            }
        }
    }
}

operator fun File.get(name: String) = File(this, name)
var File.text get() = this.readText(); set(value) = run { this.writeText(value) }
val NamedDomainObjectCollection<KotlinTarget>.js get() = this["js"] as KotlinOnlyTarget<KotlinJsCompilation>
val NamedDomainObjectCollection<KotlinTarget>.jvm get() = this["jvm"] as KotlinOnlyTarget<KotlinJvmCompilation>
val NamedDomainObjectCollection<KotlinTarget>.metadata get() = this["metadata"] as KotlinOnlyTarget<KotlinCommonCompilation>

val <T : KotlinCompilation<*>> NamedDomainObjectContainer<T>.main get() = this["main"]
val <T : KotlinCompilation<*>> NamedDomainObjectContainer<T>.test get() = this["test"]

class MultiOutputStream(val outs: List<OutputStream>) : OutputStream() {
    override fun write(b: Int) = run { for (out in outs) out.write(b) }
    override fun write(b: ByteArray, off: Int, len: Int) = run { for (out in outs) out.write(b, off, len) }
    override fun flush()  = run { for (out in outs) out.flush() }
    override fun close()  = run { for (out in outs) out.close() }
}

subprojects {
    if (project.name == "template") return@subprojects

    apply(plugin = "kotlin-multiplatform")
    apply(plugin = "com.moowork.node")

    if (hasAndroid) {
        apply(plugin = "com.android.library")
        extensions.getByType<com.android.build.gradle.LibraryExtension>().apply {
            compileSdkVersion(28)
            defaultConfig {
                minSdkVersion(18)
                targetSdkVersion(28)
            }
        }
    }

    kotlin {
        if (hasAndroid) {
            android {
                publishLibraryVariants("release", "debug")
            }
        }

        iosX64()
        iosArm32()
        iosArm64()
        macosX64()
        linuxX64()
        mingwX64()
        jvm()
        js {
            compilations.all {
                kotlinOptions {
                    languageVersion = "1.3"
                    sourceMap = true
                    metaInfo = true
                    moduleKind = "umd"
                }
            }
        }

        // Only enable when loaded in IDEA (we use a property for detection). In CLI that would produce an "expect" error.

        if (System.getProperty("idea.version") != null) {
            when {
                Os.isFamily(Os.FAMILY_WINDOWS) -> run { mingwX64("nativeCommon"); mingwX64("nativePosix") }
                Os.isFamily(Os.FAMILY_MAC) -> run { macosX64("nativeCommon"); macosX64("nativePosix") }
                else -> run { linuxX64("nativeCommon"); linuxX64("nativePosix") }
            }
        }

        sourceSets {
            fun dependants(name: String, on: Set<String>) {
                val main = maybeCreate("${name}Main")
                val test = maybeCreate("${name}Test")
                for (o in on) {
                    maybeCreate("${o}Main").dependsOn(main)
                    maybeCreate("${o}Test").dependsOn(test)
                }
            }

            val none = setOf<String>()
            val android = if (hasAndroid) setOf() else setOf("android")
            val jvm = setOf("jvm")
            val js = setOf("js")
			val ios = setOf("iosX64", "iosArm32", "iosArm64")
            val macos = setOf("macosX64")
            val linux = setOf("linuxX64")
            val mingw = setOf("mingwX64")
			val apple = ios + macos
            val allNative = apple + linux + mingw
            val jvmAndroid = jvm + android
            val allTargets = allNative + js + jvm + android

            dependants("iosCommon", ios)
            dependants("nativeCommon", allNative)
            dependants("nonNativeCommon", allTargets - allNative)
            dependants("nativePosix", allNative - mingw)
            dependants("nativePosixNonApple", allNative - mingw - apple)
            dependants("nativePosixApple", apple)
            dependants("nonJs", allTargets - js)
		}
    }

    dependencies {
        commonMainImplementation("org.jetbrains.kotlin:kotlin-stdlib-common")
        commonTestImplementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
        commonTestImplementation("org.jetbrains.kotlin:kotlin-test-common")

        if (hasAndroid) {
            add("androidMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib")
            add("androidTestImplementation", "org.jetbrains.kotlin:kotlin-test")
            add("androidTestImplementation", "org.jetbrains.kotlin:kotlin-test-junit")
        }

        add("jsMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib-js")
        add("jsTestImplementation", "org.jetbrains.kotlin:kotlin-test-js")

        add("jvmMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        add("jvmTestImplementation", "org.jetbrains.kotlin:kotlin-test")
        add("jvmTestImplementation", "org.jetbrains.kotlin:kotlin-test-junit")
    }

    // Javascript test configuration
    val korlibsDir = File(System.getProperty("user.home"), ".korlibs").apply { mkdirs() }

    run {
        extensions.getByType<NodeExtension>().apply {
            version = "8.11.4"
            download = true
            workDir = korlibsDir["nodejs"]
            npmWorkDir = korlibsDir["npm"]
            yarnWorkDir = korlibsDir["yarn"]
            nodeModulesDir = korlibsDir["node_modules"]
        }

        // Fix for https://github.com/srs/gradle-node-plugin/issues/301
        repositories.whenObjectAdded {
            if (this is IvyArtifactRepository) {
                metadataSources {
                    artifact()
                }
            }
        }

        // Small optimization
        tasks {
            this["nodeSetup"].onlyIf { !korlibsDir["nodejs"].exists() }
        }

    }

    val jsCompilations = kotlin.targets.js.compilations

    val installMocha = tasks.create<NpmTask>("installMocha") {
        onlyIf { !node.nodeModulesDir["mocha"].exists() }
        setArgs(listOf("install", "mocha@5.2.0"))
    }

    val populateNodeModules = tasks.create<Copy>("populateNodeModules") {
        afterEvaluate {
            from("${node.nodeModulesDir}")
            from(jsCompilations.main.output.allOutputs)
            from(jsCompilations.test.output.allOutputs)
            for (it in jsCompilations.test.runtimeDependencyFiles) {
                if (it.exists() && !it.isDirectory) {
                    from(zipTree(it.absolutePath).matching { include("*.js") })
                }
            }
            for (sourceSet in kotlin.sourceSets) {
                from(sourceSet.resources)
            }
            into("$buildDir/node_modules")
        }
    }

    val jsTestNode = tasks.create<NodeTask>("jsTestNode") {
        dependsOn(jsCompilations.test.compileKotlinTask, installMocha, populateNodeModules)

        val resultsFile = buildDir["node-results/results.json"]
        setScript(file("$buildDir/node_modules/mocha/bin/mocha"))
        setWorkingDir(file("$buildDir/node_modules"))
        setArgs(listOf("--timeout", "15000", "${project.name}_test.js", "-o", resultsFile))
        inputs.files(jsCompilations.test.compileKotlinTask.outputFile, jsCompilations.main.compileKotlinTask.outputFile)
        outputs.file(resultsFile)
    }

    val jsInstallMochaHeadlessChrome = tasks.create<NpmTask>("jsInstallMochaHeadlessChrome") {
        onlyIf { !node.nodeModulesDir["mocha-headless-chrome"].exists() }
        setArgs(listOf("install", "mocha-headless-chrome@2.0.1"))
    }

    val jsTestChrome = tasks.create<NodeTask>("jsTestChrome") {
        dependsOn(jsCompilations.test.compileKotlinTask, jsInstallMochaHeadlessChrome, installMocha, populateNodeModules)

        val resultsFile = buildDir["chrome-results/results.json"]
        doFirst {
            buildDir["node_modules/tests.html"].text = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Mocha Tests</title>
                    <meta charset="utf-8">
                    <link rel="stylesheet" href="mocha/mocha.css">
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/require.js/2.3.6/require.min.js"></script>
                </head>
                <body>
                <div id="mocha"></div>
                <script src="mocha/mocha.js"></script>
                <script>
                    requirejs.config({'baseUrl': '.', 'paths': { 'tests': '${project.name}_test' }});
                    mocha.setup('bdd');
                    require(['tests'], function() { mocha.run(); });
                </script>
                </body>
                </html>
            """.trimIndent()
        }
        setScript(node.nodeModulesDir["mocha-headless-chrome/bin/start"])
        setArgs(listOf("-f", "$buildDir/node_modules/tests.html", "-a", "no-sandbox", "-a", "disable-setuid-sandbox", "-a", "allow-file-access-from-files", "-o", resultsFile))
        inputs.files(jsCompilations.test.compileKotlinTask.outputFile, jsCompilations.main.compileKotlinTask.outputFile)
        outputs.file(resultsFile)
    }

    afterEvaluate {
        for (target in listOf("macosX64", "linuxX64", "mingwX64")) {
            val taskName = "copyResourcesToExecutable_$target"
            val targetTestTask = tasks.getByName("${target}Test") as Exec
            val compileTestTask = tasks.getByName("compileTestKotlin${target.capitalize()}")
            val compileMainask = tasks.getByName("compileKotlin${target.capitalize()}")

            tasks {
                create<Copy>(taskName) {
                    for (sourceSet in kotlin.sourceSets) {
                        from(sourceSet.resources)
                    }

                    into(File(targetTestTask.executable).parentFile)
                }
            }

            val reportFile = buildDir["test-results/nativeTest/text/output.txt"].apply { parentFile.mkdirs() }
            val fout = ByteArrayOutputStream()
            targetTestTask.standardOutput = MultiOutputStream(listOf(targetTestTask.standardOutput, fout))
            targetTestTask.doLast {
                reportFile.writeBytes(fout.toByteArray())
            }

            targetTestTask.inputs.files(
                *compileTestTask.outputs.files.files.toTypedArray(),
                *compileMainask.outputs.files.files.toTypedArray()
            )
            targetTestTask.outputs.file(reportFile)

            targetTestTask.dependsOn(taskName)
        }
    }

    // Include resources from JS and Metadata (common) into the JS JAR
    val jsJar = tasks.getByName<Jar>("jsJar")
    val jsTest = tasks.getByName<Test>("jsTest")

    for (target in listOf(kotlin.targets.js, kotlin.targets.metadata)) {
        for (sourceSet in target.compilations.main.kotlinSourceSets) {
            for (it in sourceSet.resources.srcDirs) {
                jsJar.from(it)
            }
        }
    }

    // Only run JS tests if not in windows
    if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
        jsTest.dependsOn(jsTestNode)

        // Except on travis (we have a separate target for it)
        if (System.getenv("TRAVIS") == null) {
            jsTest.dependsOn(jsTestChrome)
        }
    }

    group = "com.soywiz"
    version = properties["projectVersion"].toString()

    // Publishing
    val publishUser = (rootProject.findProperty("BINTRAY_USER") ?: project.findProperty("bintrayUser") ?: System.getenv("BINTRAY_USER"))?.toString()
    val publishPassword = (rootProject.findProperty("BINTRAY_KEY") ?: project.findProperty("bintrayApiKey") ?: System.getenv("BINTRAY_API_KEY"))?.toString()

    apply(plugin = "maven-publish")

    if (publishUser != null && publishPassword != null) {
        extensions.getByType<PublishingExtension>().apply {
            repositories {
                maven {
                    credentials {
                        username = publishUser
                        setPassword(publishPassword)
                    }
                    url = uri("https://api.bintray.com/maven/soywiz/soywiz/${project.property("project.package")}/")
                }
            }
            afterEvaluate {
                configure(publications) {
                    this as MavenPublication
                    pom.withXml {
                        this.asNode().apply {
                            appendNode("name", project.name)
                            appendNode("description", project.property("project.description"))
                            appendNode("url", project.property("project.scm.url"))
                            appendNode("licenses").apply {
                                appendNode("license").apply {
                                    appendNode("name").setValue(project.property("project.license.name"))
                                    appendNode("url").setValue(project.property("project.license.url"))
                                }
                            }
                            appendNode("scm").apply {
                                appendNode("url").setValue(project.property("project.scm.url"))
                            }

                            // Changes runtime -> compile in Android's AAR publications
                            if (pom.packaging == "aar") {
                                val nodes = this.getAt(QName("dependencies")).getAt("dependency").getAt("scope")
                                for (node in nodes) {
                                    (node as Node).setValue("compile")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Headless testing on JVM (so we can use GWT)
    tasks {
        "jvmTest"(Test::class) {
            jvmArgs = (jvmArgs ?: arrayListOf()) + arrayListOf("-Djava.awt.headless=true")
        }
    }
}

if (project.file("build.project.gradle.kts").exists()) {
	apply(from = project.file("build.project.gradle.kts"))
}

subprojects {
    val installJsCanvas = tasks.create<NpmTask>("installJsCanvas") {
        onlyIf { !File(node.nodeModulesDir, "canvas").exists() }
        setArgs(arrayListOf("install", "canvas@2.2.0"))
    }

    tasks.getByName("jsTestNode").dependsOn(installJsCanvas)
}

