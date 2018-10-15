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
package com.dragonbones.armature

import com.dragonbones.animation.*
import com.dragonbones.event.*

/**
 * - The armature proxy interface, the docking engine needs to implement it concretely.
 * @see dragonBones.Armature
 * @version DragonBones 5.0
 * @language en_US
 */
/**
 * - 骨架代理接口，对接的引擎需要对其进行具体实现。
 * @see dragonBones.Armature
 * @version DragonBones 5.0
 * @language zh_CN
 */
interface IArmatureProxy : IEventDispatcher {
	/**
	 * @internal
	 */
	fun dbInit(armature: Armature)

	/**
	 * @internal
	 */
	fun dbClear()

	/**
	 * @internal
	 */
	fun dbUpdate()
	/**
	 * - Dispose the instance and the Armature instance. (The Armature instance will return to the object pool)
	 * @example
	 * <pre>
	 *     removeChild(armatureDisplay);
	 *     armatureDisplay.dispose();
	 * </pre>
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 释放该实例和骨架。 （骨架会回收到对象池）
	 * @example
	 * <pre>
	 *     removeChild(armatureDisplay);
	 *     armatureDisplay.dispose();
	 * </pre>
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun dispose(disposeProxy: Boolean)
	/**
	 * - The armature.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 骨架。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	val armature: Armature
	/**
	 * - The animation player.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画播放器。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val animation: Animation
}
