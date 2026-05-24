package org.korge.e2e

import korlibs.io.lang.portableSimpleName
import korlibs.korge.view.Container
import korlibs.korge.view.Stage

open class E2ETestCase {
    val name get() = this::class.portableSimpleName.removeSuffix("E2ETestCase")
    open val scale: Double = 1.0
    open val pixelPerfect: Boolean = false

    open suspend fun run(stage: Stage) {
        stage.run()
    }

    open suspend fun Container.run() {
    }
}
