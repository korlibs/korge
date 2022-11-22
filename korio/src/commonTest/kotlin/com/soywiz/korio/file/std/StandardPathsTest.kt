package com.soywiz.korio.file.std

import kotlin.test.Test

class StandardPathsTest {
    @Test
    fun test() {
        println("StandardPathsTest:")
        println("cwd=${StandardPaths.cwd}")
        println("executableFile=${StandardPaths.executableFile}")
        println("executableFolder=${StandardPaths.executableFolder}")
        println("resourcesFolder=${StandardPaths.resourcesFolder}")
        println("temp=${StandardPaths.temp}")
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
