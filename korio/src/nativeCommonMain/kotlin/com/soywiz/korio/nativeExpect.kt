package com.soywiz.korio

expect fun nativeCwd(): String
expect fun doMkdir(path: String, attr: Int): Int
expect val TARGET_INFO: String
