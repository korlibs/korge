package korlibs.math.geom

import kotlin.test.*

class QuaternionTest {
    @Test
    fun testTransformMatrix() {
        assertEqualsFloat(Vector3F.RIGHT, Quaternion.fromVectors(Vector3F.UP, Vector3F.RIGHT).toMatrix().transform(Vector3F.UP))
    }

    @Test
    fun testTransformQuat() {
        assertEqualsFloat(Vector3F.RIGHT, Quaternion.fromVectors(Vector3F.UP, Vector3F.RIGHT).transform(Vector3F.UP))
    }

    @Test
    fun testScaled() {
        assertEqualsFloat(Vector3F.UP, Quaternion.fromVectors(Vector3F.UP, Vector3F.RIGHT).scaled(0f).transform(Vector3F.UP))
        assertEqualsFloat(Vector3F.RIGHT, Quaternion.fromVectors(Vector3F.UP, Vector3F.RIGHT).scaled(1f).transform(Vector3F.UP))
        assertEqualsFloat(Vector3F.LEFT, Quaternion.fromVectors(Vector3F.UP, Vector3F.RIGHT).scaled(-1f).transform(Vector3F.UP))
        assertEqualsFloat(Vector3F.DOWN, Quaternion.fromVectors(Vector3F.UP, Vector3F.RIGHT).scaled(2f).transform(Vector3F.UP))
    }

    @Test
    fun testInverse() {
        assertEqualsFloat(
            Quaternion.IDENTITY,
            Quaternion.fromVectors(Vector3F.UP, Vector3F.RIGHT) * Quaternion.fromVectors(Vector3F.UP, Vector3F.RIGHT).inverted()
        )
        assertEqualsFloat(
            Quaternion.fromVectors(Vector3F.UP, Vector3F.LEFT),
            Quaternion.fromVectors(Vector3F.UP, Vector3F.RIGHT).inverted()
        )
        assertEqualsFloat(
            Quaternion.fromVectors(Vector3F.UP, Vector3F.RIGHT),
            Quaternion.fromVectors(Vector3F.UP, Vector3F.LEFT).inverted()
        )
    }

    @Test
    fun testAngleBetween() {
        assertEqualsFloat(
            90.degrees,
            Angle.between(
                Quaternion.fromVectors(Vector3F.UP, Vector3F.UP),
                Quaternion.fromVectors(Vector3F.UP, Vector3F.RIGHT) * 1f
            )
        )
        assertEqualsFloat(
            45.degrees,
            Angle.between(
                Quaternion.IDENTITY,
                Quaternion.interpolated(
                    Quaternion.fromVectors(Vector3F.UP, Vector3F.UP),
                    Quaternion.fromVectors(Vector3F.UP, Vector3F.RIGHT),
                    .5f
                )
            )
        )
        assertEqualsFloat(
            90.degrees,
            Quaternion.IDENTITY.angleTo(
                Quaternion.fromVectors(Vector3F.UP, Vector3F.UP)
                    .interpolated(
                        Quaternion.fromVectors(Vector3F.UP, Vector3F.DOWN),
                        .5f
                    )
            )
        )
    }

    @Test
    //@Ignore // Failing for now
    fun testToEulerUnity() {
        assertEqualsFloat(
            EulerRotation(90.degrees, (-90).degrees, 0.degrees, EulerRotation.Config.UNITY).normalized(),
            Quaternion(-.5f, .5f, -.5f, -.5f).toEuler(EulerRotation.Config.UNITY).normalized()
        )
        assertEqualsFloat(
            EulerRotation(315.degrees, 30.degrees, 90.degrees, EulerRotation.Config.UNITY).normalized(),
            Quaternion(-.09230f, .43046f, .70106f, .56099f).toEuler(EulerRotation.Config.UNITY).normalized()
        )
        assertEqualsFloat(
            EulerRotation(30.degrees, 315.degrees, 0.degrees, EulerRotation.Config.UNITY).normalized(),
            Quaternion(.23912f, -.36964f, .09905f, .89240f).toEuler(EulerRotation.Config.UNITY).normalized(),
        )
    }

    @Test
    fun testFromEulerUnity() {
        assertEqualsFloat(
            Quaternion(-.5f, .5f, -.5f, -.5f),
            EulerRotation(90.degrees, 180.degrees, (-90).degrees, EulerRotation.Config.UNITY).toQuaternion()
        )
        assertEqualsFloat(
            Quaternion(-.09230f, .43046f, .70106f, .56099f),
            EulerRotation((-45).degrees, 30.degrees, 90.degrees, EulerRotation.Config.UNITY).toQuaternion()
        )
        assertEqualsFloat(
            Quaternion(.23912f, -.36964f, .09905f, .89240f),
            EulerRotation(30.degrees, (-45).degrees, 0.degrees, EulerRotation.Config.UNITY).toQuaternion()
        )
    }

    @Test
    fun testFromEulerLibgdx() {
        assertEqualsFloat(
            Quaternion(-.5f, .5f, -.5f, -.5f),
            EulerRotation(90.degrees, 180.degrees, (-90).degrees, EulerRotation.Config.LIBGDX).toQuaternion()
        )
        assertEqualsFloat(
            Quaternion(-.09230f, .43046f, .70106f, .56099f),
            EulerRotation((-45).degrees, 30.degrees, 90.degrees, EulerRotation.Config.LIBGDX).toQuaternion()
        )
        assertEqualsFloat(
            Quaternion(.23912f, -.36964f, .09905f, .89240f),
            EulerRotation(30.degrees, (-45).degrees, 0.degrees, EulerRotation.Config.LIBGDX).toQuaternion()
        )
    }

    @Test
    fun testFromEulerThreejs() {
        assertEqualsFloat(
            Quaternion(-0.5f, 0.5f, 0.5f, 0.5f),
            EulerRotation(90.degrees, 180.degrees, (-90).degrees, EulerRotation.Config.THREEJS).toQuaternion()
        )
        assertEqualsFloat(
            Quaternion(-.09230f, .43046f, .56099f, .70106f),
            EulerRotation((-45).degrees, 30.degrees, 90.degrees, EulerRotation.Config.THREEJS).toQuaternion()
        )
        assertEqualsFloat(
            Quaternion(.23912f, -.36964f, -.09905f, .89240f),
            EulerRotation(30.degrees, (-45).degrees, 0.degrees, EulerRotation.Config.THREEJS).toQuaternion()
        )
    }

    //val quat = //Quaternion.lookRotation(Vector3.LEFT, Vector3.FORWARD)
    val quat = Quaternion(-0.5, 0.5, 0.5, 0.5)

    @Test
    fun testToFromEulerDefault() {
        assertEqualsFloat(quat, quat.toEuler(EulerRotation.Config.DEFAULT).toQuaternion())
    }

    @Test
    //@Ignore // Failing for now
    fun testToFromEulerUnity() {
        assertEqualsFloat(quat, quat.toEuler(EulerRotation.Config.UNITY).toQuaternion())
    }

    @Test
    //@Ignore // Failing for now
    fun testToFromEulerUnreal() {
        assertEqualsFloat(quat, quat.toEuler(EulerRotation.Config.UNREAL).toQuaternion())
    }

    @Test
    fun testToFromEulerLibgdx() {
        assertEqualsFloat(quat, quat.toEuler(EulerRotation.Config.LIBGDX).toQuaternion())
    }

    @Test
    fun testToFromEulerThreejs() {
        assertEqualsFloat(quat, quat.toEuler(EulerRotation.Config.THREEJS).toQuaternion())
    }

    @Test
    //@Ignore // Not passing because EulerRotation not working in ZXY-left
    fun testLookRotation() {
        assertEqualsFloat(
            EulerRotation(0.degrees, 270.degrees, 0.degrees),
            Quaternion.lookRotation(Vector3F.LEFT, Vector3F.UP).toEuler(EulerRotation.Config.UNITY).normalized(),
        )
        assertEqualsFloat(
            EulerRotation(0.degrees, (90).degrees, 0.degrees),
            Quaternion.lookRotation(Vector3F.RIGHT).toEuler(EulerRotation.Config.UNITY).normalized(),
        )
        assertEqualsFloat(Quaternion(0f, 0f, 0f, 1f), Quaternion.IDENTITY)
        assertEqualsFloat(Quaternion(0f, 1f, 0f, 0f), Quaternion.lookRotation(Vector3F.BACK))
        assertEqualsFloat(
            EulerRotation(0.degrees, (180).degrees, 0.degrees),
            Quaternion.lookRotation(Vector3F.BACK).toEuler(EulerRotation.Config.UNITY).normalized(),
        )
        //println("Quaternion.fromVectors(Vector3.UP, Vector3.FORWARD)=${Quaternion.fromVectors(Vector3.UP, Vector3.FORWARD)}")
        assertEqualsFloat(
            EulerRotation(90.degrees, 0.degrees, 0.degrees),
            Quaternion.fromVectors(Vector3F.UP, Vector3F.FORWARD).toEuler(EulerRotation.Config.UNITY).normalized(),
        )
    }

    @Test
    @Ignore // Not working for now
    fun testLookRotation2() {
        assertEqualsFloat(
            EulerRotation((-90).degrees, 0.degrees, 0.degrees),
            Quaternion.lookRotation(Vector3F.UP, Vector3F.DOWN).toEuler(EulerRotation.Config.UNITY).normalized(),
        )
        assertEqualsFloat(
            Quaternion(.5f, -.5f, -.5f, .5f),
            Quaternion.lookRotation(Vector3F.LEFT, Vector3F.FORWARD),
        )
        assertEqualsFloat(
            EulerRotation((-90).degrees, 0.degrees, 90.degrees),
            Quaternion.lookRotation(Vector3F.LEFT, Vector3F.FORWARD).toEuler(EulerRotation.Config.UNITY).normalized(),
        )
    }
}
