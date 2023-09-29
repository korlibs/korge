package com.soywiz.kproject.util

import java.security.*

class Hash(val content: ByteArray) {
    val str: String = content.hex()
    override fun toString(): String = str
}

fun ByteArray.hash(algo: String): Hash = Hash(MessageDigest.getInstance(algo).digest(this))
fun ByteArray.sha256(): Hash = hash("SHA-256")
