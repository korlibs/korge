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

import com.soywiz.korma.geom.*
import org.jbox2d.collision.shapes.MassData
import org.jbox2d.collision.shapes.Shape
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Sweep
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.contacts.ContactEdge
import org.jbox2d.dynamics.joints.JointEdge
import org.jbox2d.internal.*
import org.jbox2d.userdata.*

/**
 * A rigid body. These are created via World.createBody.
 *
 * @author Daniel Murphy
 */
class Body(bd: BodyDef, var world: World) : Box2dTypedUserData by Box2dTypedUserData.Mixin() {
    fun destroyBody() {
        world.destroyBody(this)
    }

    val bodyDef = bd
    var didReset = true

    class ViewInfo {
        var view: Any? = null
        var x: Double = 0.0
        var y: Double = 0.0
        var rotation: Angle = 0.degrees
        var onStage = false
    }

    val viewInfo = ViewInfo()

    var _type: BodyType


    var flags: Int = 0


    var islandIndex: Int = 0

    /**
     * The body origin transform.
     */
    val xf = Transform()
    val transform: Transform get() = xf

    /**
     * The previous transform for particle simulation
     */
    val xf0 = Transform()

    /**
     * The swept motion for CCD
     */

    val sweep = Sweep()


    val _linearVelocity = Vec2()

    var _angularVelocity = 0f


    val force = Vec2()

    var torque = 0f

    var prev: Body? = null
    /** Get the next body in the world's body list.  */

    var m_next: Body? = null

    fun getNext() = m_next

    /** Get the list of all fixtures attached to this body.  */

    var m_fixtureList: Fixture? = null

    fun getFixtureList() = m_fixtureList


    var m_fixtureCount: Int = 0

    /** Get the list of all joints attached to this body.  */

    var m_jointList: JointEdge? = null
    /**
     * Get the list of all contacts attached to this body.
     *
     * @warning this list changes during the time step and you may miss some collisions if you don't
     * use ContactListener.
     */

    var m_contactList: ContactEdge? = null

    fun getContactList() = m_contactList

    /**
     * Get the total mass of the body.
     *
     * @return the mass, usually in kilograms (kg).
     */

    var m_mass: Float = 0.toFloat()

    fun getMass() = m_mass


    var m_invMass: Float = 0.toFloat()

    // Rotational inertia about the center of mass.

    var m_I: Float = 0.toFloat()

    var m_invI: Float = 0.toFloat()

    /** Get the linear damping of the body.  */
    /** Set the linear damping of the body.  */

    var m_linearDamping: Float = 0.toFloat()
    /** Get the angular damping of the body.  */
    /** Set the angular damping of the body.  */

    var m_angularDamping: Float = 0.toFloat()
    /**
     * Get the gravity scale of the body.
     *
     * @return
     */
    /**
     * Set the gravity scale of the body.
     *
     * @param gravityScale
     */

    var gravityScale: Float = 0.toFloat()

    var m_sleepTime: Float = 0.toFloat()

    /** Get the user data pointer that was provided in the body definition.  */
    /**
     * Set the user data. Use this to store your application specific data.
     */
    var userData: Any? = null

    private val fixDef = FixtureDef()

    /**
     * Get the world body origin position. Do not modify.
     *
     * @return the world position of the body's origin.
     */
    val position: Vec2
        get() = xf.p

    /**
     * Get the angle in radians.
     *
     * @return the current world rotation angle in radians.
     */
    var angleRadians: Float
        get() = sweep.a
        // @TODO: Check this is possible
        set(value) {
            sweep.a = value
        }

    var angle: Angle
        get() = angleRadians.radians
        set(value) {
            angleRadians = value.radians.toFloat()
        }

    /**
     * Get the angle in degrees.
     *
     * @return the current world rotation angle in degrees.
     */
    var angleDegrees: Float
        get() = angle.degrees.toFloat()
        set(value) {
            angle = value.degrees
        }


    /**
     * Get the world position of the center of mass. Do not modify.
     */
    val worldCenter: Vec2
        get() = sweep.c

    /**
     * Get the local position of the center of mass. Do not modify.
     */
    val localCenter: Vec2
        get() = sweep.localCenter

    /**
     * Get the linear velocity of the center of mass. Do not modify, instead use
     * [.setLinearVelocity].
     *
     * @return the linear velocity of the center of mass.
     */
    /**
     * Set the linear velocity of the center of mass.
     *
     * @param v the new linear velocity of the center of mass.
     */
    var linearVelocity: Vec2
        get() = _linearVelocity
        set(v) {
            if (_type === BodyType.STATIC) {
                return
            }

            if (Vec2.dot(v, v) > 0.0f) {
                isAwake = true
            }

            _linearVelocity.set(v)
        }

    var linearVelocityX: Float
        get() = linearVelocity.x
        set(v) {
            linearVelocity = linearVelocity.set(v, linearVelocity.y)
        }

    var linearVelocityY: Float
        get() = linearVelocity.y
        set(v) {
            linearVelocity = linearVelocity.set(linearVelocity.x, v)
        }

            /**
             * Get the angular velocity.
             *
             * @return the angular velocity in radians/second.
             */
            /**
             * Set the angular velocity.
             *
             * @param omega the new angular velocity in radians/second.
             */
            var angularVelocity: Float
            get() = _angularVelocity
            set(w) {
                if (_type === BodyType.STATIC) {
                    return
                }

                if (w * w > 0f) {
                    isAwake = true
                }

                _angularVelocity = w
            }

            /**
             * Get the central rotational inertia of the body.
             *
             * @return the rotational inertia, usually in kg-m^2.
             */
            val inertia: Float
            get() = m_I + m_mass * (sweep.localCenter.x * sweep.localCenter.x + sweep.localCenter.y * sweep.localCenter.y)

            private val pmd = MassData()

            /**
             * Set the type of this body. This may alter the mass and velocity.
             *
             * @param type
             */
            // Delete the attached contacts.
            // Touch the proxies so that new contacts will be created (when appropriate)
            var type: BodyType
            get() = _type
            set(type) {
                assert(world.isLocked == false)
                if (world.isLocked == true) {
                    return
                }

                if (_type === type) {
                    return
                }

                _type = type

                resetMassData()

                if (_type === BodyType.STATIC) {
                    _linearVelocity.setZero()
                    _angularVelocity = 0.0f
                    sweep.a0 = sweep.a
                    sweep.c0.set(sweep.c)
                    synchronizeFixtures()
                }

                isAwake = true

                force.setZero()
                torque = 0.0f
                var ce = m_contactList
                while (ce != null) {
                    val ce0 = ce
                    ce = ce.next
                    world.m_contactManager.destroy(ce0.contact!!)
                }
                m_contactList = null
                val broadPhase = world.m_contactManager.m_broadPhase
                var f = m_fixtureList
                while (f != null) {
                    val proxyCount = f.m_proxyCount
                    for (i in 0 until proxyCount) {
                        broadPhase.touchProxy(f.m_proxies!![i].proxyId)
                    }
                    f = f.m_next
                }
            }

            /** Is this body treated like a bullet for continuous collision detection?  */
            /** Should this body be treated like a bullet for continuous collision detection?  */
            var isBullet: Boolean
            get() = flags and e_bulletFlag == e_bulletFlag
            set(flag) = if (flag) {
                flags = flags or e_bulletFlag
            } else {
                flags = flags and e_bulletFlag.inv()
            }

            /**
             * Is this body allowed to sleep
             *
             * @return
             */
            /**
             * You can disable sleeping on this body. If you disable sleeping, the body will be woken.
             *
             * @param flag
             */
            var isSleepingAllowed: Boolean
            get() = flags and e_autoSleepFlag == e_autoSleepFlag
            set(flag) = if (flag) {
                flags = flags or e_autoSleepFlag
            } else {
                flags = flags and e_autoSleepFlag.inv()
                isAwake = true
            }

            /**
             * Get the sleeping state of this body.
             *
             * @return true if the body is awake.
             */
            /**
             * Set the sleep state of the body. A sleeping body has very low CPU cost.
             *
             * @param flag set to true to put body to sleep, false to wake it.
             * @param flag
             */
            var isAwake: Boolean
            get() = flags and e_awakeFlag == e_awakeFlag
            set(flag) {
                if (flag) {
                    if (flags and e_awakeFlag == 0) {
                        flags = flags or e_awakeFlag
                        m_sleepTime = 0.0f
                    }
                } else {
                    flags = flags and e_awakeFlag.inv()
                    m_sleepTime = 0.0f
                    _linearVelocity.setZero()
                    _angularVelocity = 0.0f
                    force.setZero()
                    torque = 0.0f
                }
            }

            /**
             * Get the active state of the body.
             *
             * @return
             */
            /**
             * Set the active state of the body. An inactive body is not simulated and cannot be collided with
             * or woken up. If you pass a flag of true, all fixtures will be added to the broad-phase. If you
             * pass a flag of false, all fixtures will be removed from the broad-phase and all contacts will
             * be destroyed. Fixtures and joints are otherwise unaffected. You may continue to create/destroy
             * fixtures and joints on inactive bodies. Fixtures on an inactive body are implicitly inactive
             * and will not participate in collisions, ray-casts, or queries. Joints connected to an inactive
             * body are implicitly inactive. An inactive body is still owned by a World object and remains in
             * the body list.
             *
             * @param flag
             */
            // Create all proxies.
            // Contacts are created the next time step.
            // Destroy all proxies.
            // Destroy the attached contacts.
            var isActive: Boolean
            get() = flags and e_activeFlag == e_activeFlag
            set(flag) {
                assert(world.isLocked == false)

                if (flag == isActive) {
                    return
                }

                if (flag) {
                    flags = flags or e_activeFlag
                    val broadPhase = world.m_contactManager.m_broadPhase
                    var f = m_fixtureList
                    while (f != null) {
                        f.createProxies(broadPhase, xf)
                        f = f.m_next
                    }
                } else {
                    flags = flags and e_activeFlag.inv()
                    val broadPhase = world.m_contactManager.m_broadPhase
                    var f = m_fixtureList
                    while (f != null) {
                        f.destroyProxies(broadPhase)
                        f = f.m_next
                    }
                    var ce = m_contactList
                    while (ce != null) {
                        val ce0 = ce
                        ce = ce.next
                        world.m_contactManager.destroy(ce0.contact!!)
                    }
                    m_contactList = null
                }
            }

            /**
             * Does this body have fixed rotation?
             *
             * @return
             */
            /**
             * Set this body to have fixed rotation. This causes the mass to be reset.
             *
             * @param flag
             */
            var isFixedRotation: Boolean
            get() = flags and e_fixedRotationFlag == e_fixedRotationFlag
            set(flag) {
                if (flag) {
                    flags = flags or e_fixedRotationFlag
                } else {
                    flags = flags and e_fixedRotationFlag.inv()
                }

                resetMassData()
            }

            // djm pooling
            private val pxf = Transform()


            init {
                assert(bd.position.isValid)
                assert(bd.linearVelocity.isValid)
                assert(bd.gravityScale >= 0.0f)
                assert(bd.angularDamping >= 0.0f)
                assert(bd.linearDamping >= 0.0f)

                flags = 0

                if (bd.bullet) {
                    flags = flags or e_bulletFlag
                }
                if (bd.fixedRotation) {
                    flags = flags or e_fixedRotationFlag
                }
                if (bd.allowSleep) {
                    flags = flags or e_autoSleepFlag
                }
                if (bd.awake) {
                    flags = flags or e_awakeFlag
                }
                if (bd.active) {
                    flags = flags or e_activeFlag
                }

                xf.p.set(bd.position)
                xf.q.setRadians(bd.angleRadians)

                sweep.localCenter.setZero()
                sweep.c0.set(xf.p)
                sweep.c.set(xf.p)
                sweep.a0 = bd.angleRadians
                sweep.a = bd.angleRadians
                sweep.alpha0 = 0.0f

                m_jointList = null
                m_contactList = null
                prev = null
                m_next = null

                _linearVelocity.set(bd.linearVelocity)
                _angularVelocity = bd.angularVelocity

                m_linearDamping = bd.linearDamping
                m_angularDamping = bd.angularDamping
                gravityScale = bd.gravityScale

                force.setZero()
                torque = 0.0f

                m_sleepTime = 0.0f

                _type = bd.type

                if (_type === BodyType.DYNAMIC) {
                    m_mass = 1f
                    m_invMass = 1f
                } else {
                    m_mass = 0f
                    m_invMass = 0f
                }

                m_I = 0.0f
                m_invI = 0.0f

                userData = bd.userData

                m_fixtureList = null
                m_fixtureCount = 0
            }

            /**
             * Creates a fixture and attach it to this body. Use this function if you need to set some fixture
             * parameters, like friction. Otherwise you can create the fixture directly from a shape. If the
             * density is non-zero, this function automatically updates the mass of the body. Contacts are not
             * created until the next time step.
             *
             * @param def the fixture definition.
             * @warning This function is locked during callbacks.
             */
            fun createFixture(def: FixtureDef): Fixture? {
                assert(world.isLocked == false)

                if (world.isLocked == true) {
                    return null
                }

                val fixture = Fixture()
                fixture.create(this, def)

                if (flags and e_activeFlag == e_activeFlag) {
                    val broadPhase = world.m_contactManager.m_broadPhase
                    fixture.createProxies(broadPhase, xf)
                }

                fixture.m_next = m_fixtureList
                m_fixtureList = fixture
                ++m_fixtureCount

                fixture.m_body = this

                // Adjust mass properties if needed.
                if (fixture.m_density > 0.0f) {
                    resetMassData()
                }

                // Let the world know we have a new fixture. This will cause new contacts
                // to be created at the beginning of the next time step.
                world.m_flags = world.m_flags or World.NEW_FIXTURE

                return fixture
            }

            /**
             * Creates a fixture from a shape and attach it to this body. This is a convenience function. Use
             * FixtureDef if you need to set parameters like friction, restitution, user data, or filtering.
             * If the density is non-zero, this function automatically updates the mass of the body.
             *
             * @param shape the shape to be cloned.
             * @param density the shape density (set to zero for static bodies).
             * @warning This function is locked during callbacks.
             */
            fun createFixture(shape: Shape, density: Float): Fixture? {
                fixDef.shape = shape
                fixDef.density = density

                return createFixture(fixDef)
            }

            /**
             * Destroy a fixture. This removes the fixture from the broad-phase and destroys all contacts
             * associated with this fixture. This will automatically adjust the mass of the body if the body
             * is dynamic and the fixture has positive density. All fixtures attached to a body are implicitly
             * destroyed when the body is destroyed.
             *
             * @param fixture the fixture to be removed.
             * @warning This function is locked during callbacks.
             */
            fun destroyFixture(fixture: Fixture?) {
                var fixture = fixture
                assert(world.isLocked == false)
                if (world.isLocked == true) {
                    return
                }

                assert(fixture!!.m_body === this)

                // Remove the fixture from this body's singly linked list.
                assert(m_fixtureCount > 0)
                var node = m_fixtureList
                var last: Fixture? = null // java change
                var found = false
                while (node != null) {
                    if (node === fixture) {
                        node = fixture.m_next
                        found = true
                        break
                    }
                    last = node
                    node = node.m_next
                }

                // You tried to remove a shape that is not attached to this body.
                assert(found)

                // java change, remove it from the list
                if (last == null) {
                    m_fixtureList = fixture!!.m_next
                } else {
                    last.m_next = fixture!!.m_next
                }

                // Destroy any contacts associated with the fixture.
                var edge = m_contactList
                while (edge != null) {
                    val c = edge.contact
                    edge = edge.next

                    val fixtureA = c!!.getFixtureA()
                    val fixtureB = c.getFixtureB()

                    if (fixture === fixtureA || fixture === fixtureB) {
                        // This destroys the contact and removes it from
                        // this body's contact list.
                        world.m_contactManager.destroy(c)
                    }
                }

                if (flags and e_activeFlag == e_activeFlag) {
                    val broadPhase = world.m_contactManager.m_broadPhase
                    fixture.destroyProxies(broadPhase)
                }

                fixture.destroy()
                fixture.m_body = null
                fixture.m_next = null
                fixture = null

                --m_fixtureCount

                // Reset the mass data.
                resetMassData()
            }

            /**
             * Set the position of the body's origin and rotation. This breaks any contacts and wakes the
             * other bodies. Manipulating a body's transform may cause non-physical behavior. Note: contacts
             * are updated on the next call to World.step().
             *
             * @param position the world position of the body's local origin.
             * @param angleRadians the world rotation in radians.
             */
            fun setTransformRadians(position: Vec2, angleRadians: Float) {
                assert(world.isLocked == false)
                if (world.isLocked == true) {
                    return
                }

                xf.q.setRadians(angleRadians)
                xf.p.set(position)

                // m_sweep.c0 = m_sweep.c = Mul(m_xf, m_sweep.localCenter);
                Transform.mulToOutUnsafe(xf, sweep.localCenter, sweep.c)
                sweep.a = angleRadians

                sweep.c0.set(sweep.c)
                sweep.a0 = sweep.a

                val broadPhase = world.m_contactManager.m_broadPhase
                var f = m_fixtureList
                while (f != null) {
                    f.synchronize(broadPhase, xf, xf)
                    f = f.m_next
                }
            }

            /**
             * Set the position of the body's origin and rotation. This breaks any contacts and wakes the
             * other bodies. Manipulating a body's transform may cause non-physical behavior. Note: contacts
             * are updated on the next call to World.step().
             *
             * @param position the world position of the body's local origin.
             * @param angleDegrees the world rotation in degrees.
             */
            fun setTransformDegrees(position: Vec2, angleDegrees: Float) = setTransformRadians(position, angleDegrees * MathUtils.DEG2RAD)

            /**
             * Set the position of the body's origin and rotation. This breaks any contacts and wakes the
             * other bodies. Manipulating a body's transform may cause non-physical behavior. Note: contacts
             * are updated on the next call to World.step().
             *
             * @param position the world position of the body's local origin.
             * @param angle the world rotation.
             */
            fun setTransform(position: Vec2, angle: Angle) = setTransformRadians(position, angle.radians.toFloat())

            /**
             * Apply a force at a world point. If the force is not applied at the center of mass, it will
             * generate a torque and affect the angular velocity. This wakes up the body.
             *
             * @param force the world force vector, usually in Newtons (N).
             * @param point the world position of the point of application.
             */
            fun applyForce(force: Vec2, point: Vec2) {
                if (_type !== BodyType.DYNAMIC) {
                    return
                }

                if (isAwake == false) {
                    isAwake = true
                }

                // m_force.addLocal(force);
                // Vec2 temp = tltemp.get();
                // temp.set(point).subLocal(m_sweep.c);
                // m_torque += Vec2.cross(temp, force);

                this.force.x += force.x
                this.force.y += force.y

                torque += (point.x - sweep.c.x) * force.y - (point.y - sweep.c.y) * force.x
            }

            /**
             * Apply a force to the center of mass. This wakes up the body.
             *
             * @param force the world force vector, usually in Newtons (N).
             */
            fun applyForceToCenter(force: Vec2) {
                if (_type !== BodyType.DYNAMIC) {
                    return
                }

                if (isAwake == false) {
                    isAwake = true
                }

                this.force.x += force.x
                this.force.y += force.y
            }

            /**
             * Apply a torque. This affects the angular velocity without affecting the linear velocity of the
             * center of mass. This wakes up the body.
             *
             * @param torque about the z-axis (out of the screen), usually in N-m.
             */
            fun applyTorque(torque: Float) {
                if (_type !== BodyType.DYNAMIC) {
                    return
                }

                if (isAwake == false) {
                    isAwake = true
                }

                this.torque += torque
            }

            /**
             * Apply an impulse at a point. This immediately modifies the velocity. It also modifies the
             * angular velocity if the point of application is not at the center of mass. This wakes up the
             * body if 'wake' is set to true. If the body is sleeping and 'wake' is false, then there is no
             * effect.
             *
             * @param impulse the world impulse vector, usually in N-seconds or kg-m/s.
             * @param point the world position of the point of application.
             * @param wake also wake up the body
             */
            fun applyLinearImpulse(impulse: Vec2, point: Vec2, wake: Boolean) {
                if (_type !== BodyType.DYNAMIC) {
                    return
                }

                if (!isAwake) {
                    if (wake) {
                        isAwake = true
                    } else {
                        return
                    }
                }

                _linearVelocity.x += impulse.x * m_invMass
                _linearVelocity.y += impulse.y * m_invMass

                _angularVelocity += m_invI * ((point.x - sweep.c.x) * impulse.y - (point.y - sweep.c.y) * impulse.x)
            }

            /**
             * Apply an angular impulse.
             *
             * @param impulse the angular impulse in units of kg*m*m/s
             */
            fun applyAngularImpulse(impulse: Float) {
                if (_type !== BodyType.DYNAMIC) {
                    return
                }

                if (isAwake == false) {
                    isAwake = true
                }
                _angularVelocity += m_invI * impulse
            }

            /**
             * Get the mass data of the body. The rotational inertia is relative to the center of mass.
             *
             * @return a struct containing the mass, inertia and center of the body.
             */
            fun getMassData(data: MassData) {
                // data.mass = m_mass;
                // data.I = m_I + m_mass * Vec2.dot(m_sweep.localCenter, m_sweep.localCenter);
                // data.center.set(m_sweep.localCenter);

                data.mass = m_mass
                data.I = m_I + m_mass * (sweep.localCenter.x * sweep.localCenter.x + sweep.localCenter.y * sweep.localCenter.y)
                data.center.x = sweep.localCenter.x
                data.center.y = sweep.localCenter.y
            }

            /**
             * Set the mass properties to override the mass properties of the fixtures. Note that this changes
             * the center of mass position. Note that creating or destroying fixtures can also alter the mass.
             * This function has no effect if the body isn't dynamic.
             *
             * @param massData the mass properties.
             */
            fun setMassData(massData: MassData) {
                // TODO_ERIN adjust linear velocity and torque to account for movement of center.
                assert(world.isLocked == false)
                if (world.isLocked == true) {
                    return
                }

                if (_type !== BodyType.DYNAMIC) {
                    return
                }

                m_invMass = 0.0f
                m_I = 0.0f
                m_invI = 0.0f

                m_mass = massData.mass
                if (m_mass <= 0.0f) {
                    m_mass = 1f
                }

                m_invMass = 1.0f / m_mass

                if (massData.I > 0.0f && flags and e_fixedRotationFlag == 0) {
                    m_I = massData.I - m_mass * Vec2.dot(massData.center, massData.center)
                    assert(m_I > 0.0f)
                    m_invI = 1.0f / m_I
                }

                val oldCenter = world.pool.popVec2()
                // Move center of mass.
                oldCenter.set(sweep.c)
                sweep.localCenter.set(massData.center)
                // m_sweep.c0 = m_sweep.c = Mul(m_xf, m_sweep.localCenter);
                Transform.mulToOutUnsafe(xf, sweep.localCenter, sweep.c0)
                sweep.c.set(sweep.c0)

                // Update center of mass velocity.
                // m_linearVelocity += Cross(m_angularVelocity, m_sweep.c - oldCenter);
                val temp = world.pool.popVec2()
                temp.set(sweep.c).subLocal(oldCenter)
                Vec2.crossToOut(_angularVelocity, temp, temp)
                _linearVelocity.addLocal(temp)

                world.pool.pushVec2(2)
            }

            /**
             * This resets the mass properties to the sum of the mass properties of the fixtures. This
             * normally does not need to be called unless you called setMassData to override the mass and you
             * later want to reset the mass.
             */
            fun resetMassData() {
                // Compute mass data from shapes. Each shape has its own density.
                m_mass = 0.0f
                m_invMass = 0.0f
                m_I = 0.0f
                m_invI = 0.0f
                sweep.localCenter.setZero()

                // Static and kinematic bodies have zero mass.
                if (_type === BodyType.STATIC || _type === BodyType.KINEMATIC) {
                    // m_sweep.c0 = m_sweep.c = m_xf.position;
                    sweep.c0.set(xf.p)
                    sweep.c.set(xf.p)
                    sweep.a0 = sweep.a
                    return
                }

                assert(_type === BodyType.DYNAMIC)

                // Accumulate mass over all fixtures.
                val localCenter = world.pool.popVec2()
                localCenter.setZero()
                val temp = world.pool.popVec2()
                val massData = pmd
                var f = m_fixtureList
                while (f != null) {
                    if (f.m_density == 0.0f) {
                        f = f.m_next
                        continue
                    }
                    f.getMassData(massData)
                    m_mass += massData.mass
                    // center += massData.mass * massData.center;
                    temp.set(massData.center).mulLocal(massData.mass)
                    localCenter.addLocal(temp)
                    m_I += massData.I
                    f = f.m_next
                }

                // Compute center of mass.
                if (m_mass > 0.0f) {
                    m_invMass = 1.0f / m_mass
                    localCenter.mulLocal(m_invMass)
                } else {
                    // Force all dynamic bodies to have a positive mass.
                    m_mass = 1.0f
                    m_invMass = 1.0f
                }

                if (m_I > 0.0f && flags and e_fixedRotationFlag == 0) {
                    // Center the inertia about the center of mass.
                    m_I -= m_mass * Vec2.dot(localCenter, localCenter)
                    assert(m_I > 0.0f)
                    m_invI = 1.0f / m_I
                } else {
                    m_I = 0.0f
                    m_invI = 0.0f
                }

                val oldCenter = world.pool.popVec2()
                // Move center of mass.
                oldCenter.set(sweep.c)
                sweep.localCenter.set(localCenter)
                // m_sweep.c0 = m_sweep.c = Mul(m_xf, m_sweep.localCenter);
                Transform.mulToOutUnsafe(xf, sweep.localCenter, sweep.c0)
                sweep.c.set(sweep.c0)

                // Update center of mass velocity.
                // m_linearVelocity += Cross(m_angularVelocity, m_sweep.c - oldCenter);
                temp.set(sweep.c).subLocal(oldCenter)

                val temp2 = oldCenter
                Vec2.crossToOutUnsafe(_angularVelocity, temp, temp2)
                _linearVelocity.addLocal(temp2)

                world.pool.pushVec2(3)
            }

            /**
             * Get the world coordinates of a point given the local coordinates.
             *
             * @param localPoint a point on the body measured relative the the body's origin.
             * @return the same point expressed in world coordinates.
             */
            fun getWorldPoint(localPoint: Vec2): Vec2 {
                val v = Vec2()
                getWorldPointToOut(localPoint, v)
                return v
            }

            fun getWorldPointToOut(localPoint: Vec2, out: Vec2) {
                Transform.mulToOut(xf, localPoint, out)
            }

            /**
             * Get the world coordinates of a vector given the local coordinates.
             *
             * @param localVector a vector fixed in the body.
             * @return the same vector expressed in world coordinates.
             */
            fun getWorldVector(localVector: Vec2): Vec2 {
                val out = Vec2()
                getWorldVectorToOut(localVector, out)
                return out
            }

            fun getWorldVectorToOut(localVector: Vec2, out: Vec2) {
                Rot.mulToOut(xf.q, localVector, out)
            }

            fun getWorldVectorToOutUnsafe(localVector: Vec2, out: Vec2) {
                Rot.mulToOutUnsafe(xf.q, localVector, out)
            }

            /**
             * Gets a local point relative to the body's origin given a world point.
             *
             * @param a point in world coordinates.
             * @return the corresponding local point relative to the body's origin.
             */
            fun getLocalPoint(worldPoint: Vec2): Vec2 {
                val out = Vec2()
                getLocalPointToOut(worldPoint, out)
                return out
            }

            fun getLocalPointToOut(worldPoint: Vec2, out: Vec2) {
                Transform.mulTransToOut(xf, worldPoint, out)
            }

            /**
             * Gets a local vector given a world vector.
             *
             * @param a vector in world coordinates.
             * @return the corresponding local vector.
             */
            fun getLocalVector(worldVector: Vec2): Vec2 {
                val out = Vec2()
                getLocalVectorToOut(worldVector, out)
                return out
            }

            fun getLocalVectorToOut(worldVector: Vec2, out: Vec2) {
                Rot.mulTrans(xf.q, worldVector, out)
            }

            fun getLocalVectorToOutUnsafe(worldVector: Vec2, out: Vec2) {
                Rot.mulTransUnsafe(xf.q, worldVector, out)
            }

            /**
             * Get the world linear velocity of a world point attached to this body.
             *
             * @param a point in world coordinates.
             * @return the world velocity of a point.
             */
            fun getLinearVelocityFromWorldPoint(worldPoint: Vec2): Vec2 {
                val out = Vec2()
                getLinearVelocityFromWorldPointToOut(worldPoint, out)
                return out
            }

            fun getLinearVelocityFromWorldPointToOut(worldPoint: Vec2, out: Vec2) {
                val tempX = worldPoint.x - sweep.c.x
                val tempY = worldPoint.y - sweep.c.y
                out.x = -_angularVelocity * tempY + _linearVelocity.x
                out.y = _angularVelocity * tempX + _linearVelocity.y
            }

            /**
             * Get the world velocity of a local point.
             *
             * @param a point in local coordinates.
             * @return the world velocity of a point.
             */
            fun getLinearVelocityFromLocalPoint(localPoint: Vec2): Vec2 {
                val out = Vec2()
                getLinearVelocityFromLocalPointToOut(localPoint, out)
                return out
            }

            fun getLinearVelocityFromLocalPointToOut(localPoint: Vec2, out: Vec2) {
                getWorldPointToOut(localPoint, out)
                getLinearVelocityFromWorldPointToOut(out, out)
            }

            fun synchronizeFixtures() {
                val xf1 = pxf
                // xf1.position = m_sweep.c0 - Mul(xf1.R, m_sweep.localCenter);

                // xf1.q.set(m_sweep.a0);
                // Rot.mulToOutUnsafe(xf1.q, m_sweep.localCenter, xf1.p);
                // xf1.p.mulLocal(-1).addLocal(m_sweep.c0);
                // inlined:
                xf1.q.s = MathUtils.sin(sweep.a0)
                xf1.q.c = MathUtils.cos(sweep.a0)
                xf1.p.x = sweep.c0.x - xf1.q.c * sweep.localCenter.x + xf1.q.s * sweep.localCenter.y
                xf1.p.y = sweep.c0.y - xf1.q.s * sweep.localCenter.x - xf1.q.c * sweep.localCenter.y
                // end inline

                var f = m_fixtureList
                while (f != null) {
                    f.synchronize(world.m_contactManager.m_broadPhase, xf1, xf)
                    f = f.m_next
                }
            }

            fun synchronizeTransform() {
                // m_xf.q.set(m_sweep.a);
                //
                // // m_xf.position = m_sweep.c - Mul(m_xf.R, m_sweep.localCenter);
                // Rot.mulToOutUnsafe(m_xf.q, m_sweep.localCenter, m_xf.p);
                // m_xf.p.mulLocal(-1).addLocal(m_sweep.c);
                //
                xf.q.s = MathUtils.sin(sweep.a)
                xf.q.c = MathUtils.cos(sweep.a)
                val q = xf.q
                val v = sweep.localCenter
                xf.p.x = sweep.c.x - q.c * v.x + q.s * v.y
                xf.p.y = sweep.c.y - q.s * v.x - q.c * v.y
            }

            /**
             * This is used to prevent connected bodies from colliding. It may lie, depending on the
             * collideConnected flag.
             *
             * @param other
             * @return
             */
            fun shouldCollide(other: Body): Boolean {
                // At least one body should be dynamic.
                if (_type !== BodyType.DYNAMIC && other._type !== BodyType.DYNAMIC) {
                    return false
                }

                // Does a joint prevent collision?
                var jn = m_jointList
                while (jn != null) {
                    if (jn.other === other) {
                        if (jn.joint!!.getCollideConnected() == false) {
                            return false
                        }
                    }
                    jn = jn.next
                }

                return true
            }

            fun advance(t: Float) {
                // Advance to the new safe time. This doesn't sync the broad-phase.
                sweep.advance(t)
                sweep.c.set(sweep.c0)
                sweep.a = sweep.a0
                xf.q.setRadians(sweep.a)
                // m_xf.position = m_sweep.c - Mul(m_xf.R, m_sweep.localCenter);
                Rot.mulToOutUnsafe(xf.q, sweep.localCenter, xf.p)
                xf.p.mulLocal(-1f).addLocal(sweep.c)
            }

            companion object {

            val e_islandFlag = 0x0001

            val e_awakeFlag = 0x0002

            val e_autoSleepFlag = 0x0004

            val e_bulletFlag = 0x0008

            val e_fixedRotationFlag = 0x0010

            val e_activeFlag = 0x0020

            val e_toiFlag = 0x0040
        }
        }

