package com.soywiz.korge3d

import com.soywiz.korge3d.format.*
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
