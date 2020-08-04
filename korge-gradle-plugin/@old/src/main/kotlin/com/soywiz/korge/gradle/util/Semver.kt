package com.soywiz.korge.gradle.util

import java.util.*

data class SemVer(val version: String) : Comparable<SemVer> {
	override fun compareTo(other: SemVer): Int = Scanner(this.version).use { s1 ->
		Scanner(other.version).use { s2 ->
			s1.useDelimiter("\\.")
			s2.useDelimiter("\\.")

			while (s1.hasNextInt() && s2.hasNextInt()) {
				val v1 = s1.nextInt()
				val v2 = s2.nextInt()
				if (v1 < v2) {
					return -1
				} else if (v1 > v2) {
					return 1
				}
			}

			if (s1.hasNextInt() && s1.nextInt() != 0) return 1
			return if (s2.hasNextInt() && s2.nextInt() != 0) -1 else 0
		}
	}

}
