package org.jbox2d.particle

import org.jbox2d.common.MathUtils
import org.jbox2d.common.Vec2
import org.jbox2d.internal.*
import org.jbox2d.pooling.normal.MutableStack

class VoronoiDiagram(generatorCapacity: Int) {

    private val m_generatorBuffer: Array<Generator> = Array(generatorCapacity) { Generator() }
    private var m_generatorCount: Int = 0
    private var m_countX: Int = 0
    private var m_countY: Int = 0
    // The diagram is an array of "pointers".
    private var m_diagram: Array<Generator?>? = null

    private val lower = Vec2()
    private val upper = Vec2()
    /*
    e: java.lang.IllegalStateException: Cannot get FQ name of local class: class <no name provided> : org.jbox2d.pooling.normal.MutableStack<org.jbox2d.particle.VoronoiDiagram.VoronoiDiagramTask> defined in private final val taskPool: <no name provided> defined in org.jbox2d.particle.VoronoiDiagram
        at org.jetbrains.kotlin.serialization.DescriptorAwareStringTable$DefaultImpls.getFqNameIndex(DescriptorAwareStringTable.kt:23)
        at org.jetbrains.kotlin.serialization.StringTableImpl.getFqNameIndex(StringTableImpl.kt:25)
        at org.jetbrains.kotlin.serialization.DescriptorSerializer.getClassifierId(DescriptorSerializer.kt:718)
        at org.jetbrains.kotlin.serialization.DescriptorSerializer.fillFromPossiblyInnerType(DescriptorSerializer.kt:590)
        at org.jetbrains.kotlin.serialization.DescriptorSerializer.type$serialization(DescriptorSerializer.kt:555)
        at org.jetbrains.kotlin.serialization.DescriptorSerializer.typeId$serialization(DescriptorSerializer.kt:520)
        at org.jetbrains.kotlin.serialization.DescriptorSerializer.propertyProto(DescriptorSerializer.kt:242)
     */
    //private val taskPool = object : MutableStack<VoronoiDiagram.VoronoiDiagramTask>(50) {
    //    override fun newInstance(): VoronoiDiagramTask = VoronoiDiagramTask()
    //    override fun newArray(size: Int): Array<VoronoiDiagramTask> =
    //        arrayOfNulls<VoronoiDiagramTask>(size) as Array<VoronoiDiagramTask>
    //}

    val taskPool: VoronoiDiagramTaskMutableStack = VoronoiDiagramTaskMutableStack()

    class VoronoiDiagramTaskMutableStack : MutableStack<VoronoiDiagram.VoronoiDiagramTask>(50) {
        override fun newInstance(): VoronoiDiagramTask = VoronoiDiagramTask()
        override fun newArray(size: Int): Array<VoronoiDiagramTask> =
            arrayOfNulls<VoronoiDiagramTask>(size) as Array<VoronoiDiagramTask>
    }

    private val queue = StackQueue<VoronoiDiagramTask>()

    class Generator {
        internal val center = Vec2()
        internal var tag: Int = 0
    }

    class VoronoiDiagramTask {
        internal var m_x: Int = 0
        internal var m_y: Int = 0
        internal var m_i: Int = 0
        internal var m_generator: Generator? = null

        constructor() {}

        constructor(x: Int, y: Int, i: Int, g: Generator) {
            m_x = x
            m_y = y
            m_i = i
            m_generator = g
        }

        fun set(x: Int, y: Int, i: Int, g: Generator): VoronoiDiagramTask {
            m_x = x
            m_y = y
            m_i = i
            m_generator = g
            return this
        }
    }

    interface VoronoiDiagramCallback {
        fun callback(aTag: Int, bTag: Int, cTag: Int)
    }

    fun getNodes(callback: VoronoiDiagramCallback) {
        for (y in 0 until m_countY - 1) {
            for (x in 0 until m_countX - 1) {
                val i = x + y * m_countX
                val a = m_diagram!![i]
                val b = m_diagram!![i + 1]
                val c = m_diagram!![i + m_countX]
                val d = m_diagram!![i + 1 + m_countX]
                if (b !== c) {
                    if (a !== b && a !== c) {
                        callback.callback(a!!.tag, b!!.tag, c!!.tag)
                    }
                    if (d !== b && d !== c) {
                        callback.callback(b!!.tag, d!!.tag, c!!.tag)
                    }
                }
            }
        }
    }

    fun addGenerator(center: Vec2, tag: Int) {
        val g = m_generatorBuffer[m_generatorCount++]
        g.center.x = center.x
        g.center.y = center.y
        g.tag = tag
    }

    fun generate(radius: Float) {
        assert(m_diagram == null)
        val inverseRadius = 1 / radius
        lower.x = Float.MAX_VALUE
        lower.y = Float.MAX_VALUE
        upper.x = -Float.MAX_VALUE
        upper.y = -Float.MAX_VALUE
        for (k in 0 until m_generatorCount) {
            val g = m_generatorBuffer[k]
            Vec2.minToOut(lower, g.center, lower)
            Vec2.maxToOut(upper, g.center, upper)
        }
        m_countX = 1 + (inverseRadius * (upper.x - lower.x)).toInt()
        m_countY = 1 + (inverseRadius * (upper.y - lower.y)).toInt()
        m_diagram = arrayOfNulls<Generator>(m_countX * m_countY)
        queue.reset(arrayOfNulls<VoronoiDiagramTask>(4 * m_countX * m_countX) as Array<VoronoiDiagramTask>)
        for (k in 0 until m_generatorCount) {
            val g = m_generatorBuffer[k]
            g.center.x = inverseRadius * (g.center.x - lower.x)
            g.center.y = inverseRadius * (g.center.y - lower.y)
            val x = MathUtils.max(0, MathUtils.min(g.center.x.toInt(), m_countX - 1))
            val y = MathUtils.max(0, MathUtils.min(g.center.y.toInt(), m_countY - 1))
            queue.push(taskPool.pop().set(x, y, x + y * m_countX, g))
        }
        while (!queue.empty()) {
            val front = queue.pop()
            val x = front.m_x
            val y = front.m_y
            val i = front.m_i
            val g = front.m_generator
            if (m_diagram!![i] == null) {
                m_diagram!![i] = g!!
                if (x > 0) {
                    queue.push(taskPool.pop().set(x - 1, y, i - 1, g))
                }
                if (y > 0) {
                    queue.push(taskPool.pop().set(x, y - 1, i - m_countX, g))
                }
                if (x < m_countX - 1) {
                    queue.push(taskPool.pop().set(x + 1, y, i + 1, g))
                }
                if (y < m_countY - 1) {
                    queue.push(taskPool.pop().set(x, y + 1, i + m_countX, g))
                }
            }
            taskPool.push(front)
        }
        val maxIteration = m_countX + m_countY
        for (iteration in 0 until maxIteration) {
            for (y in 0 until m_countY) {
                for (x in 0 until m_countX - 1) {
                    val i = x + y * m_countX
                    val a = m_diagram!![i]!!
                    val b = m_diagram!![i + 1]!!
                    if (a !== b) {
                        queue.push(taskPool.pop().set(x, y, i, b))
                        queue.push(taskPool.pop().set(x + 1, y, i + 1, a))
                    }
                }
            }
            for (y in 0 until m_countY - 1) {
                for (x in 0 until m_countX) {
                    val i = x + y * m_countX
                    val a = m_diagram!![i]!!
                    val b = m_diagram!![i + m_countX]!!
                    if (a !== b) {
                        queue.push(taskPool.pop().set(x, y, i, b))
                        queue.push(taskPool.pop().set(x, y + 1, i + m_countX, a))
                    }
                }
            }
            var updated = false
            while (!queue.empty()) {
                val front = queue.pop()
                val x = front.m_x
                val y = front.m_y
                val i = front.m_i
                val k = front.m_generator
                val a = m_diagram!![i]
                val b = k
                if (a !== b) {
                    val ax = a!!.center.x - x
                    val ay = a!!.center.y - y
                    val bx = b!!.center.x - x
                    val by = b.center.y - y
                    val a2 = ax * ax + ay * ay
                    val b2 = bx * bx + by * by
                    if (a2 > b2) {
                        m_diagram!![i] = b
                        if (x > 0) {
                            queue.push(taskPool.pop().set(x - 1, y, i - 1, b))
                        }
                        if (y > 0) {
                            queue.push(taskPool.pop().set(x, y - 1, i - m_countX, b))
                        }
                        if (x < m_countX - 1) {
                            queue.push(taskPool.pop().set(x + 1, y, i + 1, b))
                        }
                        if (y < m_countY - 1) {
                            queue.push(taskPool.pop().set(x, y + 1, i + m_countX, b))
                        }
                        updated = true
                    }
                }
                taskPool.push(front)
            }
            if (!updated) {
                break
            }
        }
    }
}
