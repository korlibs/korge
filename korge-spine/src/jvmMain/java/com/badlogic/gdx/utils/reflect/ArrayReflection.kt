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

package com.badlogic.gdx.utils.reflect

/** Utilities for Array reflection.
 * @author nexsoftware
 */
object ArrayReflection {

    /** Creates a new array with the specified component type and length.  */
    @JvmStatic
    fun newInstance(c: Class<*>, size: Int): Any {
        return java.lang.reflect.Array.newInstance(c, size)
    }

    /** Returns the length of the supplied array.  */
    @JvmStatic
    fun getLength(array: Any): Int {
        return java.lang.reflect.Array.getLength(array)
    }

    /** Returns the value of the indexed component in the supplied array.  */
    @JvmStatic
    operator fun get(array: Any, index: Int): Any {
        return java.lang.reflect.Array.get(array, index)
    }

    /** Sets the value of the indexed component in the supplied array to the supplied value.  */
    @JvmStatic
    operator fun set(array: Any, index: Int, value: Any) {
        java.lang.reflect.Array.set(array, index, value)
    }

}
