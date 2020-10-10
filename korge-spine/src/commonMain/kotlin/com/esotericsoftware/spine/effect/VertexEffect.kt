package com.esotericsoftware.spine.effect

import com.esotericsoftware.spine.*
import com.esotericsoftware.spine.utils.*
import com.soywiz.korim.color.*

/** Modifies the skeleton or vertex positions, UVs, or colors during rendering.  */
interface VertexEffect {
    fun begin(skeleton: Skeleton)

    fun transform(position: SpineVector2, uv: SpineVector2, color: RGBAf, darkColor: RGBAf)

    fun end()
}
