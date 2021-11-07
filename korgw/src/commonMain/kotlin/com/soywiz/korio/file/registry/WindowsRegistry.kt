package com.soywiz.korio.file.registry

expect object WindowsRegistry : WindowsRegistryBase

open class WindowsRegistryBase {
    companion object {
        const val HKEY_CLASSES_ROOT = (0x80000000L).toInt()
        const val HKEY_CURRENT_USER = (0x80000001L).toInt()
        const val HKEY_LOCAL_MACHINE = (0x80000002L).toInt()
        const val HKEY_USERS = (0x80000003L).toInt()
        const val HKEY_PERFORMANCE_DATA= (0x80000004L).toInt()
        const val HKEY_PERFORMANCE_TEXT= (0x80000050L).toInt()
        const val HKEY_PERFORMANCE_NLSTEXT = (0x80000060L).toInt()
        const val HKEY_CURRENT_CONFIG  = (0x80000005L).toInt()
        const val HKEY_DYN_DATA = (0x80000006L).toInt()
        const val HKEY_CURRENT_USER_LOCAL_SETTINGS = (0x80000007L).toInt()
        val KEY_MAP: Map<String, Int> = mapOf<String, Int>(
            "HKEY_CLASSES_ROOT" to HKEY_CLASSES_ROOT,
            "HKEY_CURRENT_USER" to HKEY_CURRENT_USER,
            "HKEY_LOCAL_MACHINE" to HKEY_LOCAL_MACHINE,
            "HKEY_USERS" to HKEY_USERS,
            "HKEY_CURRENT_CONFIG" to HKEY_CURRENT_CONFIG,
            "HKEY_PERFORMANCE_DATA" to HKEY_PERFORMANCE_DATA,
            "HKEY_PERFORMANCE_TEXT" to HKEY_PERFORMANCE_TEXT,
            "HKEY_PERFORMANCE_NLSTEXT" to HKEY_PERFORMANCE_NLSTEXT,
            "HKEY_DYN_DATA" to HKEY_DYN_DATA,
            "HKEY_CURRENT_USER_LOCAL_SETTINGS" to HKEY_CURRENT_USER_LOCAL_SETTINGS,
        )
    }

    protected fun parsePath(path: String): Pair<Int, String>? {
        val rpath = normalizePath(path)
        if (rpath.isEmpty()) return null
        val rootKeyStr = rpath.substringBefore('\\')
        val keyPath = rpath.substringAfter('\\', "")
        val rootKey = KEY_MAP[rootKeyStr.uppercase()] ?: error("Invalid rootKey '${rootKeyStr}', it should start with HKEY_ and be known")
        return rootKey to keyPath
    }

    protected fun parsePathWithValue(path: String): Triple<Int, String, String>? {
        val (root, keyPath) = parsePath(path) ?: return null
        val keyPathPath = keyPath.substringBeforeLast('\\')
        val valueName = keyPath.substringAfterLast('\\', "")
        return Triple(root, keyPathPath, valueName)
    }

    protected fun normalizePath(path: String) = path.trim('/').replace('/', '\\')

    open val isSupported: Boolean get() = false
    open fun listSubKeys(path: String): List<String> = TODO()
    open fun listValues(path: String): Map<String, Any?> = TODO()
    open fun getValue(path: String): Any? = TODO()
    open fun setValue(path: String, value: Any?): Unit = TODO()
    open fun deleteValue(path: String): Unit = TODO()
    open fun createKey(path: String): Boolean = TODO()
    open fun deleteKey(path: String): Unit = TODO()
    open fun hasKey(path: String): Boolean = TODO()

    fun hasValue(path: String): Boolean = getValue(path) != null
    fun listValueKeys(path: String): List<String> = listValues(path).keys.toList()
}

