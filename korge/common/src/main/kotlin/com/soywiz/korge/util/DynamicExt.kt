package com.soywiz.korge.util

import com.soywiz.korio.lang.Dynamic

fun Dynamic.toBool2(obj: Any?): Boolean = when (obj) {
	is Boolean -> obj
	is String -> when (obj.toLowerCase()) {
	//"1", "true", "ok", "yes" -> true
		"", "0", "false", "ko", "no" -> false
	//else -> false
		else -> true
	}
	else -> Dynamic.toInt(obj) != 0
}
