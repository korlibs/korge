package korlibs.korge.kotlincompiler

import java.io.File

object KorgeKotlinCompilerCLI {
    @JvmStatic
    fun main(args: Array<String>) {
        for (line in System.`in`.bufferedReader().lineSequence()) {
            val command = line.substringBefore(' ')
            val params = line.substringAfter(' ', "")
            when (command) {
                "listen" -> {
                    val socketFile = File("$params.socket")
                    val pidFile = File("$params.pid")
                    val currentPid = ProcessHandle.current().pid()
                    pidFile.writeText("$currentPid")
                    //println("Listening on $currentPid")
                    TODO()
                }
                "exit" -> {
                    System.exit(0)
                }
                "compile" -> {
                }
                "run" -> {
                }
                "stop" -> {
                }
                "package:jvm" -> {
                }
                //"package:js" -> {
                //}
                //"package:wasm" -> {
                //}
                //"package:ios" -> {
                //}
                //"package:android" -> {
                //}
            }
        }
    }
}
