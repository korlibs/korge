package com.soywiz.korge.box2d

import com.soywiz.kds.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*
import org.jbox2d.collision.shapes.*
import org.jbox2d.common.*
import org.jbox2d.dynamics.*
import org.jbox2d.userdata.*
import kotlin.math.*

inline fun Container.worldView(
    world: World = World(Vec2(0f, 9.8f)),
    velocityIterations: Int = 6,
    positionIterations: Int = 2,
    callback: @ViewDslMarker WorldView.() -> Unit = {}
): WorldView = WorldView(world, velocityIterations, positionIterations).addTo(this, callback)

inline fun Container.worldView(
    gravityX: Double = 0.0,
    gravityY: Double = 9.8,
    velocityIterations: Int = 6,
    positionIterations: Int = 2,
    callback: @ViewDslMarker WorldView.() -> Unit = {}
): WorldView = WorldView(World(Vec2(gravityX.toFloat(), gravityY.toFloat())), velocityIterations, positionIterations).addTo(this, callback)

class WorldView(
    override val world: World,
    var velocityIterations: Int = 6,
    var positionIterations: Int = 2
) : Container(), WorldRef {

    companion object {
        val ViewKey = Box2dTypedUserData.Key<View>()

        val BOX2D_BODY_KEY = "box2dBody"
    }

    var debugWorldViews = false

    init {
        addUpdater {
            world.step(it.seconds.toFloat(), velocityIterations, positionIterations)
            world.forEachBody { node ->
                val px = node.position.x.toDouble()
                val py = node.position.y.toDouble()
                val view = node[ViewKey]
                if (view != null) {
                    view.x = px
                    view.y = py
                    view.rotation = node.angle
                }
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

    private fun renderShape(ctx: DebugLineRenderContext, body: Body, shape: Shape) {
        when (shape) {
            is ChainShape -> Unit
            is PolygonShape -> {
                renderPoints(ctx, body, shape.count) { n ->
                    shape.getVertex(n)
                }
            }
            is EdgeShape -> Unit
            is CircleShape -> {
                val npoints = 64
                val radius = shape.radius
                renderPoints(ctx, body, npoints) { n ->
                    getCirclePoint(radius, n.toFloat() / (npoints - 1), tempVec0)
                }
            }
        }
    }

    private fun getCirclePoint(radius: Float, ratio: Float, out: Vec2 = Vec2()): Vec2 {
        val angle = ratio * PI.toFloat() * 2f
        return out.set(cos(angle) * radius, sin(angle) * radius)
    }

    private  val _p0 = Vec2()
    private  val _p1 = Vec2()
    private  val tempVec0 = Vec2()
    private  val tempVec1 = Vec2()

    private inline fun renderPoints(ctx: DebugLineRenderContext, body: Body, npoints: Int, getPoint: (n: Int) -> Vec2) {
        for (n in 0 until npoints) {
            val p0 = _p0.set(getPoint(n))
            val p1 = _p1.set(getPoint((n + 1) % npoints))
            body.getWorldPointToOut(p0, tempVec0)
            body.getWorldPointToOut(p1, tempVec1)
            ctx.line(tempVec0.x, tempVec0.y, tempVec1.x, tempVec1.y)
        }
    }

    inline fun createBody(callback: BodyDef.() -> Unit): Body = world.createBody(BodyDef().apply(callback))

    /** Shortcut to create and attach a [Fixture] to this [Body] */
    inline fun Body.fixture(callback: FixtureDef.() -> Unit): Body = this.also { createFixture(FixtureDef().apply(callback)) }

    inline fun <T : View> T.registerBody(body: Body): T {
        body[ViewKey] = this
        setExtra(BOX2D_BODY_KEY, body)
        return this
    }

    val View.body: Body?
        get() = this.getExtraTyped(BOX2D_BODY_KEY)

    /** Shortcut to create a simple [Body] to this [World] with the specified properties */
    inline fun <T : View> T.registerBodyWithFixture(
        angularVelocity: Number = 0.0,
        linearVelocityX: Number = 0.0,
        linearVelocityY: Number = 0.0,
        linearDamping: Number = 0.0,
        angularDamping: Number = 0.0,
        gravityScale: Number = 1.0,
        shape: Shape = BoxShape(width, height),
        allowSleep: Boolean = true,
        awake: Boolean = true,
        fixedRotation: Boolean = false,
        bullet: Boolean = false,
        type: BodyType = BodyType.STATIC,
        friction: Number = 0.2,
        restitution: Number = 0.0,
        density: Number = 0.0
    ): T {
        val body = createBody {
            this.type = type
            this.angle = rotation
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
        }
        body.fixture {
            this.shape = shape
            this.friction = friction.toFloat()
            this.restitution = restitution.toFloat()
            this.density = density.toFloat()
        }
        body[ViewKey] = this
        setExtra(BOX2D_BODY_KEY, body)
        return this
    }

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
}
