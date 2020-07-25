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
    views.clearColor = Colors.DARKGREEN

    worldView {
        scale(20.0)

        val view1 = solidRect(100, 20, Colors.RED).position(0, 20).interactive()
        view1.registerBodyWithFixture()

        val view2 = solidRect(2, 2, Colors.GREEN).interactive()
        view2.registerBody(createBody {
            type = BodyType.DYNAMIC
            position = Vec2(20f, 5f)
        }.fixture {
            shape = BoxShape(2, 2)
            density = 0.5f
            friction = 0.2f
        })

        val view3 = sgraphics {
            fill(Colors.BLUE) {
                rect(0.0, 0.0, 2.0, 2.0)
            }
        }.position(21, 10).interactive()
        view3.registerBodyWithFixture(
            type = BodyType.DYNAMIC,
            density = 1f,
            friction = 0.2f
        )

        val view4 = sgraphics {
            position(20, 15)
            fillStroke(ColorPaint(Colors.BLUE), ColorPaint(Colors.RED), Context2d.StrokeInfo(thickness = 0.3)) {
                circle(0.0, 0.0, 2.0)
                //rect(0, 0, 400, 20)
            }
            fill(Colors.DARKCYAN) {
                circle(1.0, 1.0, 0.2)
            }
            hitTestUsingShapes = true
        }
        view4.registerBodyWithFixture(
            type = BodyType.DYNAMIC,
            shape = CircleShape(2.0),
            density = 22f,
            friction = 3f,
        )
    }
}

fun <T : View> T.interactive(): T = apply {
    alpha = 0.5
    onOver { alpha = 1.0 }
    onOut { alpha = 0.5 }
}
