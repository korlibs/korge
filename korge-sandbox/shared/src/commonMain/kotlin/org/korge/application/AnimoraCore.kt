package org.korge.application

/** Product-neutral animation domain contracts. UI and render backends depend on these small types. */
data class AnimoraProject(
    val name: String,
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 24,
    val scenes: List<AnimoraSceneModel> = listOf(AnimoraSceneModel("Opening")),
)

data class AnimoraSceneModel(
    val name: String,
    val frameCount: Int = 96,
    val layers: List<AnimoraLayerModel> = listOf(
        AnimoraLayerModel("Character", AnimoraLayerType.PAINT),
        AnimoraLayerModel("Color", AnimoraLayerType.PAINT),
        AnimoraLayerModel("Camera", AnimoraLayerType.CAMERA),
    ),
)

data class AnimoraLayerModel(val name: String, val type: AnimoraLayerType, val visible: Boolean = true)
enum class AnimoraLayerType { PAINT, VECTOR, GROUP, CAMERA, AUDIO, TEXT, GUIDE }
enum class AnimoraTool { PENCIL, INK, BRUSH, ERASER, FILL, TRANSFORM, SELECT, TEXT }

data class AnimoraFrame(val index: Int, val exposure: Int = 1, val keyframe: Boolean = true)

interface ProjectStore {
    suspend fun save(project: AnimoraProject)
    suspend fun load(name: String): AnimoraProject?
}

interface RenderBackend {
    fun invalidateRegion(left: Int, top: Int, right: Int, bottom: Int)
    fun clearFrameCache()
}

class TimelineState(val totalFrames: Int = 96) {
    var currentFrame: Int = 1
        private set
    var isPlaying: Boolean = false
        private set
    fun setFrame(frame: Int) { currentFrame = frame.coerceIn(1, totalFrames) }
    fun togglePlayback() { isPlaying = !isPlaying }
}
