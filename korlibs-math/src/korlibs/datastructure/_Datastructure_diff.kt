@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.diff

import korlibs.datastructure.*

class Diff<T>(val kept: List<T>, val removed: List<T>, val added: List<T>) {
    companion object {
        fun <T> compare(old: Set<T>, new: Set<T>): Diff<T> {
            val kept = fastArrayListOf<T>()
            val removed = fastArrayListOf<T>()
            val added = fastArrayListOf<T>()
            for (v in old) {
                if (v in new) {
                    kept.add(v)
                } else {
                    removed.add(v)
                }
            }
            for (v in new) {
                if (v !in old) {
                    added.add(v)
                }
            }
            return Diff(kept, removed, added)
        }
    }
}
