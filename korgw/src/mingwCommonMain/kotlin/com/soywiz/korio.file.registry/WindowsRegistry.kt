package com.soywiz.korio.file.registry

import com.soywiz.kmem.*
import com.soywiz.korio.lang.*
import com.soywiz.krypto.encoding.*
import kotlinx.cinterop.*
import platform.windows.*

actual object WindowsRegistry : WindowsRegistryBase() {
    override val isSupported: Boolean get() = true

    fun Number.toHKEY(): HKEY? = (this.toLong() and 0xFFFFFFFFL).toCPointer()

    private fun parsePathEx(path: String): Pair<HKEY?, String>? =
        parsePath(path)?.let { Pair(it.first.toHKEY(), it.second) }

    private fun parsePathWithValueEx(path: String): Triple<HKEY?, String, String>? =
        parsePathWithValue(path)?.let { Triple(it.first.toHKEY(), it.second, it.third) }

    @PublishedApi
    internal fun checkError(result: Int): Int {
        if (result != ERROR_SUCCESS) {
            memScoped {
                val nameArray = allocArray<WCHARVar>(1024)
                FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM, null, result.convert(), LANG_ENGLISH, nameArray, 1024, null)
                error("Error in Winreg (${result.hex}) [${nameArray.toKString()}]")
            }
        }
        return result
    }

    inline fun <T> openUseKey(
        root: HKEY?,
        keyPath: String,
        extraOpenMode: Int = 0,
        block: MemScope.(key: HKEY?) -> T
    ): T = memScoped {
        val key = alloc<HKEYVar>()
        checkError(RegOpenKeyExW(root, keyPath, 0, (KEY_READ or extraOpenMode).convert(), key.ptr))
        try {
            block(key.value)
        } finally {
            checkError(RegCloseKey(key.value))
        }
    }

    override fun listSubKeys(path: String): List<String> {
        val (root, keyPath) = parsePathEx(path) ?: return KEY_MAP.keys.toList()
        openUseKey(root, keyPath) { key ->
            val lpcSubKeys = alloc<DWORDVar>()
            val lpcMaxSubKeyLen = alloc<DWORDVar>()
            checkError(
                RegQueryInfoKeyW(
                    key, null, null, null, lpcSubKeys.ptr, lpcMaxSubKeyLen.ptr, null, null, null, null, null, null
                )
            )
            //println("lpcSubKeys=${lpcSubKeys.value}, lpcMaxSubKeyLen=${lpcMaxSubKeyLen.value}")

            val names = arrayListOf<String>()

            for (n in 0 until lpcSubKeys.value.toInt()) {
                memScoped {
                    val arrayLength = lpcMaxSubKeyLen.value.toInt() + 1
                    val nameArray = allocArray<WCHARVar>(arrayLength)
                    val lpcchValueName = alloc<DWORDVar>()
                    lpcchValueName.value = arrayLength.convert()
                    checkError(
                        RegEnumKeyExW(
                            key, n.convert(), nameArray, lpcchValueName.ptr,
                            null, null, null, null
                        )
                    )
                    val name = nameArray.toKStringFromUtf16()
                    names += name
                    //println("name=${name}")
                }
            }

            return names
        }
    }

    override fun listValues(path: String): Map<String, Any?> {
        val (root, keyPath) = parsePathEx(path) ?: return emptyMap()
        openUseKey(root, keyPath) { hKey ->
            val lpcValues = alloc<DWORDVar>()
            val lpcMaxValueNameLen = alloc<DWORDVar>()
            val lpcMaxValueLen = alloc<DWORDVar>()
            checkError(RegQueryInfoKeyW(
                hKey, null, null, null, null, null, null,
                lpcValues.ptr, lpcMaxValueNameLen.ptr, lpcMaxValueLen.ptr, null, null
            ))
            val map = LinkedHashMap<String, Any?>()
            val byteDataLength = (lpcMaxValueLen.value.toInt() + 1) * Short.SIZE_BYTES
            val nameLength = lpcMaxValueNameLen.value.toInt() + 1

            for (n in 0 until lpcValues.value.toInt()) {
                memScoped {
                    // Data
                    val byteData = allocArray<UByteVar>(byteDataLength)
                    val lpcbData = alloc<DWORDVar>().also { it.value = lpcMaxValueLen.value }

                    // Name
                    val nameArray = allocArray<WCHARVar>(nameLength)
                    val lpcchValueName = alloc<DWORDVar>().also { it.value = nameLength.convert() }

                    // Type
                    val lpType = alloc<DWORDVar>()

                    checkError(RegEnumValueW(
                        hKey, n.convert(), nameArray, lpcchValueName.ptr,
                        null, lpType.ptr, byteData, lpcbData.ptr
                    ))

                    val keyName = nameArray.toKString()
                    val dataSize = lpcbData.value.toInt()
                    val keyType = lpType.value.toInt()

                    map[keyName] = bytesToValue(byteData, dataSize, keyType)
                }
            }
            return map
        }
    }

    fun bytesToValue(byteData: CPointer<UByteVar>, dataSize: Int, keyType: Int): Any? {
        return when (keyType) {
            REG_NONE -> null
            REG_BINARY -> byteData.readBytes(dataSize)
            REG_QWORD -> if (dataSize == 0) 0L else byteData.reinterpret<LongVar>()[0]
            REG_DWORD -> if (dataSize == 0) 0L else byteData.reinterpret<IntVar>()[0]
            REG_SZ, REG_EXPAND_SZ -> byteData.reinterpret<WCHARVar>().toKString()
            REG_MULTI_SZ -> TODO()
            else -> error("Unsupported reg type $keyType")
        }
    }

    override fun hasKey(path: String): Boolean {
        val (root, keyPath) = parsePathEx(path) ?: return false
        memScoped {
            val key = alloc<HKEYVar>()
            val result = RegOpenKeyExW(root, keyPath, 0, KEY_READ.convert(), key.ptr)
            when (result) {
                ERROR_SUCCESS -> return true.also { RegCloseKey(key.value) }
                ERROR_FILE_NOT_FOUND -> return false
                else -> checkError(result)
            }
        }
        return super.hasKey(path)
    }

    override fun createKey(path: String): Boolean {
        val (root, keyPath) = parsePathEx(path) ?: return false
        memScoped {
            val phkResult = alloc<HKEYVar>()
            val lpdwDisposition = alloc<DWORDVar>()
            checkError(RegCreateKeyExW(
                root, keyPath, 0, null, REG_OPTION_NON_VOLATILE, KEY_READ,
                null, phkResult.ptr, lpdwDisposition.ptr
            ))
            checkError(RegCloseKey(phkResult.value))
            return lpdwDisposition.value.toInt() == REG_CREATED_NEW_KEY.toInt()
        }
    }

    override fun deleteKey(path: String) {
        val (root, keyPath) = parsePathEx(path) ?: return
        checkError(RegDeleteKeyW(root, keyPath))
    }

    override fun deleteValue(path: String) {
        val (root, keyPath, valueName) = parsePathWithValueEx(path) ?: return
        openUseKey(root, keyPath) { hKey ->
            checkError(RegDeleteValueW(hKey, valueName))
        }
    }

    override fun setValue(path: String, value: Any?) {
        val (root, keyPath, valueName) = parsePathWithValueEx(path) ?: return
        openUseKey(root, keyPath, KEY_WRITE) { hKey ->
            val (bytes, kind) = when (value) {
                null -> byteArrayOf() to REG_NONE
                is String -> "$value\u0000".toByteArray(UTF16_LE) to REG_SZ
                is ByteArray -> value to REG_BINARY
                is Int -> ByteArray(4).also { it.write32LE(0, value) } to REG_DWORD
                is Long -> ByteArray(8).also { it.write64LE(0, value) } to REG_QWORD
                is List<*> -> value.joinToString("") { "$it\u0000" }.toByteArray(UTF16_LE) to REG_MULTI_SZ
                else -> TODO("Unimplemented setValue for type ${value::class}")
            }
            bytes.usePinned {
                RegSetValueExW(hKey, valueName, 0, kind.convert(), it.startAddressOf.reinterpret(), it.get().size.convert())
            }
        }
    }

    override fun getValue(path: String): Any? {
        val (root, keyPath, valueName) = parsePathWithValueEx(path) ?: return null
        memScoped {
            val lpType = alloc<DWORDVar>()
            val lpcbData = alloc<DWORDVar>()

            val rc = RegGetValueW(root, keyPath, valueName, RRF_RT_ANY, lpType.ptr, null, lpcbData.ptr)
            val keyType = lpType.value.toInt()
            if (keyType == REG_NONE) return null
            if (rc != ERROR_SUCCESS && rc != ERROR_INSUFFICIENT_BUFFER) checkError(rc)

            val byteSize = lpcbData.value.toInt()
            val byteData = allocArray<UByteVar>(byteSize + Short.SIZE_BYTES)

            checkError(RegGetValueW(root, keyPath, valueName, RRF_RT_ANY, lpType.ptr, byteData, lpcbData.ptr))

            return bytesToValue(byteData, byteSize, keyType)
        }
    }
}
