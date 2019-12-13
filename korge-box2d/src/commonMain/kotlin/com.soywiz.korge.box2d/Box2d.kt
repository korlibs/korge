package com.soywiz.korge.box2d

import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*
import org.jbox2d.collision.shapes.*
import org.jbox2d.common.*
import org.jbox2d.dynamics.*
import org.jbox2d.userdata.*
import kotlin.math.*

/**
 * Creates a [WorldView] that is a [View] with a Box2d [World] attached.
 *
 * See [WorldView] for more details.
 */
inline fun Container.worldView(world: World = World(Vec2(0f, -10f)), callback: WorldView.() -> Unit = {}): WorldView =
    WorldView(world).addTo(this).apply(callback)

/**
 * [View] with a Box2d [World] attached.
 *
 * This view will automatically update the [world] each frame.
 *
 * To adjust the simulation you can set:
 * * [WorldView.velocityIterations]
 * * [WorldView.positionIterations]
 *
 * Each [Body] can have a view attached.
 * You can attach a view to a body with [Body.setView] and obtain the view with [Body.view],
 * When calling [Body.setView] the specified view is attached to the [WorldView] on the next frame
 * and wrapped with a [Container] view (this [Container] will be the view updated (x, y and rotation)
 * along the body in each frame)
 *
 * Your [View.parent] will be the container wrapping, and [View.parent].pos would contain the body position.
 */
class WorldView(override val world: World = World(Vec2(0f, -10f))) : Container(), WorldRef {
    companion object {
        val WorldViewKey = Box2dTypedUserData.Key<WorldView>()
        val ViewKey = Box2dTypedUserData.Key<View>()
        val ViewContainerKey = Box2dTypedUserData.Key<Container>()
    }

    var velocityIterations = 6
    var positionIterations = 2
    var debugWorldViews = false

    init {
        world[WorldViewKey] = this
        addUpdatable {
            world.step(it.toFloat() / 1000f, velocityIterations = velocityIterations, positionIterations = positionIterations)
            updateViews()
        }
    }

    /**
     * Converts pixel-coordinates with (0,0) origin in the upper left to box2D World coordinates with (0,0) origin somewhere else in the view.
     * @param x X-Pixel-Coordinate
     * @param y Y-Pixel-Coordinate
     * @return Point with World Coordinates
     */
    fun convertPixelToWorld(x : Double, y : Double) = Point((x-this.x)/scaleX, -(y-this.y)/scaleY)

    /**
     * Converts pixel-coordinates as Point with (0,0) origin in the upper left to box2D World coordinates with (0,0) origin somewhere else in the view.
     * @param point Point with the Pixel-coordinates
     * @return Point with World Coordinates
     */
    fun convertPixelToWorld(point : Point) = convertPixelToWorld(point.x, point.y)

    /**
     * Converts box2d world-coordinates with (0,0) origin in the view to Pixel-Coordinates with (0,0) origin in the upper left
     * @param x X-World-Coordinate
     * @param y Y-World-Coordinate
     * @return Point with Pixel coordinates
     */
    fun convertWorldToPixel(x : Double, y: Double) = Point((x*scaleX)+this.x, -(y*this.scaleY)+this.y)

    /**
     * Converts box2d world-coordinates as Point with (0,0) origin in the view to Pixel-Coordinates with (0,0) origin in the upper left
     * @param point Point with world-coordinates
     * @return Point with pixel coordinates
     */
    fun convertWorldToPixel(point : Point) = convertWorldToPixel(point.x, point.y)

    fun updateViews() {
        world.forEachBody { node ->
            val view = node.viewContainerOrNull
            //println("view: $view")
            if (view != null) {
                if (view.parent != this) {
                    this.addChild(view)
                }
                view.x = node.position.x.toDouble()
                view.y = -node.position.y.toDouble()
                view.rotationRadians = -node.angle.toDouble()
            }
        }
    }

    override fun renderInternal(ctx: RenderContext) {
        val debugViews = stage?.views?.debugViews ?: false
        super.renderInternal(ctx)
        if (debugWorldViews || debugViews) {
            val debugLineRender = ctx.debugLineRenderContext
            debugLineRender.draw(globalMatrix) {
                world.forEachBody { body ->
                    body.forEachFixture { fixture ->
                        val shape = fixture.getShape()
                        if (shape != null) {
                            renderShape(debugLineRender, body, shape)
                        }
                    }
                }
            }
        }
    }

    private  val _p0 = Vec2()
    private  val _p1 = Vec2()
    private  val tempVec0 = Vec2()
    private  val tempVec1 = Vec2()

    private fun getCirclePoint(radius: Float, ratio: Float, out: Vec2 = Vec2()): Vec2 {
        val angle = ratio * PI.toFloat() * 2f
        return out.set(cos(angle) * radius, sin(angle) * radius)
    }

    private inline fun renderPoints(ctx: DebugLineRenderContext, body: Body, npoints: Int, getPoint: (n: Int) -> Vec2) {
        for (n in 0 until npoints) {
            val p0 = _p0.set(getPoint(n))
            val p1 = _p1.set(getPoint((n + 1) % npoints))
            body.getWorldPointToOut(p0, tempVec0)
            body.getWorldPointToOut(p1, tempVec1)
            ctx.line(tempVec0.x, -tempVec0.y, tempVec1.x, -tempVec1.y)
        }
    }

    private fun renderShape(ctx: DebugLineRenderContext, body: Body, shape: Shape) {
        when (shape) {
            is ChainShape -> {
            }
            is PolygonShape -> {
                renderPoints(ctx, body, shape.m_count) { n ->
                    shape.getVertex(n)
                }
            }
            is EdgeShape -> {
            }
            is CircleShape -> {
                val npoints = 64
                val radius = shape.m_radius
                renderPoints(ctx, body, npoints) { n ->
                    getCirclePoint(radius, n.toFloat() / (npoints - 1), tempVec0)
                }

            }
        }
    }
}

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

/** Shortcut to create and attach a [Body] to this [World] with the specified [body], [fixture] and attaching the [view] */
inline fun WorldRef.createBody(body: BodyDef, fixture: FixtureDef, view: View): Body {
    val rbody = world.createBody(body)
    rbody.createFixture(fixture)
    rbody.view = view
    return rbody
}

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
    view: View? = null,
    fixture: FixtureDef.() -> Unit = {},
    body: BodyDef.() -> Unit = {}
): Body = createBody {
    this.type = type
    this.angle = angle.radians.toFloat()
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
    if (view != null) {
        it.setView(view)
    }
}

/** Shortcut to create and attach a [Fixture] to this [Body] */
inline fun Body.fixture(callback: FixtureDef.() -> Unit): Body = this.also { createFixture(FixtureDef().apply(callback)) }

/**
 * Creates a container and sets the View created with [builder]
 *
 * @sample
 * createBody {
 *   setPosition(0, -10)
 * }.fixture {
 *   shape = BoxShape(100, 20)
 *   density = 0f
 * }.setView {
 *   solidRect(100, 20, Colors.RED).position(-50, -10).interactive()
 * }
 */
inline fun Body.setView(builder: Container.() -> View): Body = this.also { it.view = builder(viewContainer) }

@Deprecated("Do not use this method anymore. setView will also create the container.", ReplaceWith("this.setView(view)"))
inline fun Body.setViewWithContainer(view: View): Body = this.setView(view)

/**
 * Sets the [view] to this [Body] while still returning [this] body.
 */
inline fun Body.setView(view: View): Body = this.also { it.view = view }

/**
 * Creates a [PolygonShape] as a box with the specified [width] and [height]
 */
inline fun BoxShape(width: Number, height: Number) = PolygonShape().apply { setAsBox(width.toFloat() / 2, height.toFloat() / 2) }

inline fun BodyDef.setPosition(x: Number, y: Number) = position.set(x.toFloat(), y.toFloat())

/** Returns the [WorldView] attached to this [World] or null if this World doesn't have a view attached */
val WorldRef.worldView: WorldView? get() = this.world[WorldView.WorldViewKey]

val Body.viewContainerOrNull: Container?
    get() = this[WorldView.ViewContainerKey]

val Body.viewContainer: Container
    get() {
        if (WorldView.ViewContainerKey !in this) this[WorldView.ViewContainerKey] = Container()
        return this[WorldView.ViewContainerKey]!!
    }

var Body.view: View?
    set(view) = run {
        if (this[WorldView.ViewKey] != view) {
            this[WorldView.ViewKey] = view
            viewContainer.removeChildren()
            if (view != null) {
                viewContainer.addChild(view)
            }
        }
    }
    get() = this[WorldView.ViewKey]
