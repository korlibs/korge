import com.soywiz.korge.*
import com.soywiz.korge.box2d.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.paint.*
import com.soywiz.korma.geom.vector.*
import org.jbox2d.collision.shapes.*
import org.jbox2d.common.*
import org.jbox2d.dynamics.*

suspend fun main() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "My Awesome Box2D Game!") {
    views.registerBox2dSupportOnce()
    views.clearColor = Colors.DARKGREEN

    //speed = 0.1
    worldView(velocityIterations = 6 * 20, positionIterations = 2 * 20, gravityY = 2000.0) {
        //scale(20.0)

        //val view1 = solidRect(100, 20, Colors.RED).position(0, 20).interactive()
        //view1.registerBodyWithFixture()

        val view2 = solidRect(2 * 20, 2 * 20, Colors.GREEN).interactive()
        view2.registerBody(createBody {
            type = BodyType.DYNAMIC
            position = Vec2(20f * 20, 5f * 20)
        }.fixture {
            shape = BoxShape(2 * 20, 2 * 20)
            density = 0.5f * 20
            friction = 0.2f * 20
        })

        val view3 = sgraphics {
            fill(Colors.BLUE) {
                rect(0.0, 0.0, 2.0 * 20, 2.0 * 20)
            }
        }.position(21 * 20, 10 * 20).interactive()
        view3.registerBodyWithFixture(
            type = BodyType.DYNAMIC,
            density = 1f * 20,
            friction = 0.2f * 20
        )

        val view4 = sgraphics {
            position(20 * 20, 10 * 20)
            fillStroke(ColorPaint(Colors.BLUE), ColorPaint(Colors.RED), Context2d.StrokeInfo(thickness = 0.3)) {
                circle(0.0, 0.0, 2.0 * 20)
                //rect(0, 0, 400, 20)
            }
            fill(Colors.DARKCYAN) {
                circle(1.0 * 20, 1.0 * 20, 0.2 * 20)
            }
            hitTestUsingShapes = true
        }
        view4.registerBodyWithFixture(
            type = BodyType.DYNAMIC,
            shape = CircleShape(2.0 * 20),
            density = 22f * 20,
            friction = 3f * 20,
        )
    }
}

fun <T : View> T.interactive(): T = apply {
    alpha = 0.5
    onOver { alpha = 1.0 }
    onOut { alpha = 0.5 }
}
