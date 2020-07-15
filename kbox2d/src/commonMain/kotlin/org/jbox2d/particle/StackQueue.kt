package org.jbox2d.particle

import org.jbox2d.internal.*


class StackQueue<T> {

    private var m_buffer: Array<T>? = null
    private var m_front: Int = 0
    private var m_back: Int = 0
    private var m_end: Int = 0

    fun reset(buffer: Array<T>) {
        m_buffer = buffer
        m_front = 0
        m_back = 0
        m_end = buffer.size
    }

    fun push(task: T) {
        if (m_back >= m_end) {
            arraycopy(m_buffer!!, m_front, m_buffer!!, 0, m_back - m_front)
            m_back -= m_front
            m_front = 0
            if (m_back >= m_end) {
                return
            }
        }
        m_buffer!![m_back++] = task
    }

    fun pop(): T {
        assert(m_front < m_back)
        return m_buffer!![m_front++]
    }

    fun empty(): Boolean {
        return m_front >= m_back
    }

    fun front(): T {
        return m_buffer!![m_front]
    }
}
