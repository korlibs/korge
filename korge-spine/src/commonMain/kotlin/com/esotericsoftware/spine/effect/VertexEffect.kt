package com.esotericsoftware.spine.effect

import com.esotericsoftware.spine.*
import com.esotericsoftware.spine.graphics.*
import com.esotericsoftware.spine.utils.*

/** Modifies the skeleton or vertex positions, UVs, or colors during rendering.  */
interface VertexEffect {
    fun begin(skeleton: Skeleton)

    fun transform(position: Vector2, uv: Vector2, color: Color, darkColor: Color)

    fun end()
}
