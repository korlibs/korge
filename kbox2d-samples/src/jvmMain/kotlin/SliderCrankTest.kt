import com.soywiz.korge.*
import com.soywiz.korge.box2d.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.vector.*
import org.jbox2d.collision.shapes.*
import org.jbox2d.common.*
import org.jbox2d.dynamics.*
import org.jbox2d.dynamics.joints.*

suspend fun main() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "My Awesome Box2D Game!") {
    worldView {
        debugWorldViews = true
        scale = 10.0
        position(200, 200)

        val ground = sgraphics {
            position(10, 0)
            stroke(Colors.WHITE, Context2d.StrokeInfo(0.2)) {
                line(-40.0, 0.0, 40.0, 0.0)
            }
        }.registerBodyWithFixture(
            shape = EdgeShape(-40, 0, 40f, 0f)
        )

        var prevBody = ground.body!!

        val obj1 = solidRect(1.0, 4.0, Colors.GREEN) {
            position(10.0, -7.0)
        }.registerBodyWithFixture(
            density = 2.0,
            type = BodyType.DYNAMIC
        )

        val joint1 = RevoluteJointDef()
        joint1.initialize(prevBody, obj1.body!!, Vec2(0f, 5f))
        joint1.motorSpeed = 1.0f * MathUtils.PI
        joint1.maxMotorTorque = 10000f
        joint1.enableMotor = true
        world.createJoint(joint1)

        prevBody = obj1.body!!

        val obj2 = solidRect(1.0, 8.0, Colors.GREENYELLOW) {
            position(10.0, -13.0)
        }.registerBodyWithFixture(
            density = 2.0,
            type = BodyType.DYNAMIC
        )

        val joint2 = RevoluteJointDef()
        joint2.initialize(prevBody, obj2.body!!, Vec2(0f, 9f))
        joint2.enableMotor = false
        world.createJoint(joint2)

        prevBody = obj2.body!!

        val obj3 = solidRect(3.0, 3.0, Colors.DARKGREEN) {
            position(10.0, -17.0)
        }.registerBodyWithFixture(
            fixedRotation = true,
            type = BodyType.DYNAMIC,
            density = 2
        )

        val joint3 = RevoluteJointDef()
        joint3.initialize(prevBody, obj3.body!!, Vec2(0f, 17f))
        world.createJoint(joint3)

        val joint4 = PrismaticJointDef()
        joint4.initialize(ground.body!!, obj3.body!!, Vec2(0f, 17f), Vec2(0f, 1f))
        joint4.maxMotorForce = 1000f
        joint4.enableMotor = false
        world.createJoint(joint4)

        /*solidRect(3.0, 3.0, Colors.WHITE) {
            position(10, 23)
        }.registerBodyWithFixture(
            type = BodyType.DYNAMIC,
            density = 2.0
        )*/
    }
}
