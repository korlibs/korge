//import com.soywiz.korge.admob.*
import com.soywiz.kds.*
import com.soywiz.korge.*
import com.soywiz.korge.box2d.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import org.jbox2d.collision.shapes.*
import org.jbox2d.common.*
import org.jbox2d.dynamics.*
import org.jbox2d.userdata.*

suspend fun main() = Korge(width = 920, height = 720, quality = GameWindow.Quality.PERFORMANCE, title = "My Awesome Box2D Game!") {
    registerBox2dSupportOnce()
    addChild(resourcesVfs["restitution.ktree"].readKTree(views))
}

/*
suspend fun main() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "My Awesome Box2D Game!") {
    box2dWorldView(gravityY = 100.0) {
        solidRect(20, 20, Colors.RED).position(100, 100).rotation(30.degrees).registerBody(dynamic = true, density = 2, friction = 0.01)
        solidRect(20, 20, Colors.RED).position(109, 75).registerBody(dynamic = true)
        solidRect(20, 20, Colors.RED).position(93, 50).rotation((-15).degrees).registerBody(dynamic = true)
        solidRect(400, 100, Colors.WHITE).position(0, 300).registerBody(dynamic = false, friction = 0.2)
    }

    val world2 = box2dWorldView(gravityY = 100.0) {
        position(400, 0)
        solidRect(20, 20, Colors.RED).position(150, 100).rotation(30.degrees).registerBody(dynamic = true, density = 2, friction = 0.01)
        solidRect(20, 20, Colors.RED).position(109, 75).registerBody(dynamic = true)
        solidRect(20, 20, Colors.RED).position(93, 50).rotation((-15).degrees).registerBody(dynamic = true)
        solidRect(400, 100, Colors.WHITE).position(0, 300).registerBody(dynamic = false, friction = 0.2)
    }

    world2.apply {
        solidRect(20, 20, Colors.RED).position(150, 100).rotation(30.degrees).registerBody(dynamic = true, density = 2, friction = 0.01)
    }
}
 */

///////////////////////////////
// Core
///////////////////////////////

private val BOX2D_WORLD_KEY = "box2dWorld"
private val BOX2D_BODY_KEY = "box2dBody"

class NewWorldView(override val world: World, val velocityIterations: Int, val positionIterations: Int) : Container(), WorldRef {
    companion object {
        val WorldViewKey = Box2dTypedUserData.Key<NewWorldView>()
        val ViewKey = Box2dTypedUserData.Key<View>()
        val ViewContainerKey = Box2dTypedUserData.Key<Container>()
    }


    fun <T : View> T.registerBody(
        def: BodyDefinition,
        world: WorldRef = this.getOrCreateWorldRef()
    ): T = this.apply {
    }

    fun <T : View> T.registerBody(
        density: Number = 0.0f,
        friction: Number = 0.2f,
        dynamic: Boolean = true
    ): T = this.apply {
        val view = this
        val body = world.createBody {
            this.type = if (dynamic) BodyType.DYNAMIC else BodyType.STATIC
            this.setPosition(x, y)
            this.angle = view.rotation
        }.fixture {
            this.shape = BoxShape(width, height)
            this.density = density.toFloat()
            this.friction = friction.toFloat()
        }
        body[NewWorldView.ViewKey] = view
        view.setExtra(BOX2D_BODY_KEY, body)
    }

    init {
        addUpdater {
            world.step(it.seconds.toFloat(), velocityIterations, positionIterations)
            world.forEachBody { node ->
                val px = node.position.x.toDouble()
                val py = node.position.y.toDouble()
                val view = node[NewWorldView.ViewKey]
                if (view != null) {
                    view.x = px
                    view.y = py
                    view.rotation = node.angle
                }
                //println(node.position)
            }
        }
    }
}

fun Container.box2dWorldView(gravityX: Double = 0.0, gravityY: Double = 100.0, velocityIterations: Int = 6, positionIterations: Int = 2, block: NewWorldView.() -> Unit): NewWorldView {
    return NewWorldView(World(Vec2(gravityX.toFloat(), gravityY.toFloat())), velocityIterations, positionIterations).addTo(this).also(block)
}

fun createWorld(rootView: View, gravityX: Double = 0.0, gravityY: Double = 100.0, velocityIterations: Int = 6, positionIterations: Int = 2): World {
    return World(Vec2(gravityX.toFloat(), gravityY.toFloat())).also { world ->
        rootView.addUpdater {
            world.step(it.seconds.toFloat(), velocityIterations, positionIterations)
            world.forEachBody { node ->
                val px = node.position.x.toDouble()
                val py = node.position.y.toDouble()
                val view = node[NewWorldView.ViewKey]
                if (view != null) {
                    view.x = px
                    view.y = py
                    view.rotation = node.angle
                }
                //println(node.position)
            }
        }
    }
}

fun View.getOrCreateWorldRef(): WorldRef {
    var current: View? = this
    while (current != null) {
        if (current is WorldRef) {
            return current
        }
        current = current.parent
    }

    val root = this.root
    val world = root.getExtraTyped<World>(BOX2D_WORLD_KEY)
    if (world == null) {
        val newWorld = createWorld(root)
        root.setExtra(BOX2D_WORLD_KEY, newWorld)
        return newWorld
    } else {
        return world
    }
}

data class BodyDefinition(
    val density: Number = 0.5f,
    val friction: Number = 0.2f,
    val dynamic: Boolean = true
)

val View.body: Body?
    get() = this.getExtraTyped(BOX2D_BODY_KEY)






// @TODO: Move to Box2d project
private fun Body.forEachFixture(callback: (fixture: Fixture) -> Unit) {
    var node = m_fixtureList
    while (node != null) {
        callback(node)
        node = node.m_next
    }
}

/** Shortcut to create a new [BodyDef] */
inline fun bodyDef(callback: BodyDef.() -> Unit): BodyDef = BodyDef().apply(callback)

/** Shortcut to create a new [BodyDef] */
inline fun fixtureDef(callback: FixtureDef.() -> Unit): FixtureDef = FixtureDef().apply(callback)

/** Shortcut to create and attach a [Body] to this [World] */
inline fun WorldRef.createBody(callback: BodyDef.() -> Unit): Body = world.createBody(BodyDef().apply(callback))

@PublishedApi
internal val defaultBoxShape = BoxShape(10, 10)

/** Shortcut to create a simple [Body] to this [World] with the specified properties. You can specify a [body] and a [fixture] callbacks to manually set */
inline fun WorldRef.createSimpleBody(
    x: Number = 0.0,
    y: Number = 0.0,
    angle: Angle = 0.degrees,
    angularVelocity: Number = 0.0,
    linearVelocityX: Number = 0.0,
    linearVelocityY: Number = 0.0,
    linearDamping: Number = 0.0,
    angularDamping: Number = 0.0,
    gravityScale: Number = 1.0,
    shape: Shape = defaultBoxShape,
    allowSleep: Boolean = true,
    awake: Boolean = true,
    fixedRotation: Boolean = false,
    bullet: Boolean = false,
    type: BodyType = BodyType.STATIC,
    friction: Number = 0.2,
    restitution: Number = 0.0,
    density: Number = 0.0,
    fixture: FixtureDef.() -> Unit = {},
    body: BodyDef.() -> Unit = {}
): Body = createBody {
    this.type = type
    this.angle = angle
    this.angularVelocity = angularVelocity.toFloat()
    this.position.set(x.toFloat(), y.toFloat())
    this.linearVelocity.set(linearVelocityX.toFloat(), linearVelocityY.toFloat())
    this.linearDamping = linearDamping.toFloat()
    this.angularDamping = angularDamping.toFloat()
    this.gravityScale = gravityScale.toFloat()
    this.allowSleep = allowSleep
    this.fixedRotation = fixedRotation
    this.bullet = bullet
    this.awake = awake
    body(this)
}.also {
    it.fixture {
        this.shape = shape
        this.friction = friction.toFloat()
        this.restitution = restitution.toFloat()
        this.density = density.toFloat()
        fixture(this)
    }
}

/** Shortcut to create and attach a [Fixture] to this [Body] */
inline fun Body.fixture(callback: FixtureDef.() -> Unit): Body = this.also { createFixture(FixtureDef().apply(callback)) }

/**
 * Creates a [PolygonShape] as a box with the specified [width] and [height]
 */
inline fun BoxShape(width: Number, height: Number) = PolygonShape().apply {
    count = 4
    vertices[0].set(0, 0)
    vertices[1].set(width, 0)
    vertices[2].set(width, height)
    vertices[3].set(0, height)
    normals[0].set(0.0f, -1.0f)
    normals[1].set(1.0f, 0.0f)
    normals[2].set(0.0f, 1.0f)
    normals[3].set(-1.0f, 0.0f)
    centroid.setZero()
}

inline fun BodyDef.setPosition(x: Number, y: Number) = position.set(x.toFloat(), y.toFloat())
