package korlibs.render

interface MenuInterfaceProvider {
    val menuInterface: MenuInterface
}

interface MenuInterface : MenuInterfaceProvider {
    override val menuInterface: MenuInterface get() = this

    fun setMainMenu(items: List<MenuItem>): Unit = Unit
    fun showContextMenu(items: List<MenuItem>): Unit = Unit
}

fun MenuInterfaceProvider.setMainMenu(items: List<MenuItem>): Unit = menuInterface.setMainMenu(items)
fun MenuInterfaceProvider.showContextMenu(items: List<MenuItem>): Unit = menuInterface.showContextMenu(items)
fun MenuInterfaceProvider.setMainMenu(block: MenuItem.Builder.() -> Unit) = setMainMenu(MenuItem.Builder().also(block).toItem().children ?: listOf())
fun MenuInterfaceProvider.showContextMenu(block: MenuItem.Builder.() -> Unit) = showContextMenu(MenuItem.Builder().also(block).toItem().children ?: listOf())

data class MenuItem(val text: String?, val enabled: Boolean = true, val children: List<MenuItem>? = null, val action: () -> Unit = {}) {
    companion object {
        val SEPARATOR = MenuItem(null)
    }

    class Builder(private var text: String? = null, private var enabled: Boolean = true, private var action: () -> Unit = {}) {
        @PublishedApi internal val children = arrayListOf<MenuItem>()

        inline fun separator() {
            item(null)
        }

        inline fun item(text: String?, enabled: Boolean = true, noinline action: () -> Unit = {}, block: Builder.() -> Unit = {}): MenuItem {
            val mib = Builder(text, enabled, action)
            block(mib)
            val item = mib.toItem()
            children.add(item)
            return item
        }

        fun toItem() = MenuItem(text, enabled, children.ifEmpty { null }, action)
    }

}
