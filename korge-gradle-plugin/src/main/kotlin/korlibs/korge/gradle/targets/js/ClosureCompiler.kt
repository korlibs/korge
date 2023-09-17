package korlibs.korge.gradle.targets.js

import com.google.javascript.jscomp.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import java.io.*


fun Project.configureClosureCompiler() {
    val jsBrowserEsbuildClosureCompiler = tasks.createThis<Task>("jsBrowserEsbuildClosureCompiler") {
        val browserReleaseEsbuild = tasks.getByName("browserReleaseEsbuild")
        val jsFile = browserReleaseEsbuild.outputs.files.first()
        val jsFileMap = File(jsFile.parentFile, jsFile.nameWithoutExtension + ".js.map")
        val jsBigFile = File(jsFile.parentFile, jsFile.nameWithoutExtension + ".big.js")
        val jsBigFileMap = File(jsFile.parentFile, jsFile.nameWithoutExtension + ".big.js.map")
        group = "kotlin browser"
        dependsOn(browserReleaseEsbuild)
        inputs.file(jsFile)
        //task.outputs.file(jsMinFile)
        outputs.file(jsFile)
        doFirst {
            jsBigFile.writeBytes(jsFile.readBytes())
            jsFile.delete()
            if (jsFileMap.exists()) {
                jsBigFileMap.writeBytes(jsFileMap.readBytes())
                jsFileMap.delete()
            }
            compileJs(jsBigFile, jsFile, jsBigFileMap.exists())
            jsBigFile.delete()
            jsBigFileMap.delete()
            //jsFile.writeText(compileJs(jsFile.readText()) ?: error("Can't compile JS file due to an error"))
        }
    }

    tasks.createThis<Task>("packageJsClosureCompiler") {
        dependsOn("jsBrowserEsbuildClosureCompiler")
        group = "package"
    }
}


private fun compileJs(input: File, output: File, useSourceMap: Boolean) {
    val runner = object : CommandLineRunner(ArrayList<String>().apply {
        add("--warning_level"); add("QUIET")
        add("--js"); add("$input")
        if (useSourceMap) {
            add("--create_source_map"); add("$output.map")
            add("--apply_input_source_maps")
        }
        add("--js_output_file"); add("$output")
    }.toTypedArray()) {
        override fun createOptions(): CompilerOptions {
            return super.createOptions().also {
                it.setErrorHandler { level, error ->
                    println("$level:$error")
                }
            }
        }

        override fun setRunOptions(options: CompilerOptions?) {
            super.setRunOptions(options)
        }
    }
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
