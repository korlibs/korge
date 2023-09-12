package org.korge.integrations.androidcompose.korge

import korlibs.image.color.*
import korlibs.korge.scene.*
import korlibs.korge.view.*

class MainKorgeScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        solidRect(100, 100, Colors.RED)
    }
}
