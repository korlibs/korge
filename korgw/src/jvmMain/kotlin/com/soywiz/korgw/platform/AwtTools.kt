package com.soywiz.korgw.platform

import java.awt.*
import java.lang.reflect.*

inline class ReflectiveAccessor(val instance: Any?) {
    val clazz get() = if (instance != null) instance::class.java else null
    fun dynamicInvoke(methodName: String, vararg args: Any?): Any? {
        if (instance == null) return ReflectiveAccessor(null)
        val method = clazz?.getMethodOrNullNoArgs(methodName) ?: return null
        if (!method.isAccessible) {
            method.isAccessible = true;
        }
        return method?.invoke(instance, *args)
    }
}

fun Any?.reflective() = ReflectiveAccessor(this)

private fun <T> Class<T>.getMethodOrNullNoArgs(name: String): Method? =
    runCatching { declaredMethods?.first { it.name == name } }.getOrNull()
        ?: superclass?.getMethodOrNullNoArgs(name)

private fun <T> Class<T>.getMethodOrNull(name: String, vararg args: Class<*>): Method? =
    runCatching { getDeclaredMethod(name, *args) }.getOrNull()
        ?: superclass?.getMethodOrNull(name, *args)

private fun <T> Class<T>.getFieldOrNull(name: String): Field? =
    runCatching { getDeclaredField(name) }.getOrNull()
        ?: superclass?.getFieldOrNull(name)

fun Component.awtGetPeer(): Any {
    //Class.forName("AWTAccessor")
    return sun.awt.AWTAccessor.getComponentAccessor()?.getPeer(this) ?: Unit
/*
    val method = this.javaClass.getMethodOrNull("getPeer")
    if (method != null) {
        method.isAccessible = true
        return method.invoke(this)
    }
    val field = this.javaClass.getFieldOrNull("peer")
    if (field != null) {
        field.isAccessible = true
        return field.get(this)
    }
    error("Can't get peer from Frame")
 */
}

fun Component.awtNativeHandle(): Long {
    val peer = this.awtGetPeer()
    val hwnd = peer.javaClass.getFieldOrNull("hwnd")?.get(peer)
    if (hwnd != null) return hwnd as Long

    return (peer.javaClass.getFieldOrNull("ptr")?.get(peer) as? Long?)
        ?: (peer.javaClass.getMethodOrNull("getPointer")?.invoke(peer) as Long?)
        ?: (peer.javaClass.getMethodOrNull("getLayerPtr")?.invoke(peer) as Long?)
        ?: error("Can't get native handle from peer")
}
