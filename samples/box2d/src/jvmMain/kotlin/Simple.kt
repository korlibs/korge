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
    worldView(gravityX = 0.0, gravityY = 10.0) {
        debugWorldViews = true
        scale = 15.0
        position(100, 100)

        sgraphics {
            position(0, 20)
            stroke(Colors.RED, Context2d.StrokeInfo(0.2)) {
                line(0.0, 0.0, 40.0, 0.0)
            }
        }.registerBodyWithFixture(
            shape = EdgeShape(0f, 0f, 40f, 0f)
        )

        solidRect(8, 8, Colors.GREEN)
            .position(10, 0)
            .registerBodyWithFixture(
                type = BodyType.DYNAMIC,
                density = 10f
            )
    }
}
