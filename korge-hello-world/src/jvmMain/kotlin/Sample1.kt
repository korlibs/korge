import com.soywiz.korge.*
import com.soywiz.korge.box2d.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import org.jbox2d.dynamics.*

suspend fun main() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "My Awesome Box2D Game!") {
    worldView {
        solidRect(20, 20, Colors.RED).position(100, 100).centered.rotation(30.degrees)
            .registerBodyWithFixture(type = BodyType.DYNAMIC, density = 2, friction = 0.01)
        solidRect(20, 20, Colors.RED).position(109, 75).centered
            .registerBodyWithFixture(type = BodyType.DYNAMIC)
        solidRect(20, 20, Colors.RED).position(93, 50).centered.rotation((-15).degrees)
            .registerBodyWithFixture(type = BodyType.DYNAMIC)
        solidRect(400, 100, Colors.WHITE).position(100, 300).centered
            .registerBodyWithFixture(friction = 0.2)
    }
}
