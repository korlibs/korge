package com.soywiz.korge.view.ktree

import com.soywiz.korge.tests.*
import com.soywiz.korge.view.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class KTreeTest : ViewsForTesting() {
    @Test
    fun test() = viewsTest {
        val ktree = resourcesVfs["restitution.ktree"].readKTree()
        assertIs<Container>(ktree)
        val child = ktree.children[1]
        assertIs<Ellipse>(child)
        assertEquals(Anchor.CENTER, Anchor(child.anchorX, child.anchorY))
    }
}
