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

import com.soywiz.korma.geom.*
import org.jbox2d.internal.*

/**
 * A 2-by-2 matrix. Stored in column-major order.
 */
class Mat22 {


    val ex: Vec2

    val ey: Vec2

    /**
     * Extract the angle in radians from this matrix (assumed to be a rotation matrix).
     *
     * @return
     */
    val angleRadians: Float get() = MathUtils.atan2(ex.y, ex.x)

    /**
     * Extract the angle in degrees from this matrix (assumed to be a rotation matrix).
     *
     * @return
     */
    val angleDegrees: Float get() = angleRadians * MathUtils.RAD2DEG

    /**
     * Extract the angle from this matrix (assumed to be a rotation matrix).
     *
     * @return
     */
    val angle: Angle get() = angleRadians.radians

    /** Convert the matrix to printable format.  */
    override fun toString(): String {
        var s = ""
        s += "[" + ex.x + "," + ey.x + "]\n"
        s += "[" + ex.y + "," + ey.y + "]"
        return s
    }

    /**
     * Construct zero matrix. Note: this is NOT an identity matrix! djm fixed double allocation
     * problem
     */
    constructor() {
        ex = Vec2()
        ey = Vec2()
    }

    /**
     * Create a matrix with given vectors as columns.
     *
     * @param c1 Column 1 of matrix
     * @param c2 Column 2 of matrix
     */
    constructor(c1: Vec2, c2: Vec2) {
        ex = c1.clone()
        ey = c2.clone()
    }

    /**
     * Create a matrix from four floats.
     *
     * @param exx
     * @param col2x
     * @param exy
     * @param col2y
     */
    constructor(exx: Float, col2x: Float, exy: Float, col2y: Float) {
        ex = Vec2(exx, exy)
        ey = Vec2(col2x, col2y)
    }

    /**
     * Set as a copy of another matrix.
     *
     * @param m Matrix to copy
     */
    fun set(m: Mat22): Mat22 {
        ex.x = m.ex.x
        ex.y = m.ex.y
        ey.x = m.ey.x
        ey.y = m.ey.y
        return this
    }

    fun set(exx: Float, col2x: Float, exy: Float, col2y: Float): Mat22 {
        ex.x = exx
        ex.y = exy
        ey.x = col2x
        ey.y = col2y
        return this
    }

    /**
     * Return a clone of this matrix. djm fixed double allocation
     */
    // @Override // annotation omitted for GWT-compatibility
    fun clone(): Mat22 {
        return Mat22(ex!!, ey!!)
    }

    /**
     * Set as a matrix representing a rotation.
     *
     * @param angleRadians Rotation (in radians) that matrix represents.
     */
    fun setRadians(angleRadians: Float) {
        val c = MathUtils.cos(angleRadians)
        val s = MathUtils.sin(angleRadians)
        ex.x = c
        ey.x = -s
        ex.y = s
        ey.y = c
    }

    fun setDegrees(angleDegrees: Float) = setRadians(angleDegrees * MathUtils.DEG2RAD)

    fun set(angle: Angle) = setRadians(angle.radians.toFloat())

    /**
     * Set as the identity matrix.
     */
    fun setIdentity() {
        ex.x = 1.0f
        ey.x = 0.0f
        ex.y = 0.0f
        ey.y = 1.0f
    }

    /**
     * Set as the zero matrix.
     */
    fun setZero() {
        ex.x = 0.0f
        ey.x = 0.0f
        ex.y = 0.0f
        ey.y = 0.0f
    }

    /**
     * Set by column vectors.
     *
     * @param c1 Column 1
     * @param c2 Column 2
     */
    fun set(c1: Vec2, c2: Vec2) {
        ex.x = c1.x
        ey.x = c2.x
        ex.y = c1.y
        ey.y = c2.y
    }

    /** Returns the inverted Mat22 - does NOT invert the matrix locally!  */
    fun invert(): Mat22 {
        val a = ex.x
        val b = ey.x
        val c = ex.y
        val d = ey.y
        val B = Mat22()
        var det = a * d - b * c
        if (det != 0f) {
            det = 1.0f / det
        }
        B.ex.x = det * d
        B.ey.x = -det * b
        B.ex.y = -det * c
        B.ey.y = det * a
        return B
    }

    fun invertLocal(): Mat22 {
        val a = ex.x
        val b = ey.x
        val c = ex.y
        val d = ey.y
        var det = a * d - b * c
        if (det != 0f) {
            det = 1.0f / det
        }
        ex.x = det * d
        ey.x = -det * b
        ex.y = -det * c
        ey.y = det * a
        return this
    }

    fun invertToOut(out: Mat22) {
        val a = ex.x
        val b = ey.x
        val c = ex.y
        val d = ey.y
        var det = a * d - b * c
        // b2Assert(det != 0.0f);
        det = 1.0f / det
        out.ex.x = det * d
        out.ey.x = -det * b
        out.ex.y = -det * c
        out.ey.y = det * a
    }


    /**
     * Return the matrix composed of the absolute values of all elements. djm: fixed double allocation
     *
     * @return Absolute value matrix
     */
    fun abs(): Mat22 {
        return Mat22(MathUtils.abs(ex.x), MathUtils.abs(ey.x), MathUtils.abs(ex.y),
                MathUtils.abs(ey.y))
    }

    /* djm: added */
    fun absLocal() {
        ex.absLocal()
        ey.absLocal()
    }

    /**
     * Multiply a vector by this matrix.
     *
     * @param v Vector to multiply by matrix.
     * @return Resulting vector
     */
    fun mul(v: Vec2): Vec2 {
        return Vec2(ex.x * v.x + ey.x * v.y, ex.y * v.x + ey.y * v.y)
    }

    fun mulToOut(v: Vec2, out: Vec2) {
        val tempy = ex.y * v.x + ey.y * v.y
        out.x = ex.x * v.x + ey.x * v.y
        out.y = tempy
    }

    fun mulToOutUnsafe(v: Vec2, out: Vec2) {
        assert(v !== out)
        out.x = ex.x * v.x + ey.x * v.y
        out.y = ex.y * v.x + ey.y * v.y
    }


    /**
     * Multiply another matrix by this one (this one on left). djm optimized
     *
     * @param R
     * @return
     */
    fun mul(R: Mat22): Mat22 {
        /*
     * Mat22 C = new Mat22();C.set(this.mul(R.ex), this.mul(R.ey));return C;
     */
        val C = Mat22()
        C.ex.x = ex.x * R.ex.x + ey.x * R.ex.y
        C.ex.y = ex.y * R.ex.x + ey.y * R.ex.y
        C.ey.x = ex.x * R.ey.x + ey.x * R.ey.y
        C.ey.y = ex.y * R.ey.x + ey.y * R.ey.y
        // C.set(ex,col2);
        return C
    }

    fun mulLocal(R: Mat22): Mat22 {
        mulToOut(R, this)
        return this
    }

    fun mulToOut(R: Mat22, out: Mat22) {
        val tempy1 = this.ex.y * R.ex.x + this.ey.y * R.ex.y
        val tempx1 = this.ex.x * R.ex.x + this.ey.x * R.ex.y
        out.ex.x = tempx1
        out.ex.y = tempy1
        val tempy2 = this.ex.y * R.ey.x + this.ey.y * R.ey.y
        val tempx2 = this.ex.x * R.ey.x + this.ey.x * R.ey.y
        out.ey.x = tempx2
        out.ey.y = tempy2
    }

    fun mulToOutUnsafe(R: Mat22, out: Mat22) {
        assert(out !== R)
        assert(out !== this)
        out.ex.x = this.ex.x * R.ex.x + this.ey.x * R.ex.y
        out.ex.y = this.ex.y * R.ex.x + this.ey.y * R.ex.y
        out.ey.x = this.ex.x * R.ey.x + this.ey.x * R.ey.y
        out.ey.y = this.ex.y * R.ey.x + this.ey.y * R.ey.y
    }

    /**
     * Multiply another matrix by the transpose of this one (transpose of this one on left). djm:
     * optimized
     *
     * @param B
     * @return
     */
    fun mulTrans(B: Mat22): Mat22 {
        /*
     * Vec2 c1 = new Vec2(Vec2.dot(this.ex, B.ex), Vec2.dot(this.ey, B.ex)); Vec2 c2 = new
     * Vec2(Vec2.dot(this.ex, B.ey), Vec2.dot(this.ey, B.ey)); Mat22 C = new Mat22(); C.set(c1, c2);
     * return C;
     */
        val C = Mat22()

        C.ex.x = Vec2.dot(this.ex!!, B.ex!!)
        C.ex.y = Vec2.dot(this.ey!!, B.ex)

        C.ey.x = Vec2.dot(this.ex, B.ey!!)
        C.ey.y = Vec2.dot(this.ey, B.ey)
        return C
    }

    fun mulTransLocal(B: Mat22): Mat22 {
        mulTransToOut(B, this)
        return this
    }

    fun mulTransToOut(B: Mat22, out: Mat22) {
        /*
     * out.ex.x = Vec2.dot(this.ex, B.ex); out.ex.y = Vec2.dot(this.ey, B.ex); out.ey.x =
     * Vec2.dot(this.ex, B.ey); out.ey.y = Vec2.dot(this.ey, B.ey);
     */
        val x1 = this.ex.x * B.ex.x + this.ex.y * B.ex.y
        val y1 = this.ey.x * B.ex.x + this.ey.y * B.ex.y
        val x2 = this.ex.x * B.ey.x + this.ex.y * B.ey.y
        val y2 = this.ey.x * B.ey.x + this.ey.y * B.ey.y
        out.ex.x = x1
        out.ey.x = x2
        out.ex.y = y1
        out.ey.y = y2
    }

    fun mulTransToOutUnsafe(B: Mat22, out: Mat22) {
        assert(B !== out)
        assert(this !== out)
        out.ex.x = this.ex.x * B.ex.x + this.ex.y * B.ex.y
        out.ey.x = this.ex.x * B.ey.x + this.ex.y * B.ey.y
        out.ex.y = this.ey.x * B.ex.x + this.ey.y * B.ex.y
        out.ey.y = this.ey.x * B.ey.x + this.ey.y * B.ey.y
    }

    /**
     * Multiply a vector by the transpose of this matrix.
     *
     * @param v
     * @return
     */
    fun mulTrans(v: Vec2): Vec2 {
        // return new Vec2(Vec2.dot(v, ex), Vec2.dot(v, col2));
        return Vec2(v.x * ex.x + v.y * ex.y, v.x * ey.x + v.y * ey.y)
    }

    /* djm added */
    fun mulTransToOut(v: Vec2, out: Vec2) {
        /*
     * out.x = Vec2.dot(v, ex); out.y = Vec2.dot(v, col2);
     */
        val tempx = v.x * ex.x + v.y * ex.y
        out.y = v.x * ey.x + v.y * ey.y
        out.x = tempx
    }

    /**
     * Add this matrix to B, return the result.
     *
     * @param B
     * @return
     */
    fun add(B: Mat22): Mat22 {
        // return new Mat22(ex.add(B.ex), col2.add(B.ey));
        val m = Mat22()
        m.ex.x = ex.x + B.ex.x
        m.ex.y = ex.y + B.ex.y
        m.ey.x = ey.x + B.ey.x
        m.ey.y = ey.y + B.ey.y
        return m
    }

    /**
     * Add B to this matrix locally.
     *
     * @param B
     * @return
     */
    fun addLocal(B: Mat22): Mat22 {
        // ex.addLocal(B.ex);
        // col2.addLocal(B.ey);
        ex.x += B.ex.x
        ex.y += B.ex.y
        ey.x += B.ey.x
        ey.y += B.ey.y
        return this
    }

    /**
     * Solve A * x = b where A = this matrix.
     *
     * @return The vector x that solves the above equation.
     */
    fun solve(b: Vec2): Vec2 {
        val a11 = ex.x
        val a12 = ey.x
        val a21 = ex.y
        val a22 = ey.y
        var det = a11 * a22 - a12 * a21
        if (det != 0.0f) {
            det = 1.0f / det
        }
        val x = Vec2(det * (a22 * b.x - a12 * b.y), det * (a11 * b.y - a21 * b.x))
        return x
    }

    fun solveToOut(b: Vec2, out: Vec2) {
        val a11 = ex.x
        val a12 = ey.x
        val a21 = ex.y
        val a22 = ey.y
        var det = a11 * a22 - a12 * a21
        if (det != 0.0f) {
            det = 1.0f / det
        }
        val tempy = det * (a11 * b.y - a21 * b.x)
        out.x = det * (a22 * b.x - a12 * b.y)
        out.y = tempy
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (ex?.hashCode() ?: 0)
        result = prime * result + (ey?.hashCode() ?: 0)
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null) return false
        if (this::class != obj::class) return false
        val other = obj as Mat22?
        if (ex == null) {
            if (other?.ex != null) return false
        } else if (ex != other?.ex) return false
        if (ey == null) {
            if (other?.ey != null) return false
        } else if (ey != other?.ey) return false
        return true
    }

    companion object {
        /**
         * Return the matrix composed of the absolute values of all elements.
         *
         * @return Absolute value matrix
         */

        fun abs(R: Mat22): Mat22 {
            return R.abs()
        }

        /* djm created */

        fun absToOut(R: Mat22, out: Mat22) {
            out.ex.x = MathUtils.abs(R.ex.x)
            out.ex.y = MathUtils.abs(R.ex.y)
            out.ey.x = MathUtils.abs(R.ey.x)
            out.ey.y = MathUtils.abs(R.ey.y)
        }


        fun mul(R: Mat22, v: Vec2): Vec2 {
            // return R.mul(v);
            return Vec2(R.ex.x * v.x + R.ey.x * v.y, R.ex.y * v.x + R.ey.y * v.y)
        }


        fun mulToOut(R: Mat22, v: Vec2, out: Vec2) {
            val tempy = R.ex.y * v.x + R.ey.y * v.y
            out.x = R.ex.x * v.x + R.ey.x * v.y
            out.y = tempy
        }


        fun mulToOutUnsafe(R: Mat22, v: Vec2, out: Vec2) {
            assert(v !== out)
            out.x = R.ex.x * v.x + R.ey.x * v.y
            out.y = R.ex.y * v.x + R.ey.y * v.y
        }


        fun mul(A: Mat22, B: Mat22): Mat22 {
            // return A.mul(B);
            val C = Mat22()
            C.ex.x = A.ex.x * B.ex.x + A.ey.x * B.ex.y
            C.ex.y = A.ex.y * B.ex.x + A.ey.y * B.ex.y
            C.ey.x = A.ex.x * B.ey.x + A.ey.x * B.ey.y
            C.ey.y = A.ex.y * B.ey.x + A.ey.y * B.ey.y
            return C
        }


        fun mulToOut(A: Mat22, B: Mat22, out: Mat22) {
            val tempy1 = A.ex.y * B.ex.x + A.ey.y * B.ex.y
            val tempx1 = A.ex.x * B.ex.x + A.ey.x * B.ex.y
            val tempy2 = A.ex.y * B.ey.x + A.ey.y * B.ey.y
            val tempx2 = A.ex.x * B.ey.x + A.ey.x * B.ey.y
            out.ex.x = tempx1
            out.ex.y = tempy1
            out.ey.x = tempx2
            out.ey.y = tempy2
        }


        fun mulToOutUnsafe(A: Mat22, B: Mat22, out: Mat22) {
            assert(out !== A)
            assert(out !== B)
            out.ex.x = A.ex.x * B.ex.x + A.ey.x * B.ex.y
            out.ex.y = A.ex.y * B.ex.x + A.ey.y * B.ex.y
            out.ey.x = A.ex.x * B.ey.x + A.ey.x * B.ey.y
            out.ey.y = A.ex.y * B.ey.x + A.ey.y * B.ey.y
        }


        fun mulTrans(R: Mat22, v: Vec2): Vec2 {
            return Vec2(v.x * R.ex.x + v.y * R.ex.y, v.x * R.ey.x + v.y * R.ey.y)
        }


        fun mulTransToOut(R: Mat22, v: Vec2, out: Vec2) {
            val outx = v.x * R.ex.x + v.y * R.ex.y
            out.y = v.x * R.ey.x + v.y * R.ey.y
            out.x = outx
        }


        fun mulTransToOutUnsafe(R: Mat22, v: Vec2, out: Vec2) {
            assert(out !== v)
            out.y = v.x * R.ey.x + v.y * R.ey.y
            out.x = v.x * R.ex.x + v.y * R.ex.y
        }


        fun mulTrans(A: Mat22, B: Mat22): Mat22 {
            val C = Mat22()
            C.ex.x = A.ex.x * B.ex.x + A.ex.y * B.ex.y
            C.ex.y = A.ey.x * B.ex.x + A.ey.y * B.ex.y
            C.ey.x = A.ex.x * B.ey.x + A.ex.y * B.ey.y
            C.ey.y = A.ey.x * B.ey.x + A.ey.y * B.ey.y
            return C
        }


        fun mulTransToOut(A: Mat22, B: Mat22, out: Mat22) {
            val x1 = A.ex.x * B.ex.x + A.ex.y * B.ex.y
            val y1 = A.ey.x * B.ex.x + A.ey.y * B.ex.y
            val x2 = A.ex.x * B.ey.x + A.ex.y * B.ey.y
            val y2 = A.ey.x * B.ey.x + A.ey.y * B.ey.y

            out.ex.x = x1
            out.ex.y = y1
            out.ey.x = x2
            out.ey.y = y2
        }


        fun mulTransToOutUnsafe(A: Mat22, B: Mat22, out: Mat22) {
            assert(A !== out)
            assert(B !== out)
            out.ex.x = A.ex.x * B.ex.x + A.ex.y * B.ex.y
            out.ex.y = A.ey.x * B.ex.x + A.ey.y * B.ex.y
            out.ey.x = A.ex.x * B.ey.x + A.ex.y * B.ey.y
            out.ey.y = A.ey.x * B.ey.x + A.ey.y * B.ey.y
        }


        fun createRotationalTransformRadians(angleRadians: Float, out: Mat22 = Mat22()): Mat22 {
            val c = MathUtils.cos(angleRadians)
            val s = MathUtils.sin(angleRadians)
            out.ex.x = c
            out.ey.x = -s
            out.ex.y = s
            out.ey.y = c
            return out
        }

        fun createRotationalTransformDegrees(angleDegrees: Float, out: Mat22 = Mat22()): Mat22
            = createRotationalTransformRadians(angleDegrees * MathUtils.DEG2RAD, out)

        fun createRotationalTransform(angle: Angle, out: Mat22 = Mat22()): Mat22
            = createRotationalTransformRadians(angle.radians.toFloat(), out)


        fun createScaleTransform(scale: Float): Mat22 {
            val mat = Mat22()
            mat.ex.x = scale
            mat.ey.y = scale
            return mat
        }


        fun createScaleTransform(scale: Float, out: Mat22) {
            out.ex.x = scale
            out.ey.y = scale
        }
    }
}
