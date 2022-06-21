package com.soywiz.korge.gradle.targets.jvm

import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.Charset

object JvmTools {
    /**
     * If the provided class has been loaded from a jar file that is on the local file system, will find the absolute path to that jar file.
     *
     * @param context The jar file that contained the class file that represents this class will be found. Specify `null` to let `LiveInjector`
     * find its own jar.
     * @throws IllegalStateException If the specified class was loaded from a directory or in some other way (such as via HTTP, from a database, or some
     * other custom classloading device).
     */
    @Throws(IllegalStateException::class)
    fun findPathJar(context: Class<*>): String? {
        val path = context.getProtectionDomain().getCodeSource().getLocation().getPath()
        if (path != null) return path
        val rawName = context.getName()
        var classFileName: String
        /* rawName is something like package.name.ContainingClass$ClassName. We need to turn this into ContainingClass$ClassName.class. */run {
            val idx: Int = rawName.lastIndexOf('.')
            classFileName = (if (idx == -1) rawName else rawName.substring(idx + 1)) + ".class"
        }
        val uri = context.getResource(classFileName).toString()
        if (uri.startsWith("file:")) throw IllegalStateException("This class has been loaded from a directory and not from a jar file.")
        if (!uri.startsWith("jar:file:")) {
            val idx = uri.indexOf(':')
            val protocol = if (idx == -1) "(unknown)" else uri.substring(0, idx)
            throw IllegalStateException(
                "This class has been loaded remotely via the " + protocol +
                    " protocol. Only loading from a jar on the local file system is supported."
            )
        }
        val idx = uri.indexOf('!')
        //As far as I know, the if statement below can't ever trigger, so it's more of a sanity check thing.
        if (idx == -1) throw IllegalStateException("You appear to have loaded this class from a local jar file, but I can't make sense of the URL!")
        try {
            val fileName: String =
                URLDecoder.decode(uri.substring("jar:file:".length, idx), Charset.defaultCharset().name())
            return File(fileName).getAbsolutePath()
        } catch (e: UnsupportedEncodingException) {
            throw InternalError("default charset doesn't exist. Your VM is borked.")
        }
    }
}
