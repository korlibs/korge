/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2012-2018 DragonBones team and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.dragonbones.core

/**
 * - The BaseObject is the base class for all objects in the DragonBones framework.
 * All BaseObject instances are cached to the object pool to reduce the performance consumption of frequent requests for memory or memory recovery.
 * @version DragonBones 4.5
 * @language en_US
 */
/**
 * - 基础对象，通常 DragonBones 的对象都继承自该类。
 * 所有基础对象的实例都会缓存到对象池，以减少频繁申请内存或内存回收的性能消耗。
 * @version DragonBones 4.5
 * @language zh_CN
 */
abstract class BaseObject(val sopool: SingleObjectPool<out BaseObject>) {
    val pool = sopool.base
    private val ipool = sopool as SingleObjectPool<BaseObject>

	override fun toString(): String = "BaseObject.Unknown"

	/**
	 * - A unique identification number assigned to the object.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 分配给此实例的唯一标识号。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	val _hashCode: Int = pool.__hashCode++ // @TODO: Kotlin.JS hashCode produces a compiler error in JS
	internal var _isInPool: Boolean = false

	internal abstract fun _onClear(): Unit
	/**
	 * - Clear the object and return it back to object pool。
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 清除该实例的所有数据并将其返还对象池。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun returnToPool() {
		this._onClear()
        ipool.returnObject(this)
	}
}
