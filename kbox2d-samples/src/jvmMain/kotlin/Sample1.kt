import com.soywiz.korge.*
import com.soywiz.korge.box2d.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import org.jbox2d.dynamics.*

suspend fun main() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "My Awesome Box2D Game!") {
    worldView {
        solidRect(20, 20, Colors.RED).position(100, 100).rotation(30.degrees)
            .registerBodyWithFixture(type = BodyType.DYNAMIC, density = 2, friction = 0.01)
        solidRect(20, 20, Colors.RED).position(109, 75)
            .registerBodyWithFixture(type = BodyType.DYNAMIC)
        solidRect(20, 20, Colors.RED).position(93, 50).rotation((-15).degrees)
            .registerBodyWithFixture(type = BodyType.DYNAMIC)
        solidRect(400, 100, Colors.WHITE).position(0, 300)
            .registerBodyWithFixture(friction = 0.2)
    }
}
