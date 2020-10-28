package com.soywiz.korio.hash

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.internal.*
import com.soywiz.korio.stream.*
import com.soywiz.krypto.*

suspend fun AsyncInputOpenable.hash(algo: HasherFactory) = openRead().use { hash(algo) }
suspend fun AsyncInputStream.hash(algo: HasherFactory): Hash = bytesTempPool.alloc { temp -> algo.digest(temp) { read(it) } }
fun SyncInputStream.hash(algo: HasherFactory): Hash = bytesTempPool.alloc { temp -> algo.digest(temp) { read(it) } }

suspend fun AsyncInputOpenable.md5() = hash(MD5)
suspend fun AsyncInputStream.md5() = hash(MD5)
fun SyncInputStream.md5() = hash(MD5)

suspend fun AsyncInputOpenable.sha1() = hash(SHA1)
suspend fun AsyncInputStream.sha1() = hash(SHA1)
fun SyncInputStream.sha1() = hash(SHA1)
