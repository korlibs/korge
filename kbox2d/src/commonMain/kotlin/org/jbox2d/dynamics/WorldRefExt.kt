package org.jbox2d.dynamics

import org.jbox2d.dynamics.joints.*

inline fun WorldRef.forEachBody(callback: (body: Body) -> Unit) {
    var node = world.bodyList
    while (node != null) {
        callback(node)
        node = node.m_next
    }
}

inline fun WorldRef.forEachJoint(callback: (joint: Joint) -> Unit) {
    var node = world.jointList
    while (node != null) {
        callback(node)
        node = node.m_next
    }
}
