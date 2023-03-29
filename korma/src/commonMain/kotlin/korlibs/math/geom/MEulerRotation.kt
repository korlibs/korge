package korlibs.math.geom

import korlibs.math.annotations.KormaMutableApi

@KormaMutableApi
@Deprecated("")
data class MEulerRotation(
    var x: Angle = 0.degrees,
    var y: Angle = 0.degrees,
    var z: Angle = 0.degrees
) {
    companion object {
        fun toQuaternion(roll: Angle, pitch: Angle, yaw: Angle, out: MQuaternion = MQuaternion()): MQuaternion {
            val cr = cosd(roll * 0.5)
            val sr = sind(roll * 0.5)
            val cp = cosd(pitch * 0.5)
            val sp = sind(pitch * 0.5)
            val cy = cosd(yaw * 0.5)
            val sy = sind(yaw * 0.5)
            return out.setTo(
                (cy * cp * sr - sy * sp * cr),
                (sy * cp * sr + cy * sp * cr),
                (sy * cp * cr - cy * sp * sr),
                (cy * cp * cr + sy * sp * sr)
            )
        }
        fun toQuaternion(euler: MEulerRotation, out: MQuaternion = MQuaternion()): MQuaternion = toQuaternion(euler.x, euler.y, euler.z, out)
    }

    fun toQuaternion(out: MQuaternion = MQuaternion()): MQuaternion = toQuaternion(this, out)

    fun setQuaternion(x: Double, y: Double, z: Double, w: Double): MEulerRotation =
        MQuaternion.toEuler(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), this)
    fun setQuaternion(x: Int, y: Int, z: Int, w: Int): MEulerRotation =
        MQuaternion.toEuler(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), this)
    fun setQuaternion(x: Float, y: Float, z: Float, w: Float): MEulerRotation = MQuaternion.toEuler(x, y, z, w, this)

    fun setQuaternion(quaternion: MQuaternion): MEulerRotation =
        MQuaternion.toEuler(quaternion.x, quaternion.y, quaternion.z, quaternion.w, this)
    fun setTo(x: Angle, y: Angle, z: Angle): MEulerRotation = this
        .apply { this.x = x }
        .apply { this.y = y }
        .apply { this.z = z }

    fun setTo(other: MEulerRotation): MEulerRotation = setTo(other.x, other.y, other.z)

    private val tempQuat: MQuaternion by lazy { MQuaternion() }
    fun toMatrix(out: MMatrix3D = MMatrix3D()): MMatrix3D = tempQuat.setEuler(this).toMatrix(out)
}
