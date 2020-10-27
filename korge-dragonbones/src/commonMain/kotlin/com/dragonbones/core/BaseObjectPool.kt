package com.dragonbones.core

import com.dragonbones.animation.*
import com.dragonbones.armature.*
import com.dragonbones.event.*
import com.dragonbones.model.*
import com.soywiz.kds.*
import com.soywiz.korge.dragonbones.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

class SingleObjectPool<T : BaseObject>(val base: BaseObjectPool, gen: (SingleObjectPool<T>) -> T) {
    private val pool = Pool { gen(this) }

    fun returnObject(obj: T) {
        pool.free(obj)
    }

    fun borrow(): T = pool.alloc().also {
        it._onClear()
        it._isInPool = false
    }
}

class BaseObjectPool {
    var __hashCode = 0
    internal val _helpMatrix: Matrix = Matrix()
    internal val _helpTransform: TransformDb = TransformDb()
    internal val _helpPoint: Point = Point()

	val DEFAULT_COLOR: ColorTransform = ColorTransform()

    fun <T : BaseObject> objPool(gen: (SingleObjectPool<T>) -> T) = SingleObjectPool(this, gen)

    val animation = objPool<Animation> { Animation(it) }
    val eventObject = objPool<EventObject> { EventObject(it) }
    val displayFrame = objPool<DisplayFrame> { DisplayFrame(it) }
    val animationConfig = objPool<AnimationConfig> { AnimationConfig(it) }
    val blendState = objPool<BlendState> { BlendState(it) }
    val iKConstraintTimelineState = objPool<IKConstraintTimelineState> { IKConstraintTimelineState(it) }
    val boneAllTimelineState = objPool<BoneAllTimelineState> { BoneAllTimelineState(it) }
    val boneTranslateTimelineState = objPool<BoneTranslateTimelineState> { BoneTranslateTimelineState(it) }
    val boneRotateTimelineState = objPool<BoneRotateTimelineState> { BoneRotateTimelineState(it) }
    val boneScaleTimelineState = objPool<BoneScaleTimelineState> { BoneScaleTimelineState(it) }
    val slotDisplayTimelineState = objPool<SlotDisplayTimelineState> { SlotDisplayTimelineState(it) }
    val slotZIndexTimelineState = objPool<SlotZIndexTimelineState> { SlotZIndexTimelineState(it) }
    val slotColorTimelineState = objPool<SlotColorTimelineState> { SlotColorTimelineState(it) }
    val deformTimelineState = objPool<DeformTimelineState> { DeformTimelineState(it) }
    val alphaTimelineState = objPool<AlphaTimelineState> { AlphaTimelineState(it) }
    val zOrderTimelineState = objPool<ZOrderTimelineState> { ZOrderTimelineState(it) }
    val surfaceTimelineState = objPool<SurfaceTimelineState> { SurfaceTimelineState(it) }
    val animationProgressTimelineState = objPool<AnimationProgressTimelineState> { AnimationProgressTimelineState(it) }
    val animationWeightTimelineState = objPool<AnimationWeightTimelineState> { AnimationWeightTimelineState(it) }
    val animationParametersTimelineState = objPool<AnimationParametersTimelineState> { AnimationParametersTimelineState(it) }
    val armatureData = objPool<ArmatureData> { ArmatureData(it) }
    val canvasData = objPool<CanvasData> { CanvasData(it) }
    val boneData = objPool<BoneData> { BoneData(it) }
    val surfaceData = objPool<SurfaceData> { SurfaceData(it) }
    val surface = objPool<Surface> { Surface(it) }
    val bone = objPool<Bone> { Bone(it) }
    val iKConstraint = objPool<IKConstraint> { IKConstraint(it) }
    val pathConstraint = objPool<PathConstraint> { PathConstraint(it) }
    val iKConstraintData = objPool<IKConstraintData> { IKConstraintData(it) }
    val pathConstraintData = objPool<PathConstraintData> { PathConstraintData(it) }
    val slotData = objPool<SlotData> { SlotData(it) }
    val skinData = objPool<SkinData> { SkinData(it) }
    val imageDisplayData = objPool<ImageDisplayData> { ImageDisplayData(it) }
    val armatureDisplayData = objPool<ArmatureDisplayData> { ArmatureDisplayData(it) }
    val meshDisplayData = objPool<MeshDisplayData> { MeshDisplayData(it) }
    val boundingBoxDisplayData = objPool<BoundingBoxDisplayData> { BoundingBoxDisplayData(it) }
    val pathDisplayData = objPool<PathDisplayData> { PathDisplayData(it) }
    val rectangleBoundingBoxData = objPool<RectangleBoundingBoxData> { RectangleBoundingBoxData(it) }
    val ellipseBoundingBoxData = objPool<EllipseBoundingBoxData> { EllipseBoundingBoxData(it) }
    val polygonBoundingBoxData = objPool<PolygonBoundingBoxData> { PolygonBoundingBoxData(it) }
    val animationData = objPool<AnimationData> { AnimationData(it) }
    val animationTimelineData = objPool<AnimationTimelineData> { AnimationTimelineData(it) }
    val timelineData = objPool<TimelineData> { TimelineData(it) }
    val actionData = objPool<ActionData> { ActionData(it) }
    val userData = objPool<UserData> { UserData(it) }
    val weightData = objPool<WeightData> { WeightData(it) }
    val dragonBonesData = objPool<DragonBonesData> { DragonBonesData(it) }
    val armature = objPool<Armature> { Armature(it) }
    val animationState = objPool<AnimationState> { AnimationState(it) }
    val actionTimelineState = objPool<ActionTimelineState> { ActionTimelineState(it) }

    // Implementation pools
    val textureAtlasData = objPool<KorgeDbTextureAtlasData> { KorgeDbTextureAtlasData(it) }
    val textureData = objPool<KorgeDbTextureData> { KorgeDbTextureData(it) }
    val slot = objPool<KorgeDbSlot> { KorgeDbSlot(it) }
}
