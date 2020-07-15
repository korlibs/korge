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
package org.jbox2d.collision

import org.jbox2d.collision.Distance.DistanceProxy
import org.jbox2d.collision.Distance.SimplexCache
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Sweep
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.jbox2d.internal.*
import org.jbox2d.pooling.IWorldPool

/**
 * Class used for computing the time of impact. This class should not be constructed usually, just
 * retrieve from the [IWorldPool.getTimeOfImpact].
 *
 * @author daniel
 */
class TimeOfImpact(private val pool: IWorldPool, private val stats: TimeOfImpact.Stats = TimeOfImpact.Stats()) {


    // djm pooling
    private val cache = SimplexCache()
    private val distanceInput = DistanceInput()
    private val xfA = Transform()
    private val xfB = Transform()
    private val distanceOutput = DistanceOutput()
    private val fcn = SeparationFunction()
    private val indexes = IntArray(2)
    private val sweepA = Sweep()
    private val sweepB = Sweep()

    /**
     * Input parameters for TOI
     *
     * @author Daniel Murphy
     */
    class TOIInput {

        val proxyA = DistanceProxy()

        val proxyB = DistanceProxy()

        val sweepA = Sweep()

        val sweepB = Sweep()
        /**
         * defines sweep interval [0, tMax]
         */

        var tMax: Float = 0.toFloat()
    }

    enum class TOIOutputState {
        UNKNOWN, FAILED, OVERLAPPED, TOUCHING, SEPARATED
    }

    /**
     * Output parameters for TimeOfImpact
     *
     * @author daniel
     */
    class TOIOutput {

        var state: TOIOutputState? = null

        var t: Float = 0.toFloat()
    }

    /**
     * Compute the upper bound on time before two shapes penetrate. Time is represented as a fraction
     * between [0,tMax]. This uses a swept separating axis and may miss some intermediate,
     * non-tunneling collision. If you change the time interval, you should call this function again.
     * Note: use Distance to compute the contact point and normal at the time of impact.
     *
     * @param output
     * @param input
     */
    fun timeOfImpact(output: TOIOutput, input: TOIInput) {
        // CCD via the local separating axis method. This seeks progression
        // by computing the largest time at which separation is maintained.

        ++stats.toiCalls

        output.state = TOIOutputState.UNKNOWN
        output.t = input.tMax

        val proxyA = input.proxyA
        val proxyB = input.proxyB

        sweepA.set(input.sweepA)
        sweepB.set(input.sweepB)

        // Large rotations can make the root finder fail, so we normalize the
        // sweep angles.
        sweepA.normalize()
        sweepB.normalize()

        val tMax = input.tMax

        val totalRadius = proxyA.m_radius + proxyB.m_radius
        // djm: whats with all these constants?
        val target = MathUtils.max(Settings.linearSlop, totalRadius - 3.0f * Settings.linearSlop)
        val tolerance = 0.25f * Settings.linearSlop

        assert(target > tolerance)

        var t1 = 0f
        var iter = 0

        cache.count = 0
        distanceInput.proxyA = input.proxyA
        distanceInput.proxyB = input.proxyB
        distanceInput.useRadii = false

        // The outer loop progressively attempts to compute new separating axes.
        // This loop terminates when an axis is repeated (no progress is made).
        while (true) {
            sweepA.getTransform(xfA, t1)
            sweepB.getTransform(xfB, t1)
            // System.out.printf("sweepA: %f, %f, sweepB: %f, %f\n",
            // sweepA.c.x, sweepA.c.y, sweepB.c.x, sweepB.c.y);
            // Get the distance between shapes. We can also use the results
            // to get a separating axis
            distanceInput.transformA = xfA
            distanceInput.transformB = xfB
            pool.distance.distance(distanceOutput, cache, distanceInput)

            // System.out.printf("Dist: %f at points %f, %f and %f, %f.  %d iterations\n",
            // distanceOutput.distance, distanceOutput.pointA.x, distanceOutput.pointA.y,
            // distanceOutput.pointB.x, distanceOutput.pointB.y,
            // distanceOutput.iterations);

            // If the shapes are overlapped, we give up on continuous collision.
            if (distanceOutput.distance <= 0f) {
                // Failure!
                output.state = TOIOutputState.OVERLAPPED
                output.t = 0f
                break
            }

            if (distanceOutput.distance < target + tolerance) {
                // Victory!
                output.state = TOIOutputState.TOUCHING
                output.t = t1
                break
            }

            // Initialize the separating axis.
            fcn.initialize(cache, proxyA, sweepA, proxyB, sweepB, t1)

            // Compute the TOI on the separating axis. We do this by successively
            // resolving the deepest point. This loop is bounded by the number of
            // vertices.
            var done = false
            var t2 = tMax
            var pushBackIter = 0
            while (true) {

                // Find the deepest point at t2. Store the witness point indices.
                var s2 = fcn.findMinSeparation(indexes, t2)
                // System.out.printf("s2: %f\n", s2);
                // Is the final configuration separated?
                if (s2 > target + tolerance) {
                    // Victory!
                    output.state = TOIOutputState.SEPARATED
                    output.t = tMax
                    done = true
                    break
                }

                // Has the separation reached tolerance?
                if (s2 > target - tolerance) {
                    // Advance the sweeps
                    t1 = t2
                    break
                }

                // Compute the initial separation of the witness points.
                var s1 = fcn.evaluate(indexes[0], indexes[1], t1)
                // Check for initial overlap. This might happen if the root finder
                // runs out of iterations.
                // System.out.printf("s1: %f, target: %f, tolerance: %f\n", s1, target,
                // tolerance);
                if (s1 < target - tolerance) {
                    output.state = TOIOutputState.FAILED
                    output.t = t1
                    done = true
                    break
                }

                // Check for touching
                if (s1 <= target + tolerance) {
                    // Victory! t1 should hold the TOI (could be 0.0).
                    output.state = TOIOutputState.TOUCHING
                    output.t = t1
                    done = true
                    break
                }

                // Compute 1D root of: f(x) - target = 0
                var rootIterCount = 0
                var a1 = t1
                var a2 = t2
                while (true) {
                    // Use a mix of the secant rule and bisection.
                    val t: Float
                    if (rootIterCount and 1 == 1) {
                        // Secant rule to improve convergence.
                        t = a1 + (target - s1) * (a2 - a1) / (s2 - s1)
                    } else {
                        // Bisection to guarantee progress.
                        t = 0.5f * (a1 + a2)
                    }

                    ++rootIterCount
                    ++stats.toiRootIters

                    val s = fcn.evaluate(indexes[0], indexes[1], t)

                    if (MathUtils.abs(s - target) < tolerance) {
                        // t2 holds a tentative value for t1
                        t2 = t
                        break
                    }

                    // Ensure we continue to bracket the root.
                    if (s > target) {
                        a1 = t
                        s1 = s
                    } else {
                        a2 = t
                        s2 = s
                    }

                    if (rootIterCount == MAX_ROOT_ITERATIONS) {
                        break
                    }
                }

                stats.toiMaxRootIters = MathUtils.max(stats.toiMaxRootIters, rootIterCount)

                ++pushBackIter

                if (pushBackIter == Settings.maxPolygonVertices || rootIterCount == MAX_ROOT_ITERATIONS) {
                    break
                }
            }

            ++iter
            ++stats.toiIters

            if (done) {
                // System.out.println("done");
                break
            }

            if (iter == MAX_ITERATIONS) {
                // System.out.println("failed, root finder stuck");
                // Root finder got stuck. Semi-victory.
                output.state = TOIOutputState.FAILED
                output.t = t1
                break
            }
        }

        // System.out.printf("final sweeps: %f, %f, %f; %f, %f, %f", input.s)
        stats.toiMaxIters = MathUtils.max(stats.toiMaxIters, iter)
    }

    companion object {
        val MAX_ITERATIONS = 20
        val MAX_ROOT_ITERATIONS = 50
    }

    class Stats {
        var toiCalls = 0
        var toiIters = 0
        var toiMaxIters = 0
        var toiRootIters = 0
        var toiMaxRootIters = 0
    }
}


internal enum class Type {
    POINTS, FACE_A, FACE_B
}


internal class SeparationFunction {

    lateinit var m_proxyA: DistanceProxy
    lateinit var m_proxyB: DistanceProxy
    lateinit var m_type: Type
    val m_localPoint = Vec2()
    val m_axis = Vec2()
    lateinit var m_sweepA: Sweep
    lateinit var m_sweepB: Sweep

    // djm pooling
    private val localPointA = Vec2()
    private val localPointB = Vec2()
    private val pointA = Vec2()
    private val pointB = Vec2()
    private val localPointA1 = Vec2()
    private val localPointA2 = Vec2()
    private val normal = Vec2()
    private val localPointB1 = Vec2()
    private val localPointB2 = Vec2()
    private val temp = Vec2()
    private val xfa = Transform()
    private val xfb = Transform()

    private val axisA = Vec2()
    private val axisB = Vec2()

    // TODO_ERIN might not need to return the separation

    fun initialize(cache: SimplexCache, proxyA: DistanceProxy, sweepA: Sweep,
                   proxyB: DistanceProxy, sweepB: Sweep, t1: Float): Float {
        m_proxyA = proxyA
        m_proxyB = proxyB
        val count = cache.count
        assert(0 < count && count < 3)

        m_sweepA = sweepA
        m_sweepB = sweepB

        m_sweepA.getTransform(xfa, t1)
        m_sweepB.getTransform(xfb, t1)

        // log.debug("initializing separation.\n" +
        // "cache: "+cache.count+"-"+cache.metric+"-"+cache.indexA+"-"+cache.indexB+"\n"
        // "distance: "+proxyA.

        if (count == 1) {
            m_type = Type.POINTS
            /*
       * Vec2 localPointA = m_proxyA.GetVertex(cache.indexA[0]); Vec2 localPointB =
       * m_proxyB.GetVertex(cache.indexB[0]); Vec2 pointA = Mul(transformA, localPointA); Vec2
       * pointB = Mul(transformB, localPointB); m_axis = pointB - pointA; m_axis.Normalize();
       */
            localPointA.set(m_proxyA.getVertex(cache.indexA[0]))
            localPointB.set(m_proxyB.getVertex(cache.indexB[0]))
            Transform.mulToOutUnsafe(xfa, localPointA, pointA)
            Transform.mulToOutUnsafe(xfb, localPointB, pointB)
            m_axis.set(pointB).subLocal(pointA)
            val s = m_axis.normalize()
            return s
        } else if (cache.indexA[0] == cache.indexA[1]) {
            // Two points on B and one on A.
            m_type = Type.FACE_B

            localPointB1.set(m_proxyB.getVertex(cache.indexB[0]))
            localPointB2.set(m_proxyB.getVertex(cache.indexB[1]))

            temp.set(localPointB2).subLocal(localPointB1)
            Vec2.crossToOutUnsafe(temp, 1f, m_axis)
            m_axis.normalize()

            Rot.mulToOutUnsafe(xfb.q, m_axis, normal)

            m_localPoint.set(localPointB1).addLocal(localPointB2).mulLocal(.5f)
            Transform.mulToOutUnsafe(xfb, m_localPoint, pointB)

            localPointA.set(proxyA.getVertex(cache.indexA[0]))
            Transform.mulToOutUnsafe(xfa, localPointA, pointA)

            temp.set(pointA).subLocal(pointB)
            var s = Vec2.dot(temp, normal)
            if (s < 0.0f) {
                m_axis.negateLocal()
                s = -s
            }
            return s
        } else {
            // Two points on A and one or two points on B.
            m_type = Type.FACE_A

            localPointA1.set(m_proxyA.getVertex(cache.indexA[0]))
            localPointA2.set(m_proxyA.getVertex(cache.indexA[1]))

            temp.set(localPointA2).subLocal(localPointA1)
            Vec2.crossToOutUnsafe(temp, 1.0f, m_axis)
            m_axis.normalize()

            Rot.mulToOutUnsafe(xfa.q, m_axis, normal)

            m_localPoint.set(localPointA1).addLocal(localPointA2).mulLocal(.5f)
            Transform.mulToOutUnsafe(xfa, m_localPoint, pointA)

            localPointB.set(m_proxyB.getVertex(cache.indexB[0]))
            Transform.mulToOutUnsafe(xfb, localPointB, pointB)

            temp.set(pointB).subLocal(pointA)
            var s = Vec2.dot(temp, normal)
            if (s < 0.0f) {
                m_axis.negateLocal()
                s = -s
            }
            return s
        }
    }

    // float FindMinSeparation(int* indexA, int* indexB, float t) const
    fun findMinSeparation(indexes: IntArray, t: Float): Float {

        m_sweepA.getTransform(xfa, t)
        m_sweepB.getTransform(xfb, t)

        when (m_type) {
            Type.POINTS -> {
                Rot.mulTransUnsafe(xfa.q, m_axis, axisA)
                Rot.mulTransUnsafe(xfb.q, m_axis.negateLocal(), axisB)
                m_axis.negateLocal()

                indexes[0] = m_proxyA.getSupport(axisA)
                indexes[1] = m_proxyB.getSupport(axisB)

                localPointA.set(m_proxyA.getVertex(indexes[0]))
                localPointB.set(m_proxyB.getVertex(indexes[1]))

                Transform.mulToOutUnsafe(xfa, localPointA, pointA)
                Transform.mulToOutUnsafe(xfb, localPointB, pointB)

                val separation = Vec2.dot(pointB.subLocal(pointA), m_axis)
                return separation
            }
            Type.FACE_A -> {
                Rot.mulToOutUnsafe(xfa.q, m_axis, normal)
                Transform.mulToOutUnsafe(xfa, m_localPoint, pointA)

                Rot.mulTransUnsafe(xfb.q, normal.negateLocal(), axisB)
                normal.negateLocal()

                indexes[0] = -1
                indexes[1] = m_proxyB.getSupport(axisB)

                localPointB.set(m_proxyB.getVertex(indexes[1]))
                Transform.mulToOutUnsafe(xfb, localPointB, pointB)

                val separation = Vec2.dot(pointB.subLocal(pointA), normal)
                return separation
            }
            Type.FACE_B -> {
                Rot.mulToOutUnsafe(xfb.q, m_axis, normal)
                Transform.mulToOutUnsafe(xfb, m_localPoint, pointB)

                Rot.mulTransUnsafe(xfa.q, normal.negateLocal(), axisA)
                normal.negateLocal()

                indexes[1] = -1
                indexes[0] = m_proxyA.getSupport(axisA)

                localPointA.set(m_proxyA.getVertex(indexes[0]))
                Transform.mulToOutUnsafe(xfa, localPointA, pointA)

                val separation = Vec2.dot(pointA.subLocal(pointB), normal)
                return separation
            }
            else -> {
                assert(false)
                indexes[0] = -1
                indexes[1] = -1
                return 0f
            }
        }
    }

    fun evaluate(indexA: Int, indexB: Int, t: Float): Float {
        m_sweepA.getTransform(xfa, t)
        m_sweepB.getTransform(xfb, t)

        when (m_type) {
            Type.POINTS -> {
                localPointA.set(m_proxyA.getVertex(indexA))
                localPointB.set(m_proxyB.getVertex(indexB))

                Transform.mulToOutUnsafe(xfa, localPointA, pointA)
                Transform.mulToOutUnsafe(xfb, localPointB, pointB)

                val separation = Vec2.dot(pointB.subLocal(pointA), m_axis)
                return separation
            }
            Type.FACE_A -> {
                Rot.mulToOutUnsafe(xfa.q, m_axis, normal)
                Transform.mulToOutUnsafe(xfa, m_localPoint, pointA)

                localPointB.set(m_proxyB.getVertex(indexB))
                Transform.mulToOutUnsafe(xfb, localPointB, pointB)
                val separation = Vec2.dot(pointB.subLocal(pointA), normal)
                return separation
            }
            Type.FACE_B -> {
                Rot.mulToOutUnsafe(xfb.q, m_axis, normal)
                Transform.mulToOutUnsafe(xfb, m_localPoint, pointB)

                localPointA.set(m_proxyA.getVertex(indexA))
                Transform.mulToOutUnsafe(xfa, localPointA, pointA)

                val separation = Vec2.dot(pointA.subLocal(pointB), normal)
                return separation
            }
            else -> {
                assert(false)
                return 0f
            }
        }
    }
}
