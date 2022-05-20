package com.soywiz.korge.gradle

import com.soywiz.korge.gradle.targets.KorgeIconProvider
import kotlin.test.Test

class ResourcesTest {
    @Test
    fun test() {
        KorgeIconProvider().getIconBytes()
    }
}
