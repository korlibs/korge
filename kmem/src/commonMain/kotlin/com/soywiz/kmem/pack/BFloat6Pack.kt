package com.soywiz.kmem.pack

expect class BFloat6Pack
expect val BFloat6Pack.bf0: Float
expect val BFloat6Pack.bf1: Float
expect val BFloat6Pack.bf2: Float
expect val BFloat6Pack.bf3: Float
expect val BFloat6Pack.bf4: Float
expect val BFloat6Pack.bf5: Float
expect fun bfloat6PackOf(bf0: Float, bf1: Float, bf2: Float, bf3: Float, bf4: Float, bf5: Float): BFloat6Pack
