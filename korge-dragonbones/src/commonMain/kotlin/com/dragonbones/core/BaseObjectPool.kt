package com.dragonbones.core

import com.dragonbones.animation.*
import com.dragonbones.armature.*
import com.dragonbones.event.*
import com.dragonbones.geom.*
import com.dragonbones.model.*
import com.dragonbones.util.*
import com.soywiz.korio.lang.*
import kotlin.reflect.*

class BaseObjectPool {
	val DEFAULT_COLOR: ColorTransform = ColorTransform()

	internal val _helpMatrix: Matrix = Matrix()
	internal val _helpTransform: Transform = Transform()
	internal val _helpPoint: Point = Point()

	internal var __hashCode: Int = 0
	private var _defaultMaxCount: Int = 3000
	private val _maxCountMap: LinkedHashMap<KClass<*>, Int> = LinkedHashMap()
	private val _poolsMap: LinkedHashMap<KClass<*>, ArrayList<BaseObject>> = LinkedHashMap()
	private val factories = LinkedHashMap<KClass<*>, () -> BaseObject>()

	internal fun <T : BaseObject> _returnObject(obj: T) {
		val classType = obj::class
		val maxCount = _maxCountMap[classType] ?: _defaultMaxCount
		val pool = getPool(classType)
		if (pool.length < maxCount) {
			if (!obj._isInPool) {
				obj._isInPool = true
				(pool as ArrayList<BaseObject>).add(obj)
			} else {
				console.warn("The object is already in the pool.")
			}
		} else {
		}
	}

	internal fun <T : BaseObject> getPool(clazz: KClass<T>): ArrayList<T> {
		return _poolsMap.getOrPut(clazz) { arrayListOf() } as ArrayList<T>
	}

	/**
	 * - Set the maximum cache count of the specify object pool.
	 * @param objectConstructor - The specify class. (Set all object pools max cache count if not set)
	 * @param maxCount - Max count.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 设置特定对象池的最大缓存数量。
	 * @param objectConstructor - 特定的类。 (不设置则设置所有对象池的最大缓存数量)
	 * @param maxCount - 最大缓存数量。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun setMaxCount(clazz: KClass<out BaseObject>, maxCount: Int) {
		_maxCountMap[clazz] = maxCount
		val pool = getPool(clazz)
		pool.lengthSet = maxCount

		//var maxCount = maxCount
		//if (maxCount < 0 || maxCount != maxCount) { // isNaN
		//	maxCount = 0
		//}
//
		//if (objectConstructor != null) {
		//	val classType = String(objectConstructor)
		//	val pool = if (classType in BaseObject._poolsMap) BaseObject._poolsMap[classType] else null
		//	if (pool != null && pool.length > maxCount) {
		//		pool.length = maxCount
		//	}
//
		//	BaseObject._maxCountMap[classType] = maxCount
		//}
		//else {
		//	BaseObject._defaultMaxCount = maxCount
//
		//	for (classType in BaseObject._poolsMap) {
		//		val pool = BaseObject._poolsMap[classType]!!
		//		if (pool.length > maxCount) {
		//			pool.length = maxCount
		//		}
//
		//		if (classType in BaseObject._maxCountMap) {
		//			BaseObject._maxCountMap[classType] = maxCount
		//		}
		//	}
		//}
	}
	/**
	 * - Clear the cached instances of a specify object pool.
	 * @param objectConstructor - Specify class. (Clear all cached instances if not set)
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 清除特定对象池的缓存实例。
	 * @param objectConstructor - 特定的类。 (不设置则清除所有缓存的实例)
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun <T : BaseObject> clearPool(clazz: KClass<T>): Unit {
		getPool(clazz).clear()
		//if (objectConstructor != null) {
		//	val classType = String(objectConstructor)
		//	val pool = if (classType in BaseObject._poolsMap) BaseObject._poolsMap[classType] else null
		//	if (pool != null && pool.size > 0) {
		//		pool.lengthSet = 0
		//	}
		//}
		//else {
		//	for (k in BaseObject._poolsMap) {
		//		val pool = BaseObject._poolsMap[k]
		//		pool.length = 0
		//	}
		//}
	}
	/**
	 * - Get an instance of the specify class from object pool.
	 * @param objectConstructor - The specify class.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 从对象池中获取特定类的实例。
	 * @param objectConstructor - 特定的类。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun <T : BaseObject> borrowObject(clazz: KClass<T>): T {
		val pool = getPool(clazz)
		val obj = if (pool.isNotEmpty()) pool.removeAt(pool.size - 1) else createInstance(clazz)
		obj._onClear()
		obj._isInPool = false
		return obj
	}

	inline fun <reified T : BaseObject> borrowObject(): T = borrowObject(T::class)

	@Suppress("UNCHECKED_CAST")
	private fun <T : BaseObject> createInstance(clazz: KClass<T>): T {
		val factory = factories[clazz] ?: TODO("Missing createInstance ${clazz.portableSimpleName}")
		return factory() as T
	}

	fun <T : BaseObject> register(clazz: KClass<T>, factory: () -> T) = run {
		factories[clazz] = factory
	}

	inline fun <reified T : BaseObject> register(noinline factory: () -> T) = register(T::class, factory)

	val pool = this

	init {
		register { Animation(pool) }
		register { EventObject(pool) }
		register { DisplayFrame(pool) }
		register { AnimationConfig(pool) }
		register { BlendState(pool) }
		register { IKConstraintTimelineState(pool) }
		register { BoneAllTimelineState(pool) }
		register { BoneTranslateTimelineState(pool) }
		register { BoneRotateTimelineState(pool) }
		register { BoneScaleTimelineState(pool) }
		register { SlotDisplayTimelineState(pool) }
		register { SlotZIndexTimelineState(pool) }
		register { SlotColorTimelineState(pool) }
		register { DeformTimelineState(pool) }
		register { AlphaTimelineState(pool) }
		register { ZOrderTimelineState(pool) }
		register { SurfaceTimelineState(pool) }
		register { AnimationProgressTimelineState(pool) }
		register { AnimationWeightTimelineState(pool) }
		register { AnimationParametersTimelineState(pool) }
		register { ArmatureData(pool) }
		register { CanvasData(pool) }
		register { BoneData(pool) }
		register { SurfaceData(pool) }
		register { Surface(pool) }
		register { Bone(pool) }
		register { IKConstraint(pool) }
		register { PathConstraint(pool) }
		register { IKConstraintData(pool) }
		register { PathConstraintData(pool) }
		register { SlotData(pool) }
		register { SkinData(pool) }
		register { ImageDisplayData(pool) }
		register { ArmatureDisplayData(pool) }
		register { MeshDisplayData(pool) }
		register { BoundingBoxDisplayData(pool) }
		register { PathDisplayData(pool) }
		register { RectangleBoundingBoxData(pool) }
		register { EllipseBoundingBoxData(pool) }
		register { PolygonBoundingBoxData(pool) }
		register { AnimationData(pool) }
		register { AnimationTimelineData(pool) }
		register { TimelineData(pool) }
		register { ActionData(pool) }
		register { UserData(pool) }
		register { WeightData(pool) }
		register { DragonBonesData(pool) }
		register { Armature(pool) }
		register { AnimationState(pool) }
		register { ActionTimelineState(pool) }
	}
}