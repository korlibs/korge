package korlibs.math.geom

import korlibs.math.normalizeAlmostZero
import kotlin.math.*

inline class EulerRotation internal constructor(private val data: Vector3) {
    val roll: Angle get() = Angle.fromRatio(data.x)
    val pitch: Angle get() = Angle.fromRatio(data.y)
    val yaw: Angle get() = Angle.fromRatio(data.z)

    @Deprecated("", ReplaceWith("roll")) val x: Angle get() = roll
    @Deprecated("", ReplaceWith("pitch")) val y: Angle get() = pitch
    @Deprecated("", ReplaceWith("yaw")) val z: Angle get() = yaw

    override fun toString(): String = "EulerRotation(roll=$roll, pitch=$pitch, yaw=$yaw)"

    fun copy(roll: Angle = this.roll, pitch: Angle = this.pitch, yaw: Angle = this.yaw): EulerRotation = EulerRotation(roll, pitch, yaw)
    constructor(roll: Angle, pitch: Angle, yaw: Angle) : this(Vector3(roll.ratio, pitch.ratio, yaw.ratio))

    fun toMatrix(): Matrix4 = toQuaternion().toMatrix()
    fun toQuaternion(): Quaternion = Companion.toQuaternion(roll, pitch, yaw)

    companion object {
        fun toQuaternion(roll: Angle, pitch: Angle, yaw: Angle): Quaternion {
            val cr = cos(roll * 0.5f)
            val sr = sin(roll * 0.5f)
            val cp = cos(pitch * 0.5f)
            val sp = sin(pitch * 0.5f)
            val cy = cos(yaw * 0.5f)
            val sy = sin(yaw * 0.5f)
            //println("roll=$roll, pitch=$pitch, yaw=$yaw, [cr=$cr,sr=$sr], [cp=$cp,sp=$sp], [cy=$cy,sy=$sy]")
            return Quaternion(
                ((cy * cp * sr) - (sy * sp * cr)),
                ((sy * cp * sr) + (cy * sp * cr)),
                ((sy * cp * cr) - (cy * sp * sr)),
                ((cy * cp * cr) + (sy * sp * sr)),
            )
        }

        fun fromQuaternion(quat: Quaternion): EulerRotation {
            val (x, y, z, w) = quat
            //println("$x, $y, $z, $w")
            val sinrCosp = (+2f * (w * x + y * z)).normalizeAlmostZero()
            val cosrCosp = (+1f - 2f * (x * x + y * y)).normalizeAlmostZero()
            val roll = atan2(sinrCosp, cosrCosp)
            //println("roll=$roll, sinrCosp=$sinrCosp, cosrCosp=$cosrCosp")
            val sinp = (+2f * (w * y - z * x)).normalizeAlmostZero()
            val pitch: Float = when {
                abs(sinp) > 1f -> if (sinp > 0) PI2F / 2 else -PI2F / 2
                else -> asin(sinp)
            }
            val sinyCosp = (+2f * (w * z + x * y)).normalizeAlmostZero()
            val cosyCosp = (+1f - 2f * (y * y + z * z)).normalizeAlmostZero()
            val yaw = atan2(sinyCosp, cosyCosp)
            //println("x=$x, y=$y, z=$z, w=$w, sinrCosp=$sinrCosp, cosrCosp=$cosrCosp, roll=$roll, sinp=$sinp, pitch=$pitch, sinyCosp=$sinyCosp, cosyCosp=$cosyCosp, yaw=$yaw")
            return EulerRotation(roll.radians, pitch.radians, yaw.radians)
        }
    }
}
