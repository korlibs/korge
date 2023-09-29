package com.soywiz.kproject.util

import kotlin.math.*

val String.version: Version get() = Version(this)

data class Version(val str: String) : Comparable<Version> {
    override fun equals(other: Any?): Boolean = other is Version && this.compareTo(other) == 0
    override fun compareTo(other: Version): Int = VersionComparer.compare(this.str, other.str)
    override fun toString(): String = str
}

object VersionComparer {
    fun extractNumbers(s: String): List<Int> {
        return MATCH_INTS.findAll(s).toList().map {
            val value = it.groupValues[0].lowercase()
            when (value) {
                "alpha" -> -10
                "beta" -> -5
                "rc" -> -1
                else -> value.toInt()
            }
        }
    }

    fun compare(a: String, b: String): Int {
        val an = extractNumbers(a)
        val bn = extractNumbers(b)
        for (n in 0 until max(an.size, bn.size)) {
            val av = an.getOrElse(n) { 0 }
            val bv = bn.getOrElse(n) { 0 }
            val result = av.compareTo(bv)
            if (result != 0) return result
        }
        //return a.compareTo(b)
        return 0
    }

    private val MATCH_INTS = Regex("(\\d+|alpha|beta|rc)", RegexOption.IGNORE_CASE)
}
