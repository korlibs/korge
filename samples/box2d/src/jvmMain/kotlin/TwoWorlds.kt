import com.soywiz.korge.*
import com.soywiz.korge.box2d.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import org.jbox2d.dynamics.*

suspend fun main() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "My Awesome Box2D Game!") {
    worldView(gravityY = 100.0) {
        solidRect(20, 20, Colors.RED).position(100, 100).rotation(30.degrees).registerBodyWithFixture(type = BodyType.DYNAMIC, density = 2, friction = 0.01)
        solidRect(20, 20, Colors.RED).position(109, 75).registerBodyWithFixture(type = BodyType.DYNAMIC)
        solidRect(20, 20, Colors.RED).position(93, 50).rotation((-15).degrees).registerBodyWithFixture(type = BodyType.DYNAMIC)
        solidRect(400, 100, Colors.WHITE).position(0, 300).registerBodyWithFixture(type = BodyType.STATIC, friction = 0.2)
    }

    val world2 = worldView(gravityY = 100.0) {
        position(410, 0)
        solidRect(20, 20, Colors.RED).position(150, 100).rotation(30.degrees).registerBodyWithFixture(type = BodyType.DYNAMIC, density = 2, friction = 0.01)
        solidRect(20, 20, Colors.RED).position(109, 75).registerBodyWithFixture(type = BodyType.DYNAMIC)
        solidRect(20, 20, Colors.RED).position(93, 50).rotation((-15).degrees).registerBodyWithFixture(type = BodyType.DYNAMIC)
        solidRect(400, 100, Colors.WHITE).position(0, 300).registerBodyWithFixture(type = BodyType.STATIC, friction = 0.2)
    }

    world2.apply {
        solidRect(20, 20, Colors.RED).position(150, 120).rotation(30.degrees).registerBodyWithFixture(type = BodyType.DYNAMIC, density = 2, friction = 0.01)
    }
}
