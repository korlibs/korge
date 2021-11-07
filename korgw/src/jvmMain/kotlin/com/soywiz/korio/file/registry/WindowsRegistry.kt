package com.soywiz.korio.file.registry

import com.soywiz.korio.util.*
import com.sun.jna.platform.win32.*

actual object WindowsRegistry : WindowsRegistryBase() {
    override val isSupported: Boolean get() = OS.isWindows

    private fun parsePathEx(path: String): Pair<WinReg.HKEY, String>? =
        parsePath(path)?.let { Pair(WinReg.HKEY(it.first), it.second) }

    private fun parsePathWithValueEx(path: String): Triple<WinReg.HKEY, String, String>? =
        parsePathWithValue(path)?.let { Triple(WinReg.HKEY(it.first), it.second, it.third) }

    override fun listSubKeys(path: String): List<String> {
        val (root, keyPath) = parsePathEx(path) ?: return KEY_MAP.keys.toList()

        val phkKey = WinReg.HKEYByReference()
        val samDesiredExtra = 0

        var rc = Advapi32.INSTANCE.RegOpenKeyEx(
            root, keyPath, 0,
            WinNT.KEY_READ or samDesiredExtra, phkKey
        )
        if (rc != W32Errors.ERROR_SUCCESS) {
            throw Win32Exception(rc)
        }
        return try {
            Advapi32Util.registryGetKeys(phkKey.value).toList()
        } finally {
            rc = Advapi32.INSTANCE.RegCloseKey(phkKey.value)
            if (rc != W32Errors.ERROR_SUCCESS) {
                throw Win32Exception(rc)
            }
        }
    }

    override fun listValues(path: String): Map<String, Any?> {
        val (root, keyPath) = parsePathEx(path) ?: return emptyMap()
        return Advapi32Util.registryGetValues(root, keyPath)
    }

    override fun getValue(path: String): Any? {
        val (root, keyPathPath, valueName) = parsePathWithValueEx(path) ?: return null
        //Advapi32Util.registryValueExists()
        return Advapi32Util.registryGetValue(root, keyPathPath, valueName).also { result ->
            //println("root=$root, keyPathPath=$keyPathPath, valueName=$valueName, result=$result")
        }
    }

    override fun setValue(path: String, value: Any?) {
        val (root, keyName, valueName) = parsePathWithValueEx(path) ?: return
        when (value) {
            null -> {
                // @TODO: This should set to none
                Advapi32Util.registrySetStringValue(root, keyName, valueName, null)
            }
            is String -> Advapi32Util.registrySetStringValue(root, keyName, valueName, value)
            is ByteArray -> Advapi32Util.registrySetBinaryValue(root, keyName, valueName, value)
            is Int -> Advapi32Util.registrySetIntValue(root, keyName, valueName, value)
            is Long -> Advapi32Util.registrySetLongValue(root, keyName, valueName, value)
            is List<*> -> Advapi32Util.registrySetStringArray(root, keyName, valueName, value.map { it?.toString() }.toTypedArray())
        }
    }

    override fun deleteValue(path: String) {
        val (root, keyName, valueName) = parsePathWithValueEx(path) ?: return
        Advapi32Util.registryDeleteValue(root, keyName, valueName)
    }

    override fun deleteKey(path: String) {
        val (root, keyName) = parsePathEx(path) ?: return
        Advapi32Util.registryDeleteKey(root, keyName)
    }

    override fun createKey(path: String): Boolean {
        val (root, keyName) = parsePathEx(path) ?: return false
        return Advapi32Util.registryCreateKey(root, keyName)
    }

    override fun hasKey(path: String): Boolean {
        val (root, keyName) = parsePathEx(path) ?: return false
        return Advapi32Util.registryKeyExists(root, keyName)
    }
}
