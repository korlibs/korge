package com.soywiz.korio.file.std

import com.soywiz.klogger.*
import com.soywiz.korio.lang.*
import kotlin.test.Test

class StandardPathsTest {
    val logger = Logger(this::class.portableSimpleName)

    @Test
    fun test() {
        val cwd = StandardPaths.cwd
        val executableFile = StandardPaths.executableFile
        val executableFolder = StandardPaths.executableFolder
        val resourcesFolder = StandardPaths.resourcesFolder
        val temp = StandardPaths.temp
        logger.debug { "StandardPathsTest:" }
        logger.debug { "cwd=${cwd}" }
        logger.debug { "executableFile=${executableFile}" }
        logger.debug { "executableFolder=${executableFolder}" }
        logger.debug { "resourcesFolder=${resourcesFolder}" }
        logger.debug { "temp=${temp}" }
    }

    /*
    JVM:
        cwd=/Users/soywiz/projects/korge/korio
        executableFile=/Users/soywiz/projects/korge/korio/executable
        executableFolder=/Users/soywiz/projects/korge/korio
        resourcesFolder=/Users/soywiz/projects/korge/korio
        temp=/var/folders/pj/8m044gt95nzd5ny92667g8t40000gn/T/

    NODEJS:

        StandardPathsTest:
        cwd=/Users/soywiz/projects/korge/build/js/packages/korlibs-korio-test
        executableFile=/Users/soywiz/projects/korge/build/js/packages/korlibs-korio-test/executable
        executableFolder=/Users/soywiz/projects/korge/build/js/packages/korlibs-korio-test
        resourcesFolder=/Users/soywiz/projects/korge/build/js/packages/korlibs-korio-test
        temp=/var/folders/pj/8m044gt95nzd5ny92667g8t40000gn/T/

    MACOS:
        StandardPathsTest:
        cwd=/Users/soywiz/projects/korge/korio
        executableFile=/Users/soywiz/projects/korge/korio/build/bin/macosArm64/debugTest/test.kexe
        executableFolder=/Users/soywiz/projects/korge/korio/build/bin/macosArm64/debugTest
        resourcesFolder=/Users/soywiz/projects/korge/korio/build/bin/macosArm64/debugTest
        temp=/var/folders/pj/8m044gt95nzd5ny92667g8t40000gn/T/
    */
}
