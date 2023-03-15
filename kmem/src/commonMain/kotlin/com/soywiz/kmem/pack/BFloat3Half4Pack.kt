package com.soywiz.kmem.pack

expect class BFloat3Half4Pack
// 21-bit BFloat precision
expect val BFloat3Half4Pack.b0: Float
expect val BFloat3Half4Pack.b1: Float
expect val BFloat3Half4Pack.b2: Float
// 16-bit Half Float precision
expect val BFloat3Half4Pack.hf0: Float
expect val BFloat3Half4Pack.hf1: Float
expect val BFloat3Half4Pack.hf2: Float
expect val BFloat3Half4Pack.hf3: Float
expect fun bfloat3Half4PackOf(
    b0: Float, b1: Float, b2: Float,
    hf0: Float, hf1: Float, hf2: Float, hf3: Float
): BFloat3Half4Pack

