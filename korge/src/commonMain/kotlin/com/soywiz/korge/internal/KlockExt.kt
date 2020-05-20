package com.soywiz.korge.internal

import com.soywiz.klock.*

// @TODO: Remove this and replace with real .milliseconds when Klock is updated
@PublishedApi
internal val Int.ms get() = TimeSpan(this.toDouble())
@PublishedApi
internal val Double.ms get() = TimeSpan(this)
@PublishedApi
internal val Float.ms get() = TimeSpan(this.toDouble())

@PublishedApi
internal val Int.secs get() = TimeSpan(this.toDouble() * 1000.0)
@PublishedApi
internal val Double.secs get() = TimeSpan(this * 1000.0)
@PublishedApi
internal val Float.secs get() = TimeSpan(this.toDouble() * 1000.0)
