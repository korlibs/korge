package org.korge.application

import korlibs.image.color.*
import korlibs.korge.*
import korlibs.korge.scene.*

private val studioBackground = Colors["#10131B"]

suspend fun main() = Korge(
    windowSize = Korge.DEFAULT_WINDOW_SIZE,
    backgroundColor = studioBackground,
    displayMode = KorgeDisplayMode.CENTER_NO_CLIP,
    debug = false,
    multithreaded = true,
) {
    sceneContainer().changeTo({ AnimoraStudioScene() })
}
