package org.korge.e2e

import korlibs.korge.view.Container

object EmptyE2ETestCase : E2ETestCase() {
    override val pixelPerfect: Boolean = true

    override suspend fun Container.run() {
    }
}
