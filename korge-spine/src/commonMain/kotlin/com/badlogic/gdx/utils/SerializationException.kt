/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.badlogic.gdx.utils

/** Indicates an error during serialization due to misconfiguration or during deserialization due to invalid input data.
 * @author Nathan Sweet
 */
class SerializationException : RuntimeException {
    private var trace: StringBuilder? = null

    constructor(message: String, cause: Throwable?) : super(message, cause) {}

    constructor(message: String) : super(message) {}

    override val message: String?
        get() {
            if (trace == null) return super.message
            val sb = StringBuilder(512)
            sb.append(super.message)
            if (sb.isNotEmpty()) sb.append('\n')
            sb.append("Serialization trace:")
            sb.append(trace)
            return sb.toString()
        }
}
