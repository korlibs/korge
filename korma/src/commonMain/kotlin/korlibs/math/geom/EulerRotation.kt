package korlibs.math.geom

import kotlin.math.*

inline class EulerRotation internal constructor(private val data: Vector3) {
    val roll: Angle get() = Angle.fromRatio(data.x)
    val pitch: Angle get() = Angle.fromRatio(data.y)
    val yaw: Angle get() = Angle.fromRatio(data.z)

    @Deprecated("", ReplaceWith("roll")) val x: Angle get() = roll
    @Deprecated("", ReplaceWith("pitch")) val y: Angle get() = pitch
    @Deprecated("", ReplaceWith("yaw")) val z: Angle get() = yaw

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
            return Quaternion(
                (cy * cp * sr - sy * sp * cr),
                (sy * cp * sr + cy * sp * cr),
                (sy * cp * cr - cy * sp * sr),
                (cy * cp * cr + sy * sp * sr)
            )
        }

        fun fromQuaternion(quat: Quaternion): EulerRotation {
            val (x, y, z, w) = quat
            val sinrCosp = +2f * (w * x + y * z)
            val cosrCosp = +1f - 2f * (x * x + y * y)
            val roll = atan2(sinrCosp, cosrCosp)
            val sinp = +2f * (w * y - z * x)
            val pitch: Float = when {
                abs(sinp) >= 1f -> if (sinp > 0) PI2F / 2 else -PI2F / 2
                else -> asin(sinp)
            }
            val sinyCosp = +2f * (w * z + x * y)
            val cosyCosp = +1f - 2f * (y * y + z * z)
            val yaw = atan2(sinyCosp, cosyCosp)
            return EulerRotation(roll.radians, pitch.radians, yaw.radians)
        }
    }
}
