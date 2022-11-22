package com.soywiz.korio.file.std

import com.soywiz.korio.file.PathInfo
import com.soywiz.korio.file.parent
import com.soywiz.korio.lang.Environment
import com.soywiz.korio.lang.tempPath

/**
 * Contain standard paths to different parts of the operating system.
 */
expect object StandardPaths : StandardPathsBase

interface StandardPathsBase {
    /**
     * Gets the absolute executable path that is running.
     */
    val executableFile: String get() = "$cwd/executable"

    /**
     * Gets the folder where is the executable that is running.
     * In the case of JS, it will `.` and in the case of the JVM it will be where the `jar` is located.
     **/
    val executableFolder: String get() = PathInfo(executableFile).parent.fullPath

    /**
     * Gets the executable folder, or the resources folder when running in a container/package
     * that has the resources in a different path, for example in a macos .app.
     */
    val resourcesFolder: String get() = executableFolder

    /**
     * Current Working Directory where the application was launched.
     * Might be different to where the executable is if the application was launched in a different directory.
     */
    val cwd: String get() = "."

    /**
     * Home directory of the current user.
     *
     * Typically:
     * - linux: /home/user
     * - windows: /Users/user
     * - macos: /Users/user
     */
    val userHome: String get() = Environment["~"] ?: "/tmp"

    /**
     * Temp folder where to store temporal files that might be discarded anytime.
     *
     * Typically (or equivalent):
     * - /tmp
     */
    val temp: String get() = Environment.tempPath
}
