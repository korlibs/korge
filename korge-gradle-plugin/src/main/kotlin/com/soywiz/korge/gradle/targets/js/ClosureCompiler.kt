package com.soywiz.korge.gradle.targets.js

import com.google.javascript.jscomp.*
import org.gradle.api.*
import java.io.*


fun Project.configureClosureCompiler() {
    val jsBrowserEsbuildClosureCompiler = tasks.create("jsBrowserEsbuildClosureCompiler") {
        val browserReleaseEsbuild = tasks.getByName("browserReleaseEsbuild")
        val jsFile = browserReleaseEsbuild.outputs.files.first()
        val jsMinFile = File(jsFile.parentFile, jsFile.nameWithoutExtension + ".min.js")
        group = "kotlin browser"
        dependsOn(browserReleaseEsbuild)
        inputs.file(jsFile)
        //task.outputs.file(jsMinFile)
        outputs.file(jsFile)
        doFirst {
            compileJs(jsFile, jsFile)
            //compileJs(jsFile, jsMinFile)
            //jsFile.writeText(compileJs(jsFile.readText()) ?: error("Can't compile JS file due to an error"))
        }
    }

    tasks.create("packageJsClosureCompiler") {
        dependsOn("jsBrowserEsbuildClosureCompiler")
        group = "package"
    }
}


private fun compileJs(input: File, output: File) {
    val runner = object : CommandLineRunner(arrayOf("--warning_level", "QUIET", "--js", input.toString(), "--js_output_file", output.toString())) { }
    runner.setExitCodeReceiver {
        if (it != 0) {
            error("Error compiling JS")
        }
        null
    }
    if (runner.shouldRunCompiler()) {
        runner.run()
    }
}

/*
private fun compileJs(code: String?): String? = Compiler().also { compiler ->
    //println(AbstractCommandLineRunner.getBuiltinExterns(CompilerOptions.Environment.BROWSER))
    val bytes = AbstractCommandLineRunner::class.java.getResource("/externs.zip")!!.readBytes()
    val files = LinkedHashMap<String, SourceFile>()
    ZipInputStream(bytes.inputStream()).use { zis ->
        while (true) {
            val ze = zis.nextEntry ?: break
            try {
                val fileName = File(ze.name).name
                files[fileName] = SourceFile.fromCode(fileName, zis.readAllBytes().toString(Charsets.UTF_8))
            } finally {
                zis.closeEntry()
            }
        }
    }
    println("files = $files")
    val externFiles = DefaultExterns.prepareExterns(CompilerOptions.Environment.BROWSER, files)
    println("externFiles = $externFiles")

    compiler.compile(
        externFiles,
        listOf(SourceFile.fromCode("input.js", code)),
        CompilerOptions().also {
            it.setErrorHandler { level, error ->
                if (level == CheckLevel.ERROR) {
                    System.err.println(error)
                }
            }
            CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel(it)
        }
    )
}.toSource()
*/
