package korlibs.math.geom

import korlibs.math.*
import kotlin.math.*

/**
 * Rotations around Z axis, then X axis, then Y axis in that order.
 */
inline class EulerRotation private constructor(val data: Vector4F) : IsAlmostEqualsF<EulerRotation> {
    val config: Config get() = Config(data.w.toInt())
    val order: Order get() = config.order
    val coordinateSystem: CoordinateSystem get() = config.coordinateSystem

    enum class Order(
        val x: Int, val y: Int, val z: Int, val w: Int, val str: String,
    ) {
        INVALID(0, 0, 0, 0, "XXX"),
        XYZ(+1, -1, +1, -1, "XYZ"),
        XZY(-1, -1, +1, +1, "XZY"),
        YXZ(+1, -1, -1, +1, "YXZ"),
        YZX(+1, +1, -1, -1, "YZX"),
        ZXY(-1, +1, +1, -1, "ZXY"),
        ZYX(-1, +1, -1, +1, "ZYX"),
        ;

        fun withCoordinateSystem(coordinateSystem: CoordinateSystem) = if (coordinateSystem.sign < 0) reversed() else this

        fun reversed(): Order = when (this) {
            INVALID -> INVALID
            XYZ -> ZYX
            XZY -> YZX
            YXZ -> ZXY
            YZX -> XZY
            ZXY -> YXZ
            ZYX -> XYZ
        }

        fun indexAt(pos: Int, reversed: Boolean = false): Int = str[(if (reversed) 2 - pos else pos) umod 3] - 'X'

        override fun toString(): String = "$name [$x, $y, $z, $w]"

        companion object {
            val VALUES = values()
            val DEFAULT = XYZ
        }
    }
    //enum class Normalized { NO, FULL_ANGLE, HALF_ANGLE }
    inline class Config(val id: Int) {
        //constructor(order: Order, coordinateSystem: CoordinateSystem) : this(order.ordinal * coordinateSystem.sign)
        constructor(order: Order, coordinateSystem: CoordinateSystem) : this(order.withCoordinateSystem(coordinateSystem).ordinal)

        val order: Order get() = Order.VALUES[id.absoluteValue]
        val coordinateSystem: CoordinateSystem get() = if (id < 0) CoordinateSystem.LEFT_HANDED else CoordinateSystem.RIGHT_HANDED

        override fun toString(): String = "EulerRotation.Config(order=$order, coordinateSystem=$coordinateSystem)"

        companion object {
            val UNITY get() = Config(Order.ZXY, CoordinateSystem.LEFT_HANDED)
            //val UNITY get() = LIBGDX
            val UNREAL get() = Config(Order.ZYX, CoordinateSystem.LEFT_HANDED)
            //val UNREAL get() = THREEJS
            val GODOT get() = Config(Order.YXZ, CoordinateSystem.RIGHT_HANDED)
            val LIBGDX get() = Config(Order.YXZ, CoordinateSystem.RIGHT_HANDED)
            val THREEJS get() = Config(Order.XYZ, CoordinateSystem.RIGHT_HANDED)

            // Same as Three.JS
            val DEFAULT get() = Config(Order.XYZ, CoordinateSystem.RIGHT_HANDED)
        }
    }
    enum class CoordinateSystem(val sign: Int) {
        LEFT_HANDED(-1), RIGHT_HANDED(+1);
        val rsign = -sign
    }

    val roll: Angle get() = Angle.fromRatio(data.x)
    val pitch: Angle get() = Angle.fromRatio(data.y)
    val yaw: Angle get() = Angle.fromRatio(data.z)

    @Deprecated("", ReplaceWith("roll")) val x: Angle get() = roll
    @Deprecated("", ReplaceWith("pitch")) val y: Angle get() = pitch
    @Deprecated("", ReplaceWith("yaw")) val z: Angle get() = yaw

    override fun toString(): String = "EulerRotation(roll=$roll, pitch=$pitch, yaw=$yaw)"

    fun copy(roll: Angle = this.roll, pitch: Angle = this.pitch, yaw: Angle = this.yaw): EulerRotation = EulerRotation(roll, pitch, yaw)
    constructor() : this(Angle.ZERO, Angle.ZERO, Angle.ZERO)
    constructor(roll: Angle, pitch: Angle, yaw: Angle, config: Config = Config.DEFAULT)
        : this(Vector4F(roll.ratio.toFloat(), pitch.ratio.toFloat(), yaw.ratio.toFloat(), config.id.toFloat()))

    fun normalized(): EulerRotation = EulerRotation(roll.normalized, pitch.normalized, yaw.normalized)
    fun normalizedHalf(): EulerRotation = EulerRotation(roll.normalizedHalf, pitch.normalizedHalf, yaw.normalizedHalf)

    fun toMatrix(): Matrix4 = toQuaternion().toMatrix()
    fun toQuaternion(): Quaternion = _toQuaternion(x, y, z, config)
    override fun isAlmostEquals(other: EulerRotation, epsilon: Float): Boolean =
        this.data.isAlmostEquals(other.data, epsilon)

    companion object {
        fun toQuaternion(roll: Angle, pitch: Angle, yaw: Angle, config: Config = Config.DEFAULT): Quaternion {
            return _toQuaternion(roll, pitch, yaw, config)
        }
        // http://www.mathworks.com/matlabcentral/fileexchange/20696-function-to-convert-between-dcm-euler-angles-quaternions-and-euler-vectors/content/SpinCalc.m
        private fun _toQuaternion(x: Angle, y: Angle, z: Angle, config: Config = Config.DEFAULT): Quaternion {
            val order = config.order
            val coordinateSystem = config.coordinateSystem
            val sign = coordinateSystem.sign
            //println("ORDER=$order, coordinateSystem=$coordinateSystem, sign=$sign")

            val c1 = cos(x / 2)
            val c2 = cos(y / 2)
            val c3 = cos(z / 2)
            val s1 = sin(x / 2)
            val s2 = sin(y / 2)
            val s3 = sin(z / 2)

            return Quaternion(
                ((s1 * c2 * c3) + ((c1 * s2 * s3) * order.x * sign)),
                ((c1 * s2 * c3) + ((s1 * c2 * s3) * order.y * sign)),
                ((c1 * c2 * s3) + ((s1 * s2 * c3) * order.z * sign)),
                ((c1 * c2 * c3) + ((s1 * s2 * s3) * order.w * sign)),
            )
        }

        fun fromRotationMatrix(m: Matrix3, config: Config = Config.DEFAULT): EulerRotation {
            //val config = if (config == Config.UNITY) Config.LIBGDX else config
            val order = config.order
            val coordinateSystem = config.coordinateSystem

            val sign = coordinateSystem.sign

            //val m = if (sign < 0) m.transposed() else m
            //val m = m

            val m11 = m.v00
            val m12 = m.v01
            val m13 = m.v02

            val m21 = m.v10
            val m22 = m.v11
            val m23 = m.v12

            val m31 = m.v20
            val m32 = m.v21
            val m33 = m.v22

            val x: Angle
            val y: Angle
            val z: Angle

            when (order) {
                Order.XYZ -> {
                    x = if (m13.absoluteNotAlmostOne) Angle.atan2(-m23, m33) else Angle.atan2(m32, m22)
                    y = Angle.asin(m13.clamp(-1f, +1f))
                    z = if (m13.absoluteNotAlmostOne) Angle.atan2(-m12, m11) else Angle.ZERO
                }
                Order.YXZ -> {
                    x = Angle.asin(-(m23.clamp(-1f, +1f)))
                    y = if (m23.absoluteNotAlmostOne) Angle.atan2(m13, m33) else Angle.atan2(-m31, m11)
                    z = if (m23.absoluteNotAlmostOne) Angle.atan2(m21, m22) else Angle.ZERO
                }
                Order.ZXY -> {
                    y = Angle.asin(m32.clamp(-1f, +1f))
                    x = if (m32.absoluteNotAlmostOne) Angle.atan2(-m31, m33) else Angle.ZERO
                    z = if (m32.absoluteNotAlmostOne) Angle.atan2(-m12, m22) else Angle.atan2(m21, m11)
                }
                Order.ZYX -> {
                    x = if (m31.absoluteNotAlmostOne) Angle.atan2(m32, m33) else Angle.ZERO
                    y = Angle.asin(-(m31.clamp(-1f, +1f)))
                    z = if (m31.absoluteNotAlmostOne) Angle.atan2(m21, m11) else Angle.atan2(-m12, m22)
                }
                Order.YZX -> {
                    x = if (m21.absoluteNotAlmostOne) Angle.atan2(-m23, m22) else Angle.ZERO
                    y = if (m21.absoluteNotAlmostOne) Angle.atan2(-m31, m11) else Angle.atan2(m13, m33)
                    z = Angle.asin(m21.clamp(-1f, +1f))
                }
                Order.XZY -> {
                    x = if (m12.absoluteNotAlmostOne) Angle.atan2(m32, m22) else Angle.atan2(-m23, m33)
                    y = if (m12.absoluteNotAlmostOne) Angle.atan2(m13, m11) else Angle.ZERO
                    z = Angle.asin(-(m12.clamp(-1f, +1f)))
                }
                Order.INVALID -> error("Invalid")
            }

            //println("order=$order, coordinateSystem=$coordinateSystem : ${coordinateSystem.sign}, x=$x, y=$y, z=$z")

            //val sign = coordinateSystem.sign
            //return EulerRotation(x * coordinateSystem.sign, y * coordinateSystem.sign, z * coordinateSystem.sign, config)
            //return EulerRotation(x * sign, y * sign, z * sign, config)
            return EulerRotation(x, y, z, config)
        }

        private val Float.absoluteNotAlmostOne: Boolean get() = absoluteValue < 0.9999999


        fun fromQuaternion(q: Quaternion, config: Config = Config.DEFAULT): EulerRotation {
            return fromRotationMatrix(q.toMatrix3(), config)
            /*
            //return fromQuaternion(q.x, q.y, q.z, q.w, config)

            val extrinsic = false

            // intrinsic/extrinsic conversion helpers
            val angle_first: Int
            val angle_third: Int
            val reversed: Boolean
            if (extrinsic) {
                angle_first = 0
                angle_third = 2
                reversed = false
            } else {
                reversed = true
                //reversed = false
                //seq = seq[:: - 1]
                angle_first = 2
                angle_third = 0
            }

            val quat = q
            val i = config.order.indexAt(0, reversed = reversed)
            val j = config.order.indexAt(1, reversed = reversed)
            val symmetric = i == j
            var k = if (symmetric) 3 - i - j else config.order.indexAt(2, reversed = reversed)
            val sign = (i - j) * (j - k) * (k - i) / 2

            println("ORDER: $i, $j, $k")
            val eps = 1e-7f

            val _angles = FloatArray(3)
            //_angles = angles[ind, :]

            // Step 1
            // Permutate quaternion elements
            val a: Float
            val b: Float
            val c: Float
            val d: Float
            if (symmetric) {
                a = quat[3]
                b = quat[i]
                c = quat[j]
                d = quat[k] * sign
            } else {
                a = quat[3] - quat[j]
                b = quat[i] + quat[k] * sign
                c = quat[j] + quat[3]
                d = quat[k] * sign - quat[i]
            }

            // Step 2
            // Compute second angle...
            _angles[1] = 2 * atan2(hypot(c, d), hypot(a, b))

            // ... and check if equal to is 0 or pi, causing a singularity
            val case = when {
                abs(_angles[1]) <= eps -> 1
                abs(_angles[1] - PIF) <= eps -> 2
                else -> 0 // normal case
            }

            // Step 3
            // compute first and third angles, according to case
            val half_sum = atan2(b, a)
            val half_diff = atan2(d, c)

            if (case == 0) {  // no singularities
                _angles[angle_first] = half_sum - half_diff
                _angles[angle_third] = half_sum + half_diff
            } else {  // any degenerate case
                _angles[2] = 0f
                if (case == 1) {
                    _angles[0] = 2 * half_sum
                } else {
                    _angles[0] = 2 * half_diff * (if (extrinsic) -1 else 1)
                }
            }

            // for Tait-Bryan angles
            if (!symmetric) {
                _angles[angle_third] *= sign.toFloat()
                _angles[1] -= PIF / 2
            }

            for (idx in 0 until 3) {
                if (_angles[idx] < -PIF) {
                    _angles[idx] += 2 * PIF
                } else if (_angles[idx] > PIF) {
                    _angles[idx] -= 2 * PIF
                }
            }

            if (case != 0) {
                println(
                    "Gimbal lock detected. Setting third angle to zero " +
                    "since it is not possible to uniquely determine " +
                    "all angles."
                )
            }

            return EulerRotation(_angles[0].radians, _angles[2].radians, _angles[1].radians * config.coordinateSystem.sign)
            */
        }

        fun fromQuaternion(x: Float, y: Float, z: Float, w: Float, config: Config = Config.DEFAULT): EulerRotation {

            return fromQuaternion(Quaternion(x, y, z, w), config)
            /*
            val t = y * x + z * w
            // Gimbal lock, if any: positive (+1) for north pole, negative (-1) for south pole, zero (0) when no gimbal lock
            val pole = if (t > 0.499f) 1 else if (t < -0.499f) -1 else 0
            println("pole=$pole")
            println(Angle.atan2(2f * (y * w + x * z), 1f - 2f * (y * y + x * x)))
            return EulerRotation(
                roll = when (pole) {
                    0 -> Angle.asin((2f * (w * x - z * y)).clamp(-1f, +1f))
                    else -> (pole.toFloat() * PIF * .5f).radians
                },
                pitch = when (pole) {
                    0 -> Angle.atan2(2f * (y * w + x * z), 1f - 2f * (y * y + x * x))
                    else -> Angle.ZERO
                },
                yaw = when (pole) {
                    0 -> Angle.atan2(2f * (w * z + y * x), 1f - 2f * (x * x + z * z))
                    else -> Angle.atan2(y, w) * pole.toFloat() * 2f
                },
            )

             */
        }
    }
}
