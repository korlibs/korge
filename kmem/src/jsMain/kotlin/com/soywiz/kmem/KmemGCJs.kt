package com.soywiz.kmem

actual val KmemGC: KmemGCImpl = object : KmemGCImpl() {
}
