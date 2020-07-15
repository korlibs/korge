package org.jbox2d.utests

import org.jbox2d.collision.shapes.*
import org.jbox2d.common.*
import org.jbox2d.dynamics.*
import org.jbox2d.userdata.*
import kotlin.test.*


class WorldTest {
    @Test
    fun test() {
        class Demo(val a: Int = 10)

        val DemoKey = Box2dTypedUserData.Key<Demo>()

        val world = World(Vec2(0f, -10f))
        world[DemoKey] = Demo()
        val groundBodyDef = BodyDef()
        groundBodyDef.position.set(0f, -10f)
        val groundBody = world.createBody(groundBodyDef)
        val groundBox = PolygonShape()
        groundBox.setAsBox(50f, 10f)
        groundBody.createFixture(groundBox, 0f)

        // Dynamic Body
        val bodyDef = BodyDef()
        bodyDef.type = BodyType.DYNAMIC
        bodyDef.position.set(0f, 4f)
        val body = world.createBody(bodyDef)
        val dynamicBox = PolygonShape()
        dynamicBox.setAsBox(1f, 1f)
        val fixtureDef = FixtureDef()
        fixtureDef.shape = dynamicBox
        fixtureDef.density = 1f
        fixtureDef.friction = 0.3f
        body.createFixture(fixtureDef)

        // Setup world
        val timeStep = 1.0f / 60.0f
        val velocityIterations = 6
        val positionIterations = 2

        assertEquals(true, world[DemoKey] is Demo)
        assertEquals(true, bodyDef[DemoKey] == null)
        world[DemoKey] = null
        assertEquals(false, world[DemoKey] is Demo)

        // Run loop
        for (i in 0 until 60) {
            world.step(timeStep, velocityIterations, positionIterations)
            val position = body.position
            val angle = body.angleRadians
            println("${position.x} ${position.y} $angle")
        }
    }
}
