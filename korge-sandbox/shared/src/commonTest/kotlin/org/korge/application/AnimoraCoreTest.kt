package org.korge.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimoraCoreTest {
    @Test
    fun timelineClampsFramesAndTogglesPlayback() {
        val timeline = TimelineState(totalFrames = 12)
        timeline.setFrame(999)
        assertEquals(12, timeline.currentFrame)
        timeline.setFrame(-2)
        assertEquals(1, timeline.currentFrame)
        assertFalse(timeline.isPlaying)
        timeline.togglePlayback()
        assertTrue(timeline.isPlaying)
    }

    @Test
    fun defaultProjectContainsSafeAnimationLayers() {
        val project = AnimoraProject("Test")
        assertEquals(24, project.fps)
        assertEquals(3, project.scenes.first().layers.size)
        assertEquals(AnimoraLayerType.CAMERA, project.scenes.first().layers.last().type)
    }
}
