package com.soywiz.korge3d

import com.soywiz.korge3d.format.readColladaLibrary
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import doIOTest
import kotlin.test.Test

class Library3DTest {
    @Test
    fun test() = suspendTest({ doIOTest }) {
        val library = resourcesVfs["scene.dae"].readColladaLibrary()
        println(library)
    }
}
