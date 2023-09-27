package korlibs.io.file.std

import korlibs.logger.*
import korlibs.io.lang.*
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

    @Test
    fun test2() {
        // /Users/soywiz/Library/Developer/CoreSimulator/Devices/5FF29F5D-2E43-4994-991B-A8C4EC3F1677/data/Documents
        // /Users/soywiz/Library/Developer/CoreSimulator/Devices/5FF29F5D-2E43-4994-991B-A8C4EC3F1677/data/Library/Preferences/myappid
        println(StandardPaths.appPreferencesFolder("myappid"))
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
