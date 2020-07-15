package com.soywiz.korio.util

import java.net.*

fun URI.portWithDefault(default: Int) = if (this.port < 0) default else this.port