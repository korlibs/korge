package com.soywiz.korge3d.experimental

import com.soywiz.korge3d.experimental.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class Library3DTest {
    @Test
    fun test() = suspendTest {
        val library = resourcesVfs["scene.dae"].readColladaLibrary()
        println(library)
    }
}
