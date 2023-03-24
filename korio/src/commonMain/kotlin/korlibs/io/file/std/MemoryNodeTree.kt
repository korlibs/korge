package korlibs.io.file.std

import korlibs.datastructure.iterators.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.stream.*

class MemoryNodeTree(val caseSensitive: Boolean = true) {
    val rootNode = Node("", isDirectory = true)

    open inner class Node(
        val name: String,
        val isDirectory: Boolean = false,
        parent: Node? = null
    ) : Iterable<Node> {
        val nameLC = name.toLowerCase()
        override fun iterator(): Iterator<Node> = children.values.iterator()

        var parent: Node? = null
            set(value) {
                if (field != null) {
                    field!!.children.remove(this.name)
                    field!!.childrenLC.remove(this.nameLC)
                }
                field = value
                field?.children?.set(name, this)
                field?.childrenLC?.set(nameLC, this)
            }

        init {
            this.parent = parent
        }

        var bytes: ByteArray? = null
        var data: Any? = null
        val children = linkedMapOf<String, Node>()
        val childrenLC = linkedMapOf<String, Node>()
        val root: Node get() = parent?.root ?: this
        var stream: AsyncStream? = null
        var link: String? = null

        val path: String get() = if (parent == null) "/" else "/" + "${parent?.path ?: ""}/$name".trimStart('/')

        fun child(name: String): Node? = when (name) {
            "", "." -> this
            ".." -> parent
            else -> if (caseSensitive) {
                children[name]
            } else {
                childrenLC[name.lowercase()]
            }
        }

        fun createChild(name: String, isDirectory: Boolean = false): Node =
            Node(name, isDirectory = isDirectory, parent = this)

        operator fun get(path: String): Node = access(path, createFolders = false)
        fun getOrNull(path: String): Node? = try {
            access(path, createFolders = false)
        } catch (e: FileNotFoundException) {
            null
        }

        fun accessOrNull(path: String): Node? = getOrNull(path)
        fun access(path: String, createFolders: Boolean = false): Node {
            var node = if (path.startsWith('/')) root else this
            path.pathInfo.parts().fastForEach { part ->
                var child = node.child(part)
                if (child == null && createFolders) child = node.createChild(part, isDirectory = true)
                node = child ?: throw FileNotFoundException("Can't find '$part' in $path")
            }
            return node
        }
        fun followLinks(): Node = link?.let { accessOrNull(it)?.followLinks() } ?: this

        fun mkdir(name: String): Boolean {
            if (child(name) != null) {
                return false
            } else {
                createChild(name, isDirectory = true)
                return true
            }
        }

        fun delete() {
            parent?.children?.remove(this.name)
            parent?.childrenLC?.remove(this.nameLC)
        }
    }

}
