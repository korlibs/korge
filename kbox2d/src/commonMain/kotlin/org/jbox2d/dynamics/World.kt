/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.jbox2d.dynamics

import org.jbox2d.callbacks.*
import org.jbox2d.collision.*
import org.jbox2d.collision.broadphase.*
import org.jbox2d.collision.shapes.*
import org.jbox2d.common.*
import org.jbox2d.dynamics.contacts.*
import org.jbox2d.dynamics.joints.*
import org.jbox2d.internal.*
import org.jbox2d.particle.*
import org.jbox2d.pooling.*
import org.jbox2d.pooling.arrays.*
import org.jbox2d.pooling.normal.*
import org.jbox2d.userdata.*

/**
 * The world class manages all physics entities, dynamic simulation, and asynchronous queries. The
 * world also contains efficient memory management facilities.
 *
 * @author Daniel Murphy
 */
open class World(gravity: Vec2, val pool: IWorldPool, broadPhase: BroadPhase) : WorldRef, Box2dTypedUserData by Box2dTypedUserData.Mixin() {
    override val world: World get() = this

    // statistics gathering
    var activeContacts = 0
    var contactPoolCount = 0

    var userData: Any? = null

    var m_flags: Int = CLEAR_FORCES

    /**
     * Get the contact manager for testing purposes
     *
     * @return
     */
    var m_contactManager: ContactManager = ContactManager(this, broadPhase)
        protected set

    /**
     * Get the world body list. With the returned body, use Body.getNext to get the next body in the
     * world list. A null body indicates the end of the list.
     *
     * @return the head of the world body list.
     */
    var bodyList: Body? = null
        private set
    /**
     * Get the world joint list. With the returned joint, use Joint.getNext to get the next joint in
     * the world list. A null joint indicates the end of the list.
     *
     * @return the head of the world joint list.
     */
    var jointList: Joint? = null
        private set

    /**
     * Get the number of bodies.
     *
     * @return
     */
    var bodyCount: Int = 0
        private set
    /**
     * Get the number of joints.
     *
     * @return
     */
    var jointCount: Int = 0
        private set

    /**
     * Get the global gravity vector.
     *
     * @return
     */
    /**
     * Change the global gravity vector.
     *
     * @param gravity
     */
    var gravity = Vec2()
        set(gravity) {
            this.gravity.set(gravity)
        }
    var isSleepingAllowed: Boolean = true

    // private Body m_groundBody;

    /**
     * Register a destruction listener. The listener is owned by you and must remain in scope.
     *
     * @param listener
     */
    var destructionListener: DestructionListener? = null
    var particleDestructionListener: ParticleDestructionListener? = null
    private var m_debugDraw: DebugDraw? = null

    /**
     * This is used to compute the time step ratio to support a variable time step.
     */
    private var m_inv_dt0: Float = 0f

    // these are for debugging the solver
    /**
     * Enable/disable warm starting. For testing.
     *
     * @param flag
     */
    var isWarmStarting: Boolean = true
    /**
     * Enable/disable continuous physics. For testing.
     *
     * @param flag
     */
    var isContinuousPhysics: Boolean = true
    var isSubStepping: Boolean = false

    private var m_stepComplete: Boolean = true

    val profile: Profile = Profile()

    private val m_particleSystem: ParticleSystem = ParticleSystem(this)


    private val contactStacks = Array<Array<ContactRegister>>(ShapeType.values().size) { arrayOfNulls<ContactRegister>(ShapeType.values().size) as Array<ContactRegister> }

    var isAllowSleep: Boolean
        get() = isSleepingAllowed
        set(flag) {
            if (flag == isSleepingAllowed) {
                return
            }

            isSleepingAllowed = flag
            if (isSleepingAllowed == false) {
                var b = bodyList
                while (b != null) {
                    b.isAwake = true
                    b = b.m_next
                }
            }
        }

    // djm pooling
    private val step = TimeStep()
    private val stepTimer = Timer()
    private val tempTimer = Timer()

    private val color = Color3f()
    private val xf = Transform()
    private val cA = Vec2()
    private val cB = Vec2()
    private val avs = Vec2ArrayPool()

    private val wqwrapper = WorldQueryWrapper()

    private val wrcwrapper = WorldRayCastWrapper()
    private val input = RayCastInput()

    /**
     * Get the world contact list. With the returned contact, use Contact.getNext to get the next
     * contact in the world list. A null contact indicates the end of the list.
     *
     * @return the head of the world contact list.
     * @warning contacts are created and destroyed in the middle of a time step. Use ContactListener
     * to avoid missing contacts.
     */
    val contactList: Contact
        get() = m_contactManager!!.m_contactList!!


    /**
     * Get the number of broad-phase proxies.
     *
     * @return
     */
    val proxyCount: Int
        get() = m_contactManager.m_broadPhase.proxyCount

    /**
     * Get the number of contacts (each may have 0 or more contact points).
     *
     * @return
     */
    val contactCount: Int
        get() = m_contactManager.m_contactCount

    /**
     * Gets the height of the dynamic tree
     *
     * @return
     */
    val treeHeight: Int
        get() = m_contactManager.m_broadPhase.treeHeight

    /**
     * Gets the balance of the dynamic tree
     *
     * @return
     */
    val treeBalance: Int
        get() = m_contactManager.m_broadPhase.treeBalance

    /**
     * Gets the quality of the dynamic tree
     *
     * @return
     */
    val treeQuality: Float
        get() = m_contactManager.m_broadPhase.treeQuality

    /**
     * Is the world locked (in the middle of a time step).
     *
     * @return
     */
    val isLocked: Boolean
        get() = m_flags and LOCKED == LOCKED

    /**
     * Get the flag that controls automatic clearing of forces after each time step.
     *
     * @return
     */
    /**
     * Set flag to control automatic clearing of forces after each time step.
     *
     * @param flag
     */
    var autoClearForces: Boolean
        get() = m_flags and CLEAR_FORCES == CLEAR_FORCES
        set(flag) = if (flag) {
            m_flags = m_flags or CLEAR_FORCES
        } else {
            m_flags = m_flags and CLEAR_FORCES.inv()
        }

    private val island = Island()
    private var stack = arrayOfNulls<Body>(10) // TODO djm find a good initial stack number;
    private val broadphaseTimer = Timer()

    private val toiIsland = Island()
    private val toiInput = TimeOfImpact.TOIInput()
    private val toiOutput = TimeOfImpact.TOIOutput()
    private val subStep = TimeStep()
    private val tempBodies = arrayOfNulls<Body>(2)
    private val backup1 = Sweep()
    private val backup2 = Sweep()
    private val liquidLength = .12f
    private var averageLinearVel = -1f
    private val liquidOffset = Vec2()
    private val circCenterMoved = Vec2()
    private val liquidColor = Color3f(.4f, .4f, 1f)

    private val center = Vec2()
    private val axis = Vec2()
    private val v1 = Vec2()
    private val v2 = Vec2()
    private val tlvertices = Vec2ArrayPool()

    /**
     * Get the world particle group list. With the returned group, use ParticleGroup::GetNext to get
     * the next group in the world list. A NULL group indicates the end of the list.
     *
     * @return the head of the world particle group list.
     */
    val particleGroupList: Array<ParticleGroup?>
        get() = m_particleSystem.getParticleGroupList()!!

    /**
     * Get the number of particle groups.
     *
     * @return
     */
    val particleGroupCount: Int
        get() = m_particleSystem.particleGroupCount

    /**
     * Get the number of particles.
     *
     * @return
     */
    val particleCount: Int
        get() = m_particleSystem.particleCount

    /**
     * Get the maximum number of particles.
     *
     * @return
     */
    /**
     * Set the maximum number of particles.
     *
     * @param count
     */
    var particleMaxCount: Int
        get() = m_particleSystem.particleMaxCount
        set(count) {
            m_particleSystem.particleMaxCount = count
        }

    /**
     * Get the particle density.
     *
     * @return
     */
    /**
     * Change the particle density.
     *
     * @param density
     */
    var particleDensity: Float
        get() = m_particleSystem.particleDensity
        set(density) {
            m_particleSystem.particleDensity = density
        }

    /**
     * Get the particle gravity scale.
     *
     * @return
     */
    /**
     * Change the particle gravity scale. Adjusts the effect of the global gravity vector on
     * particles. Default value is 1.0f.
     *
     * @param gravityScale
     */
    var particleGravityScale: Float
        get() = m_particleSystem.particleGravityScale
        set(gravityScale) {
            m_particleSystem.particleGravityScale = gravityScale

        }

    /**
     * Get damping for particles
     *
     * @return
     */
    /**
     * Damping is used to reduce the velocity of particles. The damping parameter can be larger than
     * 1.0f but the damping effect becomes sensitive to the time step when the damping parameter is
     * large.
     *
     * @param damping
     */
    var particleDamping: Float
        get() = m_particleSystem.particleDamping
        set(damping) {
            m_particleSystem.particleDamping = damping
        }

    /**
     * Get the particle radius.
     *
     * @return
     */
    /**
     * Change the particle radius. You should set this only once, on world start. If you change the
     * radius during execution, existing particles may explode, shrink, or behave unexpectedly.
     *
     * @param radius
     */
    var particleRadius: Float
        get() = m_particleSystem.particleRadius
        set(radius) {
            m_particleSystem.particleRadius = radius
        }

    /**
     * Get the particle data. @return the pointer to the head of the particle data.
     *
     * @return
     */
    val particleFlagsBuffer: IntArray
        get() = m_particleSystem!!.particleFlagsBuffer!!

    val particlePositionBuffer: Array<Vec2>
        get() = m_particleSystem!!.particlePositionBuffer!!

    val particleVelocityBuffer: Array<Vec2>
        get() = m_particleSystem.particleVelocityBuffer!!

    val particleColorBuffer: Array<ParticleColor>
        get() = m_particleSystem.particleColorBuffer!!

    val particleGroupBuffer: Array<ParticleGroup?>
        get() = m_particleSystem.particleGroupBuffer!!

    val particleUserDataBuffer: Array<Any>
        get() = m_particleSystem.particleUserDataBuffer!!

    /**
     * Get contacts between particles
     *
     * @return
     */
    val particleContacts: Array<ParticleContact>
        get() = m_particleSystem.m_contactBuffer

    val particleContactCount: Int
        get() = m_particleSystem.m_contactCount

    /**
     * Get contacts between particles and bodies
     *
     * @return
     */
    val particleBodyContacts: Array<ParticleBodyContact>
        get() = m_particleSystem.m_bodyContactBuffer

    val particleBodyContactCount: Int
        get() = m_particleSystem.m_bodyContactCount


    constructor(gravity: Vec2, pool: IWorldPool = DefaultWorldPool(WORLD_POOL_SIZE, WORLD_POOL_CONTAINER_SIZE), strategy: BroadPhaseStrategy = DynamicTree()) : this(gravity, pool, DefaultBroadPhaseBuffer(strategy)) {
    }

    init {
        this.gravity.set(gravity)
        initializeRegisters()
    }

    private fun addType(creator: IDynamicStack<Contact>, type1: ShapeType, type2: ShapeType) {
        val register = ContactRegister()
        register.creator = creator
        register.primary = true
        contactStacks[type1.ordinal][type2.ordinal] = register

        if (type1 !== type2) {
            val register2 = ContactRegister()
            register2.creator = creator
            register2.primary = false
            contactStacks[type2.ordinal][type1.ordinal] = register2
        }
    }

    private fun initializeRegisters() {
        addType(pool.circleContactStack, ShapeType.CIRCLE, ShapeType.CIRCLE)
        addType(pool.polyCircleContactStack, ShapeType.POLYGON, ShapeType.CIRCLE)
        addType(pool.polyContactStack, ShapeType.POLYGON, ShapeType.POLYGON)
        addType(pool.edgeCircleContactStack, ShapeType.EDGE, ShapeType.CIRCLE)
        addType(pool.edgePolyContactStack, ShapeType.EDGE, ShapeType.POLYGON)
        addType(pool.chainCircleContactStack, ShapeType.CHAIN, ShapeType.CIRCLE)
        addType(pool.chainPolyContactStack, ShapeType.CHAIN, ShapeType.POLYGON)
    }

    fun popContact(fixtureA: Fixture, indexA: Int, fixtureB: Fixture, indexB: Int): Contact? {
        val type1 = fixtureA.type
        val type2 = fixtureB.type

        val reg = contactStacks[type1.ordinal][type2.ordinal]
        if (reg != null) {
            if (reg.primary) {
                val c = reg.creator!!.pop()
                c.init(fixtureA, indexA, fixtureB, indexB)
                return c
            } else {
                val c = reg.creator!!.pop()
                c.init(fixtureB, indexB, fixtureA, indexA)
                return c
            }
        } else {
            return null
        }
    }

    fun pushContact(contact: Contact) {
        val fixtureA = contact.getFixtureA()
        val fixtureB = contact.getFixtureB()

        if (contact.m_manifold.pointCount > 0 && !fixtureA!!.isSensor && !fixtureB!!.isSensor) {
            fixtureA.getBody()!!.isAwake = true
            fixtureB.getBody()!!.isAwake = true
        }

        val type1 = fixtureA!!.type
        val type2 = fixtureB!!.type

        val creator = contactStacks[type1.ordinal][type2.ordinal].creator
        creator!!.push(contact)
    }

    /**
     * Register a contact filter to provide specific control over collision. Otherwise the default
     * filter is used (_defaultFilter). The listener is owned by you and must remain in scope.
     *
     * @param filter
     */
    fun setContactFilter(filter: ContactFilter) {
        m_contactManager.m_contactFilter = filter
    }

    /**
     * Register a contact event listener. The listener is owned by you and must remain in scope.
     *
     * @param listener
     */
    fun setContactListener(listener: ContactListener) {
        m_contactManager.m_contactListener = listener
    }

    /**
     * Register a routine for debug drawing. The debug draw functions are called inside with
     * World.DrawDebugData method. The debug draw object is owned by you and must remain in scope.
     *
     * @param debugDraw
     */
    fun setDebugDraw(debugDraw: DebugDraw) {
        m_debugDraw = debugDraw
    }

    /**
     * create a rigid body given a definition. No reference to the definition is retained.
     *
     * @warning This function is locked during callbacks.
     * @param def
     * @return
     */
    fun createBody(def: BodyDef): Body {
        assert(isLocked == false)
        if (isLocked) {
            error("World is locked")
        }
        // TODO djm pooling
        val b = Body(def, this)

        // add to world doubly linked list
        b.m_prev = null
        b.m_next = bodyList
        if (bodyList != null) {
            bodyList!!.m_prev = b
        }
        bodyList = b
        ++bodyCount

        return b
    }

    /**
     * destroy a rigid body given a definition. No reference to the definition is retained. This
     * function is locked during callbacks.
     *
     * @warning This automatically deletes all associated shapes and joints.
     * @warning This function is locked during callbacks.
     * @param body
     */
    fun destroyBody(body: Body) {
        assert(bodyCount > 0)
        assert(isLocked == false)
        if (isLocked) {
            return
        }

        // Delete the attached joints.
        var je = body.m_jointList
        while (je != null) {
            val je0 = je
            je = je.next
            if (destructionListener != null) {
                destructionListener!!.sayGoodbye(je0.joint!!)
            }

            destroyJoint(je0.joint)

            body.m_jointList = je
        }
        body.m_jointList = null

        // Delete the attached contacts.
        var ce = body.m_contactList
        while (ce != null) {
            val ce0 = ce
            ce = ce.next
            m_contactManager.destroy(ce0.contact!!)
        }
        body.m_contactList = null

        var f = body.m_fixtureList
        while (f != null) {
            val f0 = f
            f = f.m_next

            if (destructionListener != null) {
                destructionListener!!.sayGoodbye(f0)
            }

            f0.destroyProxies(m_contactManager.m_broadPhase)
            f0.destroy()
            // TODO djm recycle fixtures (here or in that destroy method)
            body.m_fixtureList = f
            body.m_fixtureCount -= 1
        }
        body.m_fixtureList = null
        body.m_fixtureCount = 0

        // Remove world body list.
        if (body.m_prev != null) {
            body.m_prev!!.m_next = body.m_next
        }

        if (body.m_next != null) {
            body.m_next!!.m_prev = body.m_prev
        }

        if (body == bodyList) {
            bodyList = body.m_next
        }

        --bodyCount
        // TODO djm recycle body
    }

    /**
     * create a joint to constrain bodies together. No reference to the definition is retained. This
     * may cause the connected bodies to cease colliding.
     *
     * @warning This function is locked during callbacks.
     * @param def
     * @return
     */
    fun createJoint(def: JointDef): Joint? {
        assert(isLocked == false)
        if (isLocked) {
            return null
        }

        val j = Joint.create(this, def)

        // Connect to the world list.
        j!!.m_prev = null
        j.m_next = jointList
        if (jointList != null) {
            jointList!!.m_prev = j
        }
        jointList = j
        ++jointCount

        // Connect to the bodies' doubly linked lists.
        j.m_edgeA.joint = j
        j.m_edgeA.other = j.getBodyB()
        j.m_edgeA.prev = null
        j.m_edgeA.next = j.getBodyA()!!.m_jointList
        if (j.getBodyA()!!.m_jointList != null) {
            j.getBodyA()!!.m_jointList!!.prev = j.m_edgeA
        }
        j.getBodyA()!!.m_jointList = j.m_edgeA

        j.m_edgeB.joint = j
        j.m_edgeB.other = j.getBodyA()
        j.m_edgeB.prev = null
        j.m_edgeB.next = j.getBodyB()!!.m_jointList
        if (j.getBodyB()!!.m_jointList != null) {
            j.getBodyB()!!.m_jointList!!.prev = j.m_edgeB
        }
        j.getBodyB()!!.m_jointList = j.m_edgeB

        val bodyA = def.bodyA
        val bodyB = def.bodyB

        // If the joint prevents collisions, then flag any contacts for filtering.
        if (def.collideConnected == false) {
            var edge = bodyB!!.getContactList()
            while (edge != null) {
                if (edge.other == bodyA) {
                    // Flag the contact for filtering at the next time step (where either
                    // body is awake).
                    edge.contact!!.flagForFiltering()
                }

                edge = edge.next
            }
        }

        // Note: creating a joint doesn't wake the bodies.

        return j
    }

    /**
     * destroy a joint. This may cause the connected bodies to begin colliding.
     *
     * @warning This function is locked during callbacks.
     * @param joint
     */
    fun destroyJoint(j: Joint?) {
        assert(isLocked == false)
        if (isLocked) {
            return
        }

        val collideConnected = j!!.getCollideConnected()

        // Remove from the doubly linked list.
        if (j.m_prev != null) {
            j.m_prev!!.m_next = j.m_next
        }

        if (j.m_next != null) {
            j.m_next!!.m_prev = j.m_prev
        }

        if (j === jointList) {
            jointList = j.m_next
        }

        // Disconnect from island graph.
        val bodyA = j.getBodyA()
        val bodyB = j.getBodyB()

        // Wake up connected bodies.
        bodyA!!.isAwake = true
        bodyB!!.isAwake = true

        // Remove from body 1.
        if (j.m_edgeA.prev != null) {
            j.m_edgeA.prev!!.next = j.m_edgeA.next
        }

        if (j.m_edgeA.next != null) {
            j.m_edgeA.next!!.prev = j.m_edgeA.prev
        }

        if (j.m_edgeA == bodyA.m_jointList) {
            bodyA.m_jointList = j.m_edgeA.next
        }

        j.m_edgeA.prev = null
        j.m_edgeA.next = null

        // Remove from body 2
        if (j.m_edgeB.prev != null) {
            j.m_edgeB.prev!!.next = j.m_edgeB.next
        }

        if (j.m_edgeB.next != null) {
            j.m_edgeB.next!!.prev = j.m_edgeB.prev
        }

        if (j.m_edgeB == bodyB.m_jointList) {
            bodyB.m_jointList = j.m_edgeB.next
        }

        j.m_edgeB.prev = null
        j.m_edgeB.next = null

        Joint.destroy(j)

        assert(jointCount > 0)
        --jointCount

        // If the joint prevents collisions, then flag any contacts for filtering.
        if (collideConnected == false) {
            var edge = bodyB.getContactList()
            while (edge != null) {
                if (edge.other == bodyA) {
                    // Flag the contact for filtering at the next time step (where either
                    // body is awake).
                    edge.contact!!.flagForFiltering()
                }

                edge = edge.next
            }
        }
    }

    /**
     * Take a time step. This performs collision detection, integration, and constraint solution.
     *
     * @param timeStep the amount of time to simulate, this should not vary.
     * @param velocityIterations for the velocity constraint solver.
     * @param positionIterations for the position constraint solver.
     */
    fun step(dt: Float, velocityIterations: Int, positionIterations: Int) {
        stepTimer.reset()
        tempTimer.reset()
        // log.debug("Starting step");
        // If new fixtures were added, we need to find the new contacts.
        if (m_flags and NEW_FIXTURE == NEW_FIXTURE) {
            // log.debug("There's a new fixture, lets look for new contacts");
            m_contactManager.findNewContacts()
            m_flags = m_flags and NEW_FIXTURE.inv()
        }

        m_flags = m_flags or LOCKED

        step.dt = dt
        step.velocityIterations = velocityIterations
        step.positionIterations = positionIterations
        if (dt > 0.0f) {
            step.inv_dt = 1.0f / dt
        } else {
            step.inv_dt = 0.0f
        }

        step.dtRatio = m_inv_dt0 * dt

        step.warmStarting = isWarmStarting
        profile.stepInit.record(tempTimer.milliseconds)

        // Update contacts. This is where some contacts are destroyed.
        tempTimer.reset()
        m_contactManager.collide()
        profile.collide.record(tempTimer.milliseconds)

        // Integrate velocities, solve velocity constraints, and integrate positions.
        if (m_stepComplete && step.dt > 0.0f) {
            tempTimer.reset()
            m_particleSystem.solve(step) // Particle Simulation
            profile.solveParticleSystem.record(tempTimer.milliseconds)
            tempTimer.reset()
            solve(step)
            profile.solve.record(tempTimer.milliseconds)
        }

        // Handle TOI events.
        if (isContinuousPhysics && step.dt > 0.0f) {
            tempTimer.reset()
            solveTOI(step)
            profile.solveTOI.record(tempTimer.milliseconds)
        }

        if (step.dt > 0.0f) {
            m_inv_dt0 = step.inv_dt
        }

        if (m_flags and CLEAR_FORCES == CLEAR_FORCES) {
            clearForces()
        }

        m_flags = m_flags and LOCKED.inv()
        // log.debug("ending step");

        profile.step.record(stepTimer.milliseconds)
    }

    /**
     * Call this after you are done with time steps to clear the forces. You normally call this after
     * each call to Step, unless you are performing sub-steps. By default, forces will be
     * automatically cleared, so you don't need to call this function.
     *
     * @see setAutoClearForces
     */
    fun clearForces() {
        var body = bodyList
        while (body != null) {
            body.m_force.setZero()
            body.m_torque = 0.0f
            body = body.getNext()
        }
    }

    /**
     * Call this to draw shapes and other debug draw data.
     */
    fun drawDebugData() {
        if (m_debugDraw == null) {
            return
        }


        val flags = m_debugDraw!!.flags
        val wireframe = flags and DebugDraw.e_wireframeDrawingBit != 0

        if (flags and DebugDraw.e_shapeBit != 0) {
            var b = bodyList
            while (b != null) {
                xf.set(b.getTransform())
                var f = b.getFixtureList()
                while (f != null) {
                    if (b.isActive == false) {
                        color.set(0.5f, 0.5f, 0.3f)
                        drawShape(f, xf, color, wireframe)
                    } else if (b.type === BodyType.STATIC) {
                        color.set(0.5f, 0.9f, 0.3f)
                        drawShape(f, xf, color, wireframe)
                    } else if (b.type === BodyType.KINEMATIC) {
                        color.set(0.5f, 0.5f, 0.9f)
                        drawShape(f, xf, color, wireframe)
                    } else if (b.isAwake == false) {
                        color.set(0.5f, 0.5f, 0.5f)
                        drawShape(f, xf, color, wireframe)
                    } else {
                        color.set(0.9f, 0.7f, 0.7f)
                        drawShape(f, xf, color, wireframe)
                    }
                    f = f.getNext()
                }
                b = b.getNext()
            }
            drawParticleSystem(m_particleSystem)
        }

        if (flags and DebugDraw.e_jointBit != 0) {
            var j = jointList
            while (j != null) {
                drawJoint(j)
                j = j.getNext()
            }
        }

        if (flags and DebugDraw.e_pairBit != 0) {
            color.set(0.3f, 0.9f, 0.9f)
            var c: Contact? = m_contactManager.m_contactList
            while (c != null) {
                val fixtureA = c.getFixtureA()
                val fixtureB = c.getFixtureB()
                fixtureA!!.getAABB(c.getChildIndexA()).getCenterToOut(cA)
                fixtureB!!.getAABB(c.getChildIndexB()).getCenterToOut(cB)
                m_debugDraw!!.drawSegment(cA, cB, color)
                c = c.getNext()
            }
        }

        if (flags and DebugDraw.e_aabbBit != 0) {
            color.set(0.9f, 0.3f, 0.9f)

            var b = bodyList
            while (b != null) {
                if (b.isActive == false) {
                    b = b.getNext()
                    continue
                }

                var f = b.getFixtureList()
                while (f != null) {
                    for (i in 0 until f.m_proxyCount) {
                        val proxy = f.m_proxies!![i]
                        val aabb = m_contactManager.m_broadPhase.getFatAABB(proxy.proxyId)
                        if (aabb != null) {
                            val vs = avs[4]
                            vs[0].set(aabb.lowerBound.x, aabb.lowerBound.y)
                            vs[1].set(aabb.upperBound.x, aabb.lowerBound.y)
                            vs[2].set(aabb.upperBound.x, aabb.upperBound.y)
                            vs[3].set(aabb.lowerBound.x, aabb.upperBound.y)
                            m_debugDraw!!.drawPolygon(vs, 4, color)
                        }
                    }
                    f = f.getNext()
                }
                b = b.getNext()
            }
        }

        if (flags and DebugDraw.e_centerOfMassBit != 0) {
            var b = bodyList
            while (b != null) {
                xf.set(b.getTransform())
                xf.p.set(b.worldCenter)
                m_debugDraw!!.drawTransform(xf)
                b = b.getNext()
            }
        }

        if (flags and DebugDraw.e_dynamicTreeBit != 0) {
            m_contactManager.m_broadPhase.drawTree(m_debugDraw!!)
        }

        m_debugDraw!!.flush()
    }

    /**
     * Query the world for all fixtures that potentially overlap the provided AABB.
     *
     * @param callback a user implemented callback class.
     * @param aabb the query box.
     */
    fun queryAABB(callback: QueryCallback, aabb: AABB) {
        wqwrapper.broadPhase = m_contactManager.m_broadPhase
        wqwrapper.callback = callback
        m_contactManager.m_broadPhase.query(wqwrapper, aabb)
    }

    /**
     * Query the world for all fixtures and particles that potentially overlap the provided AABB.
     *
     * @param callback a user implemented callback class.
     * @param particleCallback callback for particles.
     * @param aabb the query box.
     */
    fun queryAABB(callback: QueryCallback, particleCallback: ParticleQueryCallback, aabb: AABB) {
        wqwrapper.broadPhase = m_contactManager.m_broadPhase
        wqwrapper.callback = callback
        m_contactManager.m_broadPhase.query(wqwrapper, aabb)
        m_particleSystem.queryAABB(particleCallback, aabb)
    }

    /**
     * Query the world for all particles that potentially overlap the provided AABB.
     *
     * @param particleCallback callback for particles.
     * @param aabb the query box.
     */
    fun queryAABB(particleCallback: ParticleQueryCallback, aabb: AABB) {
        m_particleSystem.queryAABB(particleCallback, aabb)
    }

    /**
     * Ray-cast the world for all fixtures in the path of the ray. Your callback controls whether you
     * get the closest point, any point, or n-points. The ray-cast ignores shapes that contain the
     * starting point.
     *
     * @param callback a user implemented callback class.
     * @param point1 the ray starting point
     * @param point2 the ray ending point
     */
    fun raycast(callback: RayCastCallback, point1: Vec2, point2: Vec2) {
        wrcwrapper.broadPhase = m_contactManager.m_broadPhase
        wrcwrapper.callback = callback
        input.maxFraction = 1.0f
        input.p1.set(point1)
        input.p2.set(point2)
        m_contactManager.m_broadPhase.raycast(wrcwrapper, input)
    }

    /**
     * Ray-cast the world for all fixtures and particles in the path of the ray. Your callback
     * controls whether you get the closest point, any point, or n-points. The ray-cast ignores shapes
     * that contain the starting point.
     *
     * @param callback a user implemented callback class.
     * @param particleCallback the particle callback class.
     * @param point1 the ray starting point
     * @param point2 the ray ending point
     */
    fun raycast(callback: RayCastCallback, particleCallback: ParticleRaycastCallback,
                point1: Vec2, point2: Vec2) {
        wrcwrapper.broadPhase = m_contactManager.m_broadPhase
        wrcwrapper.callback = callback
        input.maxFraction = 1.0f
        input.p1.set(point1)
        input.p2.set(point2)
        m_contactManager.m_broadPhase.raycast(wrcwrapper, input)
        m_particleSystem.raycast(particleCallback, point1, point2)
    }

    /**
     * Ray-cast the world for all particles in the path of the ray. Your callback controls whether you
     * get the closest point, any point, or n-points.
     *
     * @param particleCallback the particle callback class.
     * @param point1 the ray starting point
     * @param point2 the ray ending point
     */
    fun raycast(particleCallback: ParticleRaycastCallback, point1: Vec2, point2: Vec2) {
        m_particleSystem.raycast(particleCallback, point1, point2)
    }

    private fun solve(step: TimeStep) {
        profile.solveInit.startAccum()
        profile.solveVelocity.startAccum()
        profile.solvePosition.startAccum()

        // update previous transforms
        run {
            var b = bodyList
            while (b != null) {
                b!!.m_xf0.set(b!!.m_xf)
                b = b!!.m_next
            }
        }

        // Size the island for the worst case.
        island.init(bodyCount, m_contactManager.m_contactCount, jointCount,
                m_contactManager.m_contactListener)

        // Clear all the island flags.
        run {
            var b = bodyList
            while (b != null) {
                b!!.m_flags = b!!.m_flags and Body.e_islandFlag.inv()
                b = b!!.m_next
            }
        }
        var c: Contact? = m_contactManager.m_contactList
        while (c != null) {
            c.m_flags = c.m_flags and Contact.ISLAND_FLAG.inv()
            c = c.m_next
        }
        var j = jointList
        while (j != null) {
            j.m_islandFlag = false
            j = j.m_next
        }

        // Build and simulate all awake islands.
        val stackSize = bodyCount
        if (stack.size < stackSize) {
            stack = arrayOfNulls(stackSize)
        }
        var seed = bodyList
        while (seed != null) {
            if (seed.m_flags and Body.e_islandFlag == Body.e_islandFlag) {
                seed = seed.m_next
                continue
            }

            if (seed.isAwake == false || seed.isActive == false) {
                seed = seed.m_next
                continue
            }

            // The seed can be dynamic or kinematic.
            if (seed.type === BodyType.STATIC) {
                seed = seed.m_next
                continue
            }

            // Reset island and stack.
            island.clear()
            var stackCount = 0
            stack[stackCount++] = seed
            seed.m_flags = seed.m_flags or Body.e_islandFlag

            // Perform a depth first search (DFS) on the constraint graph.
            while (stackCount > 0) {
                // Grab the next body off the stack and add it to the island.
                val b = stack[--stackCount]!!
                assert(b.isActive == true)
                island.add(b)

                // Make sure the body is awake.
                b.isAwake = true

                // To keep islands as small as possible, we don't
                // propagate islands across static bodies.
                if (b.type === BodyType.STATIC) {
                    continue
                }

                // Search all contacts connected to this body.
                var ce = b.m_contactList
                while (ce != null) {
                    val contact = ce.contact

                    // Has this contact already been added to an island?
                    if (contact!!.m_flags and Contact.ISLAND_FLAG == Contact.ISLAND_FLAG) {
                        ce = ce.next
                        continue
                    }

                    // Is this contact solid and touching?
                    if (contact.isEnabled == false || contact.isTouching == false) {
                        ce = ce.next
                        continue
                    }

                    // Skip sensors.
                    val sensorA = contact.m_fixtureA!!.m_isSensor
                    val sensorB = contact.m_fixtureB!!.m_isSensor
                    if (sensorA || sensorB) {
                        ce = ce.next
                        continue
                    }

                    island.add(contact)
                    contact.m_flags = contact.m_flags or Contact.ISLAND_FLAG

                    val other = ce.other

                    // Was the other body already added to this island?
                    if (other!!.m_flags and Body.e_islandFlag == Body.e_islandFlag) {
                        ce = ce.next
                        continue
                    }

                    assert(stackCount < stackSize)
                    stack[stackCount++] = other
                    other.m_flags = other.m_flags or Body.e_islandFlag
                    ce = ce.next
                }

                // Search all joints connect to this body.
                var je = b.m_jointList
                while (je != null) {
                    if (je.joint!!.m_islandFlag == true) {
                        je = je.next
                        continue
                    }

                    val other = je.other

                    // Don't simulate joints connected to inactive bodies.
                    if (other!!.isActive == false) {
                        je = je.next
                        continue
                    }

                    island.add(je.joint!!)
                    je.joint!!.m_islandFlag = true

                    if (other.m_flags and Body.e_islandFlag == Body.e_islandFlag) {
                        je = je.next
                        continue
                    }

                    assert(stackCount < stackSize)
                    stack[stackCount++] = other
                    other.m_flags = other.m_flags or Body.e_islandFlag
                    je = je.next
                }
            }
            island.solve(profile, step, gravity, isSleepingAllowed)

            // Post solve cleanup.
            for (i in 0 until island.m_bodyCount) {
                // Allow static bodies to participate in other islands.
                val b = island.m_bodies!![i]
                if (b.type === BodyType.STATIC) {
                    b.m_flags = b.m_flags and Body.e_islandFlag.inv()
                }
            }
            seed = seed.m_next
        }
        profile.solveInit.endAccum()
        profile.solveVelocity.endAccum()
        profile.solvePosition.endAccum()

        broadphaseTimer.reset()
        // Synchronize fixtures, check for out of range bodies.
        var b = bodyList
        while (b != null) {
            // If a body was not in an island then it did not move.
            if (b!!.m_flags and Body.e_islandFlag == 0) {
                b = b!!.getNext()
                continue
            }

            if (b!!.type === BodyType.STATIC) {
                b = b!!.getNext()
                continue
            }

            // Update fixtures (for broad-phase).
            b!!.synchronizeFixtures()
            b = b!!.getNext()
        }

        // Look for new contacts.
        m_contactManager.findNewContacts()
        profile.broadphase.record(broadphaseTimer.milliseconds)
    }

    private fun solveTOI(step: TimeStep) {

        val island = toiIsland
        island.init(2 * Settings.maxTOIContacts, Settings.maxTOIContacts, 0,
                m_contactManager.m_contactListener)
        if (m_stepComplete) {
            var b = bodyList
            while (b != null) {
                b.m_flags = b.m_flags and Body.e_islandFlag.inv()
                b.m_sweep.alpha0 = 0.0f
                b = b.m_next
            }

            var c: Contact? = m_contactManager.m_contactList
            while (c != null) {
                // Invalidate TOI
                c.m_flags = c.m_flags and (Contact.TOI_FLAG or Contact.ISLAND_FLAG).inv()
                c.m_toiCount = 0f
                c.m_toi = 1.0f
                c = c.m_next
            }
        }

        // Find TOI events and solve them.
        while (true) {
            // Find the first TOI.
            var minContact: Contact? = null
            var minAlpha = 1.0f

            var c: Contact? = m_contactManager.m_contactList
            while (c != null) {
                // Is this contact disabled?
                if (c.isEnabled == false) {
                    c = c.m_next
                    continue
                }

                // Prevent excessive sub-stepping.
                if (c.m_toiCount > Settings.maxSubSteps) {
                    c = c.m_next
                    continue
                }

                var alpha = 1.0f
                if (c.m_flags and Contact.TOI_FLAG != 0) {
                    // This contact has a valid cached TOI.
                    alpha = c.m_toi
                } else {
                    val fA = c.getFixtureA()
                    val fB = c.getFixtureB()

                    // Is there a sensor?
                    if (fA!!.isSensor || fB!!.isSensor) {
                        c = c.m_next
                        continue
                    }

                    val bA = fA.getBody()
                    val bB = fB.getBody()

                    val typeA = bA!!.m_type
                    val typeB = bB!!.m_type
                    assert(typeA === BodyType.DYNAMIC || typeB === BodyType.DYNAMIC)

                    val activeA = bA.isAwake && typeA !== BodyType.STATIC
                    val activeB = bB.isAwake && typeB !== BodyType.STATIC

                    // Is at least one body active (awake and dynamic or kinematic)?
                    if (activeA == false && activeB == false) {
                        c = c.m_next
                        continue
                    }

                    val collideA = bA.isBullet || typeA !== BodyType.DYNAMIC
                    val collideB = bB.isBullet || typeB !== BodyType.DYNAMIC

                    // Are these two non-bullet dynamic bodies?
                    if (collideA == false && collideB == false) {
                        c = c.m_next
                        continue
                    }

                    // Compute the TOI for this contact.
                    // Put the sweeps onto the same time interval.
                    var alpha0 = bA.m_sweep.alpha0

                    if (bA.m_sweep.alpha0 < bB.m_sweep.alpha0) {
                        alpha0 = bB.m_sweep.alpha0
                        bA.m_sweep.advance(alpha0)
                    } else if (bB.m_sweep.alpha0 < bA.m_sweep.alpha0) {
                        alpha0 = bA.m_sweep.alpha0
                        bB.m_sweep.advance(alpha0)
                    }

                    assert(alpha0 < 1.0f)

                    val indexA = c.getChildIndexA()
                    val indexB = c.getChildIndexB()

                    // Compute the time of impact in interval [0, minTOI]
                    val input = toiInput
                    input.proxyA.set(fA.getShape()!!, indexA)
                    input.proxyB.set(fB.getShape()!!, indexB)
                    input.sweepA.set(bA.m_sweep)
                    input.sweepB.set(bB.m_sweep)
                    input.tMax = 1.0f

                    pool.timeOfImpact.timeOfImpact(toiOutput, input)

                    // Beta is the fraction of the remaining portion of the .
                    val beta = toiOutput.t
                    if (toiOutput.state === TimeOfImpact.TOIOutputState.TOUCHING) {
                        alpha = MathUtils.min(alpha0 + (1.0f - alpha0) * beta, 1.0f)
                    } else {
                        alpha = 1.0f
                    }

                    c.m_toi = alpha
                    c.m_flags = c.m_flags or Contact.TOI_FLAG
                }

                if (alpha < minAlpha) {
                    // This is the minimum TOI found so far.
                    minContact = c
                    minAlpha = alpha
                }
                c = c.m_next
            }

            if (minContact == null || 1.0f - 10.0f * Settings.EPSILON < minAlpha) {
                // No more TOI events. Done!
                m_stepComplete = true
                break
            }

            // Advance the bodies to the TOI.
            val fA = minContact.getFixtureA()
            val fB = minContact.getFixtureB()
            val bA = fA!!.getBody()
            val bB = fB!!.getBody()

            backup1.set(bA!!.m_sweep)
            backup2.set(bB!!.m_sweep)

            bA.advance(minAlpha)
            bB.advance(minAlpha)

            // The TOI contact likely has some new contact points.
            minContact.update(m_contactManager.m_contactListener)
            minContact.m_flags = minContact.m_flags and Contact.TOI_FLAG.inv()
            ++minContact.m_toiCount

            // Is the contact solid?
            if (minContact.isEnabled == false || minContact.isTouching == false) {
                // Restore the sweeps.
                minContact.isEnabled = false
                bA.m_sweep.set(backup1)
                bB.m_sweep.set(backup2)
                bA.synchronizeTransform()
                bB.synchronizeTransform()
                continue
            }

            bA.isAwake = true
            bB.isAwake = true

            // Build the island
            island.clear()
            island.add(bA)
            island.add(bB)
            island.add(minContact)

            bA.m_flags = bA.m_flags or Body.e_islandFlag
            bB.m_flags = bB.m_flags or Body.e_islandFlag
            minContact.m_flags = minContact.m_flags or Contact.ISLAND_FLAG

            // Get contacts on bodyA and bodyB.
            tempBodies[0] = bA
            tempBodies[1] = bB
            for (i in 0..1) {
                val body = tempBodies[i]!!
                if (body.m_type === BodyType.DYNAMIC) {
                    var ce = body.m_contactList
                    while (ce != null) {
                        if (island.m_bodyCount == island.m_bodyCapacity) {
                            break
                        }

                        if (island.m_contactCount == island.m_contactCapacity) {
                            break
                        }

                        val contact = ce.contact

                        // Has this contact already been added to the island?
                        if (contact!!.m_flags and Contact.ISLAND_FLAG != 0) {
                            ce = ce.next
                            continue
                        }

                        // Only add static, kinematic, or bullet bodies.
                        val other = ce.other
                        if (other!!.m_type === BodyType.DYNAMIC && body.isBullet == false
                                && other!!.isBullet == false) {
                            ce = ce.next
                            continue
                        }

                        // Skip sensors.
                        val sensorA = contact.m_fixtureA!!.m_isSensor
                        val sensorB = contact.m_fixtureB!!.m_isSensor
                        if (sensorA || sensorB) {
                            ce = ce.next
                            continue
                        }

                        // Tentatively advance the body to the TOI.
                        backup1.set(other!!.m_sweep)
                        if (other.m_flags and Body.e_islandFlag == 0) {
                            other.advance(minAlpha)
                        }

                        // Update the contact points
                        contact.update(m_contactManager.m_contactListener)

                        // Was the contact disabled by the user?
                        if (contact.isEnabled == false) {
                            other.m_sweep.set(backup1)
                            other.synchronizeTransform()
                            ce = ce.next
                            continue
                        }

                        // Are there contact points?
                        if (contact.isTouching == false) {
                            other.m_sweep.set(backup1)
                            other.synchronizeTransform()
                            ce = ce.next
                            continue
                        }

                        // Add the contact to the island
                        contact.m_flags = contact.m_flags or Contact.ISLAND_FLAG
                        island.add(contact)

                        // Has the other body already been added to the island?
                        if (other.m_flags and Body.e_islandFlag != 0) {
                            ce = ce.next
                            continue
                        }

                        // Add the other body to the island.
                        other.m_flags = other.m_flags or Body.e_islandFlag

                        if (other.m_type !== BodyType.STATIC) {
                            other.isAwake = true
                        }

                        island.add(other)
                        ce = ce.next
                    }
                }
            }

            subStep.dt = (1.0f - minAlpha) * step.dt
            subStep.inv_dt = 1.0f / subStep.dt
            subStep.dtRatio = 1.0f
            subStep.positionIterations = 20
            subStep.velocityIterations = step.velocityIterations
            subStep.warmStarting = false
            island.solveTOI(subStep, bA.m_islandIndex, bB.m_islandIndex)

            // Reset island flags and synchronize broad-phase proxies.
            for (i in 0 until island.m_bodyCount) {
                val body = island.m_bodies!![i]
                body.m_flags = body.m_flags and Body.e_islandFlag.inv()

                if (body.m_type !== BodyType.DYNAMIC) {
                    continue
                }

                body.synchronizeFixtures()

                // Invalidate all contact TOIs on this displaced body.
                var ce = body.m_contactList
                while (ce != null) {
                    ce.contact!!.m_flags = ce.contact!!.m_flags and (Contact.TOI_FLAG or Contact.ISLAND_FLAG).inv()
                    ce = ce.next
                }
            }

            // Commit fixture proxy movements to the broad-phase so that new contacts are created.
            // Also, some contacts can be destroyed.
            m_contactManager.findNewContacts()

            if (isSubStepping) {
                m_stepComplete = false
                break
            }
        }
    }

    private fun drawJoint(joint: Joint) {
        val bodyA = joint.getBodyA()
        val bodyB = joint.getBodyB()
        val xf1 = bodyA!!.getTransform()
        val xf2 = bodyB!!.getTransform()
        val x1 = xf1.p
        val x2 = xf2.p
        val p1 = pool.popVec2()
        val p2 = pool.popVec2()
        joint.getAnchorA(p1)
        joint.getAnchorB(p2)

        color.set(0.5f, 0.8f, 0.8f)

        when (joint.getType()) {
            // TODO djm write after writing joints
            JointType.DISTANCE -> m_debugDraw!!.drawSegment(p1, p2, color)

            JointType.PULLEY -> {
                val pulley = joint as PulleyJoint
                val s1 = pulley.getGroundAnchorA()
                val s2 = pulley.getGroundAnchorB()
                m_debugDraw!!.drawSegment(s1, p1, color)
                m_debugDraw!!.drawSegment(s2, p2, color)
                m_debugDraw!!.drawSegment(s1, s2, color)
            }
            JointType.CONSTANT_VOLUME, JointType.MOUSE -> {
            }
            else -> {
                m_debugDraw!!.drawSegment(x1, p1, color)
                m_debugDraw!!.drawSegment(p1, p2, color)
                m_debugDraw!!.drawSegment(x2, p2, color)
            }
        }// don't draw this
        pool.pushVec2(2)
    }

    private fun drawShape(fixture: Fixture, xf: Transform, color: Color3f, wireframe: Boolean) {
        when (fixture.type) {
            ShapeType.CIRCLE -> {
                val circle = fixture.getShape() as CircleShape?

                // Vec2 center = Mul(xf, circle.m_p);
                Transform.mulToOutUnsafe(xf, circle!!.m_p, center)
                val radius = circle.m_radius
                xf.q.getXAxis(axis)

                if (fixture.userData != null && fixture.userData == LIQUID_INT) {
                    val b = fixture.getBody()
                    liquidOffset.set(b!!.m_linearVelocity)
                    val linVelLength = b.m_linearVelocity.length()
                    if (averageLinearVel == -1f) {
                        averageLinearVel = linVelLength
                    } else {
                        averageLinearVel = .98f * averageLinearVel + .02f * linVelLength
                    }
                    liquidOffset.mulLocal(liquidLength / averageLinearVel / 2f)
                    circCenterMoved.set(center).addLocal(liquidOffset)
                    center.subLocal(liquidOffset)
                    m_debugDraw!!.drawSegment(center, circCenterMoved, liquidColor)
                    return
                }
                if (wireframe) {
                    m_debugDraw!!.drawCircle(center, radius, axis, color)
                } else {
                    m_debugDraw!!.drawSolidCircle(center, radius, axis, color)
                }
            }

            ShapeType.POLYGON -> {
                val poly = fixture.getShape() as PolygonShape?
                val vertexCount = poly!!.m_count
                assert(vertexCount <= Settings.maxPolygonVertices)
                val vertices = tlvertices[Settings.maxPolygonVertices]

                for (i in 0 until vertexCount) {
                    // vertices[i] = Mul(xf, poly.m_vertices[i]);
                    Transform.mulToOutUnsafe(xf, poly.m_vertices[i], vertices[i])
                }
                if (wireframe) {
                    m_debugDraw!!.drawPolygon(vertices, vertexCount, color)
                } else {
                    m_debugDraw!!.drawSolidPolygon(vertices, vertexCount, color)
                }
            }
            ShapeType.EDGE -> {
                val edge = fixture.getShape() as EdgeShape?
                Transform.mulToOutUnsafe(xf, edge!!.m_vertex1, v1)
                Transform.mulToOutUnsafe(xf, edge.m_vertex2, v2)
                m_debugDraw!!.drawSegment(v1, v2, color)
            }
            ShapeType.CHAIN -> {
                val chain = fixture.getShape() as ChainShape?
                val count = chain!!.m_count
                val vertices = chain.m_vertices

                Transform.mulToOutUnsafe(xf, vertices!![0], v1)
                for (i in 1 until count) {
                    Transform.mulToOutUnsafe(xf, vertices[i], v2)
                    m_debugDraw!!.drawSegment(v1, v2, color)
                    m_debugDraw!!.drawCircle(v1, 0.05f, color)
                    v1.set(v2)
                }
            }
            else -> {
            }
        }
    }

    private fun drawParticleSystem(system: ParticleSystem) {
        val wireframe = m_debugDraw!!.flags and DebugDraw.e_wireframeDrawingBit != 0
        val particleCount = system.particleCount
        if (particleCount != 0) {
            val particleRadius = system.particleRadius
            val positionBuffer = system.particlePositionBuffer
            var colorBuffer: Array<ParticleColor>? = null
            if (system.m_colorBuffer.data != null) {
                colorBuffer = system.particleColorBuffer
            }
            if (wireframe) {
                m_debugDraw!!.drawParticlesWireframe(positionBuffer!!, particleRadius, colorBuffer!!,
                        particleCount)
            } else {
                m_debugDraw!!.drawParticles(positionBuffer!!, particleRadius, colorBuffer!!, particleCount)
            }
        }
    }

    /**
     * Create a particle whose properties have been defined. No reference to the definition is
     * retained. A simulation step must occur before it's possible to interact with a newly created
     * particle. For example, DestroyParticleInShape() will not destroy a particle until Step() has
     * been called.
     *
     * @warning This function is locked during callbacks.
     * @return the index of the particle.
     */
    fun createParticle(def: ParticleDef): Int {
        assert(isLocked == false)
        if (isLocked) {
            return 0
        }
        val p = m_particleSystem.createParticle(def)
        return p
    }

    /**
     * Destroy a particle. The particle is removed after the next step.
     *
     * @param Index of the particle to destroy.
     * @param Whether to call the destruction listener just before the particle is destroyed.
     */

    fun destroyParticle(index: Int, callDestructionListener: Boolean = false) {
        m_particleSystem.destroyParticle(index, callDestructionListener)
    }

    /**
     * Destroy particles inside a shape. This function is locked during callbacks. In addition, this
     * function immediately destroys particles in the shape in contrast to DestroyParticle() which
     * defers the destruction until the next simulation step.
     *
     * @param Shape which encloses particles that should be destroyed.
     * @param Transform applied to the shape.
     * @param Whether to call the world b2DestructionListener for each particle destroyed.
     * @warning This function is locked during callbacks.
     * @return Number of particles destroyed.
     */

    fun destroyParticlesInShape(shape: Shape, xf: Transform, callDestructionListener: Boolean = false): Int {
        assert(isLocked == false)
        return if (isLocked) {
            0
        } else m_particleSystem.destroyParticlesInShape(shape, xf, callDestructionListener)
    }

    /**
     * Create a particle group whose properties have been defined. No reference to the definition is
     * retained.
     *
     * @warning This function is locked during callbacks.
     */
    fun createParticleGroup(def: ParticleGroupDef): ParticleGroup? {
        assert(isLocked == false)
        if (isLocked) {
            return null
        }
        val g = m_particleSystem.createParticleGroup(def)
        return g
    }

    /**
     * Join two particle groups.
     *
     * @param the first group. Expands to encompass the second group.
     * @param the second group. It is destroyed.
     * @warning This function is locked during callbacks.
     */
    fun joinParticleGroups(groupA: ParticleGroup, groupB: ParticleGroup) {
        assert(isLocked == false)
        if (isLocked) {
            return
        }
        m_particleSystem.joinParticleGroups(groupA, groupB)
    }

    /**
     * Destroy particles in a group. This function is locked during callbacks.
     *
     * @param The particle group to destroy.
     * @param Whether to call the world b2DestructionListener for each particle is destroyed.
     * @warning This function is locked during callbacks.
     */

    fun destroyParticlesInGroup(group: ParticleGroup, callDestructionListener: Boolean = false) {
        assert(isLocked == false)
        if (isLocked) {
            return
        }
        m_particleSystem.destroyParticlesInGroup(group, callDestructionListener)
    }

    /**
     * Set a buffer for particle data.
     *
     * @param buffer is a pointer to a block of memory.
     * @param size is the number of values in the block.
     */
    fun setParticleFlagsBuffer(buffer: IntArray, capacity: Int) {
        m_particleSystem.setParticleFlagsBuffer(buffer, capacity)
    }

    fun setParticlePositionBuffer(buffer: Array<Vec2>, capacity: Int) {
        m_particleSystem.setParticlePositionBuffer(buffer, capacity)

    }

    fun setParticleVelocityBuffer(buffer: Array<Vec2>, capacity: Int) {
        m_particleSystem.setParticleVelocityBuffer(buffer, capacity)

    }

    fun setParticleColorBuffer(buffer: Array<ParticleColor>, capacity: Int) {
        m_particleSystem.setParticleColorBuffer(buffer, capacity)

    }

    fun setParticleUserDataBuffer(buffer: Array<Any>, capacity: Int) {
        m_particleSystem.setParticleUserDataBuffer(buffer, capacity)
    }

    /**
     * Compute the kinetic energy that can be lost by damping force
     *
     * @return
     */
    fun computeParticleCollisionEnergy(): Float {
        return m_particleSystem.computeParticleCollisionEnergy()
    }

    companion object {
        val WORLD_POOL_SIZE = 100
        val WORLD_POOL_CONTAINER_SIZE = 10

        val NEW_FIXTURE = 0x0001
        val LOCKED = 0x0002
        val CLEAR_FORCES = 0x0004

        // NOTE this corresponds to the liquid test, so the debugdraw can draw
        // the liquid particles correctly. They should be the same.
        private val LIQUID_INT = 1234598372
    }
}
/**
 * Construct a world object.
 *
 * @param gravity the world gravity vector.
 */
/**
 * Construct a world object.
 *
 * @param gravity the world gravity vector.
 */
/**
 * Destroy a particle. The particle is removed after the next step.
 *
 * @param index
 */
/**
 * Destroy particles inside a shape without enabling the destruction callback for destroyed
 * particles. This function is locked during callbacks. For more information see
 * DestroyParticleInShape(Shape&, Transform&,bool).
 *
 * @param Shape which encloses particles that should be destroyed.
 * @param Transform applied to the shape.
 * @warning This function is locked during callbacks.
 * @return Number of particles destroyed.
 */
/**
 * Destroy particles in a group without enabling the destruction callback for destroyed particles.
 * This function is locked during callbacks.
 *
 * @param The particle group to destroy.
 * @warning This function is locked during callbacks.
 */


internal class WorldQueryWrapper : TreeCallback {

    var broadPhase: BroadPhase? = null
    var callback: QueryCallback? = null
    override fun treeCallback(nodeId: Int): Boolean {
        val proxy = broadPhase!!.getUserData(nodeId) as FixtureProxy?
        return callback!!.reportFixture(proxy!!.fixture!!)
    }
}


internal class WorldRayCastWrapper : TreeRayCastCallback {

    // djm pooling
    private val output = RayCastOutput()
    private val temp = Vec2()
    private val point = Vec2()

    var broadPhase: BroadPhase? = null
    var callback: RayCastCallback? = null

    override fun raycastCallback(input: RayCastInput, nodeId: Int): Float {
        val userData = broadPhase!!.getUserData(nodeId)
        val proxy = userData as FixtureProxy?
        val fixture = proxy!!.fixture
        val index = proxy.childIndex
        val hit = fixture!!.raycast(output, input, index)

        if (hit) {
            val fraction = output.fraction
            // Vec2 point = (1.0f - fraction) * input.p1 + fraction * input.p2;
            temp.set(input.p2).mulLocal(fraction)
            point.set(input.p1).mulLocal(1 - fraction).addLocal(temp)
            return callback!!.reportFixture(fixture, point, output.normal, fraction)
        }

        return input.maxFraction
    }
}
