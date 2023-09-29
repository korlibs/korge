package com.soywiz.kproject.util

import java.io.*

fun normalizePath(path: String): String = File(path).normalize().toString().replace("\\", "/")
fun ensureRepo(repo: String): String = normalizePath(repo).also { if (it.count { it == '/' } != 1) error("Invalid repo '$repo'") }
fun getKProjectDir(): File = File("${System.getProperty("user.home")}/.kproject").also { it.mkdirs() }
operator fun File.get(path: String): File = File(this, path)
fun File.execToString(vararg params: String, throwOnError: Boolean = true): String {
    val result = Runtime.getRuntime().exec(params, arrayOf(), this)
    val stdout = result.inputStream.readBytes().toString(Charsets.UTF_8)
    val stderr = result.errorStream.readBytes().toString(Charsets.UTF_8)
    if (result.exitValue() != 0 && throwOnError) error("$stdout$stderr")
    return stdout + stderr
}

