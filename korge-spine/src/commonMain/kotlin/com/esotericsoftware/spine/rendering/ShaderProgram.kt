package com.esotericsoftware.spine.rendering

import com.esotericsoftware.spine.utils.Matrix4

class ShaderProgram(vertexShader: String?, fragmentShader: String?) {
    fun begin() {}
    fun end() {}
    fun dispose() {}
    fun setUniformf(u_pma: String?, i: Int) {}
    fun setUniformMatrix(u_projTrans: String?, combinedMatrix: Matrix4?) {}
    fun setUniformi(u_texture: String?, i: Int) {}
    val isCompiled: Boolean
        get() = false
    val log: String?
        get() = null
}
