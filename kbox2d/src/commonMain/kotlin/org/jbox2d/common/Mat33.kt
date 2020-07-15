/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.jbox2d.common

import org.jbox2d.internal.*

/**
 * A 3-by-3 matrix. Stored in column-major order.
 *
 * @author Daniel Murphy
 */
class Mat33 {


    val ex: Vec3

    val ey: Vec3

    val ez: Vec3

    constructor() {
        ex = Vec3()
        ey = Vec3()
        ez = Vec3()
    }

    constructor(exx: Float, exy: Float, exz: Float, eyx: Float, eyy: Float, eyz: Float, ezx: Float,
                ezy: Float, ezz: Float) {
        ex = Vec3(exx, exy, exz)
        ey = Vec3(eyx, eyy, eyz)
        ez = Vec3(ezx, ezy, ezz)
    }

    constructor(argCol1: Vec3, argCol2: Vec3, argCol3: Vec3) {
        ex = argCol1.clone()
        ey = argCol2.clone()
        ez = argCol3.clone()
    }

    fun setZero() {
        ex.setZero()
        ey.setZero()
        ez.setZero()
    }

    fun set(exx: Float, exy: Float, exz: Float, eyx: Float, eyy: Float, eyz: Float, ezx: Float,
                     ezy: Float, ezz: Float) {
        ex.x = exx
        ex.y = exy
        ex.z = exz
        ey.x = eyx
        ey.y = eyy
        ey.z = eyz
        ez.x = eyx
        ez.y = eyy
        ez.z = eyz
    }

    fun set(mat: Mat33) {
        val vec = mat.ex
        ex.x = vec.x
        ex.y = vec.y
        ex.z = vec.z
        val vec1 = mat.ey
        ey.x = vec1.x
        ey.y = vec1.y
        ey.z = vec1.z
        val vec2 = mat.ez
        ez.x = vec2.x
        ez.y = vec2.y
        ez.z = vec2.z
    }

    fun setIdentity() {
        ex.x = 1.toFloat()
        ex.y = 0.toFloat()
        ex.z = 0.toFloat()
        ey.x = 0.toFloat()
        ey.y = 1.toFloat()
        ey.z = 0.toFloat()
        ez.x = 0.toFloat()
        ez.y = 0.toFloat()
        ez.z = 1.toFloat()
    }

    /**
     * Solve A * x = b, where b is a column vector. This is more efficient than computing the inverse
     * in one-shot cases.
     *
     * @param b
     * @return
     */
    fun solve22(b: Vec2): Vec2 {
        val x = Vec2()
        solve22ToOut(b, x)
        return x
    }

    /**
     * Solve A * x = b, where b is a column vector. This is more efficient than computing the inverse
     * in one-shot cases.
     *
     * @param b
     * @return
     */
    fun solve22ToOut(b: Vec2, out: Vec2) {
        val a11 = ex.x
        val a12 = ey.x
        val a21 = ex.y
        val a22 = ey.y
        var det = a11 * a22 - a12 * a21
        if (det != 0.0f) {
            det = 1.0f / det
        }
        out.x = det * (a22 * b.x - a12 * b.y)
        out.y = det * (a11 * b.y - a21 * b.x)
    }

    // djm pooling from below
    /**
     * Solve A * x = b, where b is a column vector. This is more efficient than computing the inverse
     * in one-shot cases.
     *
     * @param b
     * @return
     */
    fun solve33(b: Vec3): Vec3 {
        val x = Vec3()
        solve33ToOut(b, x)
        return x
    }

    /**
     * Solve A * x = b, where b is a column vector. This is more efficient than computing the inverse
     * in one-shot cases.
     *
     * @param b
     * @param out the result
     */
    fun solve33ToOut(b: Vec3, out: Vec3) {
        assert(b !== out)
        Vec3.crossToOutUnsafe(ey!!, ez!!, out)
        var det = Vec3.dot(ex!!, out)
        if (det != 0.0f) {
            det = 1.0f / det
        }
        Vec3.crossToOutUnsafe(ey, ez, out)
        val x = det * Vec3.dot(b, out)
        Vec3.crossToOutUnsafe(b, ez, out)
        val y = det * Vec3.dot(ex, out)
        Vec3.crossToOutUnsafe(ey, b, out)
        val z = det * Vec3.dot(ex, out)
        out.x = x
        out.y = y
        out.z = z
    }

    fun getInverse22(M: Mat33) {
        val a = ex.x
        val b = ey.x
        val c = ex.y
        val d = ey.y
        var det = a * d - b * c
        if (det != 0.0f) {
            det = 1.0f / det
        }

        M.ex.x = det * d
        M.ey.x = -det * b
        M.ex.z = 0.0f
        M.ex.y = -det * c
        M.ey.y = det * a
        M.ey.z = 0.0f
        M.ez.x = 0.0f
        M.ez.y = 0.0f
        M.ez.z = 0.0f
    }

    // / Returns the zero matrix if singular.
    fun getSymInverse33(M: Mat33) {
        val bx = ey.y * ez.z - ey.z * ez.y
        val by = ey.z * ez.x - ey.x * ez.z
        val bz = ey.x * ez.y - ey.y * ez.x
        var det = ex.x * bx + ex.y * by + ex.z * bz
        if (det != 0.0f) {
            det = 1.0f / det
        }

        val a11 = ex.x
        val a12 = ey.x
        val a13 = ez.x
        val a22 = ey.y
        val a23 = ez.y
        val a33 = ez.z

        M.ex.x = det * (a22 * a33 - a23 * a23)
        M.ex.y = det * (a13 * a23 - a12 * a33)
        M.ex.z = det * (a12 * a23 - a13 * a22)

        M.ey.x = M.ex.y
        M.ey.y = det * (a11 * a33 - a13 * a13)
        M.ey.z = det * (a13 * a12 - a11 * a23)

        M.ez.x = M.ex.z
        M.ez.y = M.ey.z
        M.ez.z = det * (a11 * a22 - a12 * a12)
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (ex?.hashCode() ?: 0)
        result = prime * result + (ey?.hashCode() ?: 0)
        result = prime * result + (ez?.hashCode() ?: 0)
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null) return false
        if (this::class != obj::class) return false
        val other = obj as Mat33?
        if (ex == null) {
            if (other?.ex != null) return false
        } else if (ex != other?.ex) return false
        if (ey == null) {
            if (other?.ey != null) return false
        } else if (ey != other?.ey) return false
        if (ez == null) {
            if (other?.ez != null) return false
        } else if (ez != other?.ez) return false
        return true
    }

    companion object {

        //@ThreadLocal
        val IDENTITY = Mat33(Vec3(1f, 0f, 0f), Vec3(0f, 1f, 0f), Vec3(0f, 0f, 1f))

        // / Multiply a matrix times a vector.

        fun mul(A: Mat33, v: Vec3): Vec3 {
            return Vec3(v.x * A.ex.x + v.y * A.ey.x + v.z + A.ez.x, v.x * A.ex.y + v.y * A.ey.y + v.z * A.ez.y, v.x * A.ex.z + v.y * A.ey.z + v.z * A.ez.z)
        }


        fun mul22(A: Mat33, v: Vec2): Vec2 {
            return Vec2(A.ex.x * v.x + A.ey.x * v.y, A.ex.y * v.x + A.ey.y * v.y)
        }


        fun mul22ToOut(A: Mat33, v: Vec2, out: Vec2) {
            val tempx = A.ex.x * v.x + A.ey.x * v.y
            out.y = A.ex.y * v.x + A.ey.y * v.y
            out.x = tempx
        }


        fun mul22ToOutUnsafe(A: Mat33, v: Vec2, out: Vec2) {
            assert(v !== out)
            out.y = A.ex.y * v.x + A.ey.y * v.y
            out.x = A.ex.x * v.x + A.ey.x * v.y
        }


        fun mulToOut(A: Mat33, v: Vec3, out: Vec3) {
            val tempy = v.x * A.ex.y + v.y * A.ey.y + v.z * A.ez.y
            val tempz = v.x * A.ex.z + v.y * A.ey.z + v.z * A.ez.z
            out.x = v.x * A.ex.x + v.y * A.ey.x + v.z * A.ez.x
            out.y = tempy
            out.z = tempz
        }


        fun mulToOutUnsafe(A: Mat33, v: Vec3, out: Vec3) {
            assert(out !== v)
            out.x = v.x * A.ex.x + v.y * A.ey.x + v.z * A.ez.x
            out.y = v.x * A.ex.y + v.y * A.ey.y + v.z * A.ez.y
            out.z = v.x * A.ex.z + v.y * A.ey.z + v.z * A.ez.z
        }


        fun setScaleTransform(scale: Float, out: Mat33) {
            out.ex.x = scale
            out.ey.y = scale
        }
    }
}
