package com.soywiz.korge.box2d

import com.soywiz.kds.*
import com.soywiz.klock.hr.*
import com.soywiz.korge.component.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import org.jbox2d.collision.shapes.*
import org.jbox2d.common.*
import org.jbox2d.dynamics.*
import org.jbox2d.userdata.*
import kotlin.math.*

@PublishedApi
internal val DEFAULT_SCALE = 20.0
@PublishedApi
internal val DEFAULT_GRAVITY_Y = 9.8f

var Views.registeredBox2dSupport: Boolean by Extra.Property { false }

fun Views.checkBox2dRegistered() {
    if (!registeredBox2dSupport) error("You should call Views.registerBox2dSupport()")
}

var KTreeSerializer.box2dWorld by Extra.PropertyThis<KTreeSerializer, Box2dWorldComponent?> { null }

object PhysicsKTreeSerializerExtension : KTreeSerializerExtension("physics") {
    override fun complete(serializer: KTreeSerializer, view: View) {
        //serializer.box2dWorld?.world?.forEachBody { println("it.linearVelocityY: ${it.linearVelocityY}") }
        serializer.box2dWorld?.update(0.hrSeconds)
        serializer.box2dWorld?.world?.forEachBody {
            if (!it.didReset) {
                it.didReset = true
                it.type = it.bodyDef.type
                it.linearVelocityX = it.bodyDef.linearVelocity.x
                it.linearVelocityY = it.bodyDef.linearVelocity.y
                it.gravityScale = it.bodyDef.gravityScale
                it.angularVelocity = it.bodyDef.angularVelocity
                it.isSleepingAllowed = it.bodyDef.allowSleep
                it.isAwake = it.bodyDef.awake
                it.isFixedRotation = it.bodyDef.fixedRotation
                it.isBullet = it.bodyDef.bullet
            }
            //println("it.linearVelocityY: ${it.linearVelocityY}")
        }
    }

    override fun setProps(serializer: KTreeSerializer, view: View, xml: Xml) {
        //println("PhysicsKTreeSerializerExtension.setProps")
        val body = view.registerBodyWithFixture(
            world = serializer.box2dWorld?.world,
            type = xml.strNull("type")?.let { BodyType[it] } ?: BodyType.STATIC,
            linearVelocityX = xml.float("linearVelocityX", 0f),
            linearVelocityY = xml.float("linearVelocityY", 0f),
            gravityScale = xml.float("gravityScale", 1f),
            angularVelocity = xml.float("angularVelocity", 0f),
            allowSleep = xml.boolean("isSleepingAllowed", true),
            awake = xml.boolean("isAwake", true),
            fixedRotation = xml.boolean("isFixedRotation", false),
            bullet = xml.boolean("isBullet", false),
            friction = xml.float("friction", 0f),
            density = xml.float("density", 1f),
            restitution = xml.float("restitution", 0f),
            isSensor = xml.boolean("isSensor", false),
            active = xml.boolean("isActive", true)
        ).body
        body?.didReset = false
    }

    override fun getProps(serializer: KTreeSerializer, view: View): Map<String, Any?>? {
        val body = view.body ?: return null
        val fixture = body.m_fixtureList
        //println("PhysicsKTreeSerializerExtension.getProps")
        return LinkedHashMap<String, Any?>().apply {
            if (body.type != BodyType.STATIC) this["type"] = body.type
            if (body.linearVelocityX != 0f) this["linearVelocityX"] = body.linearVelocityX
            if (body.linearVelocityY != 0f) this["linearVelocityY"] = body.linearVelocityY
            if (body.gravityScale != 1f) this["gravityScale"] = body.gravityScale
            if (body.angularVelocity != 0f) this["angularVelocity"] = body.angularVelocity
            if (!body.isSleepingAllowed) this["isSleepingAllowed"] = body.isSleepingAllowed
            if (!body.isAwake) this["isAwake"] = body.isAwake
            if (body.isFixedRotation) this["isFixedRotation"] = body.isFixedRotation
            if (body.isBullet) this["isBullet"] = body.isBullet
            if (!body.isActive) this["isActive"] = body.isActive
            if (fixture != null) {
                if (fixture.isSensor) this["isSensor"] = fixture.isSensor
                if (fixture.friction != 0f) this["friction"] = fixture.friction
                if (fixture.density != 1f) this["density"] = fixture.density
                if (fixture.restitution != 0f) this["restitution"] = fixture.restitution
            }
        }
    }
}
fun ViewsContainer.registerBox2dSupportOnce() {
    if (views.registeredBox2dSupport) return
    views.registeredBox2dSupport = true
    views.serializer.registerExtension(PhysicsKTreeSerializerExtension)
    views.viewExtraBuildDebugComponent.add { views, view, container ->
        val physicsContainer = container.container {
        }
        fun physicsContainer() {
            physicsContainer.removeChildren()
            val body = view.body
            if (body != null) {
                physicsContainer.uiCollapsableSection("Box2D Physics") {
                    button("Remove") {
                        body.destroyBody()
                        view.body = null
                        body.view = null
                        physicsContainer()
                    }
                    uiEditableValue(body::type, values = { listOf(BodyType.STATIC, BodyType.DYNAMIC, BodyType.KINEMATIC) })
                    val fixture = body.m_fixtureList
                    if (fixture != null) {
                        uiEditableValue(fixture::isSensor)
                        uiEditableValue(fixture::friction)
                        uiEditableValue(fixture::density, min = 0f, clampMin = true, clampMax = false)
                        uiEditableValue(fixture::restitution)
                    }
                    uiEditableValue(body::linearVelocityX, min = -100f, max = 100f, clampMin = true, clampMax = false)
                    uiEditableValue(body::linearVelocityY, min = -100f, max = 100f, clampMin = true, clampMax = false)
                    uiEditableValue(body::gravityScale, min = -100f, max = 100f, clampMin = true, clampMax = false)
                    uiEditableValue(body::angularVelocity)
                    uiEditableValue(body::isSleepingAllowed)
                    uiEditableValue(body::isAwake)
                    uiEditableValue(body::isFixedRotation)
                    uiEditableValue(body::isBullet)
                    uiEditableValue(body::isActive)
                }
            } else {
                physicsContainer.button("Add box2d physics") {
                    view.registerBodyWithFixture(type = BodyType.STATIC)
                    views.debugSaveView("Add physics", view)
                    physicsContainer()
                }
            }
            physicsContainer.root?.relayout()
        }
        physicsContainer()
    }
    //views.serializer.register()
}

inline fun Container.worldView(
    world: World = World(Vec2(0f, DEFAULT_GRAVITY_Y)),
    velocityIterations: Int = 6,
    positionIterations: Int = 2,
    callback: @ViewDslMarker WorldView.() -> Unit = {}
): WorldView = WorldView(world, velocityIterations, positionIterations).addTo(this, callback)

inline fun Container.worldView(
    gravityX: Double = 0.0,
    gravityY: Double = DEFAULT_GRAVITY_Y.toDouble(),
    velocityIterations: Int = 6,
    positionIterations: Int = 2,
    callback: @ViewDslMarker WorldView.() -> Unit = {}
): WorldView = WorldView(World(Vec2(gravityX.toFloat(), gravityY.toFloat())), velocityIterations, positionIterations).addTo(this, callback)

var World.component: Box2dWorldComponent?
    get() = get(Box2dWorldComponent.Key)
    set(value) {
        set(Box2dWorldComponent.Key, value)
    }

class Box2dWorldComponent(
    val worldView: View,
    override val world: World,
    var velocityIterations: Int = 6,
    var positionIterations: Int = 2,
    var autoDestroyBodies: Boolean = true,
) : FixedUpdateComponent(worldView, 16.hrMilliseconds), WorldRef {
    init {
        world.component = this
    }

    object Key : Box2dTypedUserData.Key<Box2dWorldComponent>()

    private val tempVec = Vec2()
    private val tempPos = Point()
    override fun update() {
        world.step(step.secondsDouble.toFloat(), velocityIterations, positionIterations)
        world.forEachBody { node ->
            val view = node.view

            if (view != null) {
                val worldScale = world.customScale
                val worldScaleInv = 1.0 / worldScale

                //val viewPos = view.getPositionRelativeTo(worldView, tempPos)
                val viewPos = tempPos.setTo(view.x, view.y)

                if (viewPos.x != node.viewInfo.x || viewPos.y != node.viewInfo.y || view.rotation != node.viewInfo.rotation) {
                    node.setTransform(
                        tempVec.set(viewPos.x * worldScaleInv, viewPos.y * worldScaleInv),
                        view.rotation
                    )
                    node.linearVelocity = tempVec.set(0f, 0f)
                    node.angularVelocity = 0f
                    node.isActive = true
                    node.isAwake = true
                }

                val newX = node.position.x.toDouble() * worldScale
                val newY = node.position.y.toDouble() * worldScale

                view.position(newX, newY)
                //view.setPositionRelativeTo(worldView, tempPos.setTo(newX, newY))

                view.rotation = node.angle

                val viewRoot = view.root
                val viewRootStage = viewRoot is Stage

                node.viewInfo.x = view.x
                node.viewInfo.y = view.y
                node.viewInfo.rotation = view.rotation

                if (autoDestroyBodies && node.viewInfo.onStage && !viewRootStage) {
                    world.destroyBody(node)
                    node.view?.body = null
                    node.view = null
                }

                node.viewInfo.onStage = viewRootStage
            }
        }
    }
}

var View.box2dWorldComponent by Extra.PropertyThis<View, Box2dWorldComponent?> { null }

inline fun View.getOrCreateBox2dWorld(): Box2dWorldComponent {
    if (this.box2dWorldComponent == null) {
        val component = Box2dWorldComponent(this, World(0f, DEFAULT_GRAVITY_Y).also { it.customScale = DEFAULT_SCALE }, 6, 2)
        this.box2dWorldComponent = component
        addComponent(component)
    }
    return this.box2dWorldComponent!!
}

val View.nearestBox2dWorldComponent: Box2dWorldComponent
    get() {
        var nearestReference: View? = null
        var view: View? = this
        while (view != null) {
            val component = view.box2dWorldComponent
            if (component != null) {
                return component
            }
            //if (view.parent == null || view is View.Reference) {
            if (view is View.Reference) {
                if (nearestReference == null) {
                    nearestReference = view
                }
            }
            if (view.parent == null) {
                return (nearestReference ?: view).getOrCreateBox2dWorld()
            }
            view = view.parent
        }
        invalidOp
    }

val View.nearestBox2dWorld: World get() = nearestBox2dWorldComponent.world

inline fun View.createBody(world: World? = null, callback: BodyDef.() -> Unit): Body = (world ?: nearestBox2dWorld).createBody(BodyDef().apply(callback))

/** Shortcut to create and attach a [Fixture] to this [Body] */
inline fun Body.fixture(callback: FixtureDef.() -> Unit): Body = this.also { createFixture(FixtureDef().apply(callback)) }

var View.body by Extra.PropertyThis<View, Body?>("box2dBody") { null }

inline fun <T : View> T.registerBody(body: Body): T {
    body.view = this
    this.body = body
    return this
}

//private val BOX2D_BODY_KEY = "box2dBody"

private val ViewKey = Box2dTypedUserData.Key<View>()

var Body.view: View?
    get() = this[ViewKey]
    set(value) {
        this[ViewKey] = value
    }

/** Shortcut to create a simple [Body] to this [World] with the specified properties */
inline fun <T : View> T.registerBodyWithFixture(
    angularVelocity: Number = 0.0,
    linearVelocityX: Number = 0.0,
    linearVelocityY: Number = 0.0,
    linearDamping: Number = 0.0,
    angularDamping: Number = 0.0,
    gravityScale: Number = 1.0,
    shape: Shape? = null,
    allowSleep: Boolean = true,
    awake: Boolean = true,
    fixedRotation: Boolean = false,
    bullet: Boolean = false,
    type: BodyType = BodyType.STATIC,
    friction: Number = 0.2,
    restitution: Number = 0.2,
    active: Boolean = true,
    isSensor: Boolean = false,
    density: Number = 1.0,
    world: World? = null,
): T {
    val body = createBody(world) {
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
        this.active = active
    }
    val world = body.world

    body.fixture {
        this.shape = if (shape != null) {
            shape
        } else {
            BoxShape(width / world.customScale, height / world.customScale)
        }
        this.isSensor = isSensor
        this.friction = friction.toFloat()
        this.restitution = restitution.toFloat()
        this.density = density.toFloat()
    }
    body.view = this
    this.body = body
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

@Deprecated("Not required")
class WorldView(
    override val world: World,
    var velocityIterations: Int = 6,
    var positionIterations: Int = 2
) : Container(), WorldRef {

    var debugWorldViews = false

    init {
        addUpdater {
            world.step(it.seconds.toFloat(), velocityIterations, positionIterations)
            world.forEachBody { node ->
                val px = node.position.x.toDouble()
                val py = node.position.y.toDouble()
                val view = node.view
                if (view != null) {
                    view.x = px
                    view.y = py
                    view.rotation = node.angle
                }
            }
        }
    }

    override fun renderInternal(ctx: RenderContext) {
        //ctx.views?.checkBox2dRegistered()

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
        body.view = this
        this.body = body
        return this
    }

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
        restitution: Number = 0.01,
        density: Number = 1.0
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
        body.view = this
        this.body = body
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
