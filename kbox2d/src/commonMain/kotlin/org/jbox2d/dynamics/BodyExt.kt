package org.jbox2d.dynamics

inline fun Body.forEachFixture(callback: (fixture: Fixture) -> Unit) {
    var node = m_fixtureList
    while (node != null) {
        callback(node)
        node = node.m_next
    }
}
