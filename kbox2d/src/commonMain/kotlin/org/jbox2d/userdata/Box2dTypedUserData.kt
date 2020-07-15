package org.jbox2d.userdata

interface Box2dTypedUserData {
    @kotlin.Suppress("unused")
    open class Key<T>

    operator fun <T> contains(key: Key<T>): Boolean
    operator fun <T> get(key: Key<T>): T?
    operator fun <T : Any> set(key: Key<T>, value: T?)

    class Mixin : Box2dTypedUserData {
        private var typedUserData: LinkedHashMap<Key<*>, Any>? = null
        override operator fun <T> contains(key: Key<T>): Boolean = typedUserData?.containsKey(key) == true
        override operator fun <T> get(key: Key<T>): T? = typedUserData?.get(key) as T?
        override operator fun <T : Any> set(key: Key<T>, value: T?) {
            if (value != null) {
                if (typedUserData == null) typedUserData = LinkedHashMap()
                typedUserData?.set(key, value)
            } else {
                typedUserData?.remove(key)
            }
        }
    }
}
