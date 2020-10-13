import com.soywiz.korge.*
import com.soywiz.korge.box2d.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.vector.*
import org.jbox2d.collision.shapes.*
import org.jbox2d.dynamics.*

suspend fun main() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "My Awesome Box2D Game!") {
    worldView {
        //debugWorldViews = true
        scale = 20.0

        sgraphics {
            position(40, 20)
            stroke(Colors.WHITE, Context2d.StrokeInfo(0.2)) {
                line(-40.0, 0.0, 40.0, 0.0)
            }
        }.registerBodyWithFixture(
            shape = EdgeShape(-40, 0, 40f, 0f),
        )

        arrayOf(
            0.0f, 0.1f, 0.3f, 0.5f, 0.75f, 0.9f, 1.0f
        ).forEachIndexed { i, restitution ->
            circle(1.0, Colors.YELLOWGREEN) {
                position(10.0 + 3.0 * i, 0.0)
                centered
            }.registerBodyWithFixture(
                shape = CircleShape(1.0),
                density = 1.0,
                type = BodyType.DYNAMIC,
                restitution = restitution
            )
        }
    }
}
