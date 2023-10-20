package korlibs.io.file.registry

import korlibs.annotations.*
import korlibs.ffi.*
import korlibs.io.lang.*
import korlibs.platform.*

private typealias HKEY = Int

object WindowsRegistry {
    @KeepNames
    object Advapi32 : FFILib("Advapi32") {
        val RegCloseKey: (hKey: HKEY) -> Int by func(config = FFIFuncConfig.WIDE_STRING)
        val RegDeleteKeyW: (hKey: HKEY, lpSubKey: String?) -> Int by func(config = FFIFuncConfig.WIDE_STRING)
        val RegDeleteValueW: (hKey: HKEY, lpValueName: String?) -> Int by func(config = FFIFuncConfig.WIDE_STRING)
        val RegSetValueExW: (hKey: HKEY, lpValueName: String, reserved: Int, dwType: Int, lpData: FFIPointer?, cbData: Int) -> Int by func(config = FFIFuncConfig.WIDE_STRING)
        val RegOpenKeyExW: (
            hKey: HKEY,
            lpSubKey: String,
            ulOptions: Int,
            samDesired: Int,
            phkResult: FFIPointer?,
        ) -> Int by func(config = FFIFuncConfig.WIDE_STRING)
        val RegCreateKeyExW: (
            hKey: HKEY,
            lpSubKey: String,
            reserved: Int,
            lpClass: String?,
            dwOptions: Int,
            samDesired: Int,
            lpSecurityAttributes: FFIPointer?,
            phkResult: FFIPointer?,
            lpdwDisposition: FFIPointer?,
        ) -> Int by func(config = FFIFuncConfig.WIDE_STRING)
        val RegGetValueW: (
            hKey: HKEY,
            lpSubKey: String,
            lpValue: String,
            dwFlags: Int,
            pdwType: FFIPointer?,
            pvData: FFIPointer?,
            pcbData: FFIPointer?,
        ) -> Int by func(config = FFIFuncConfig.WIDE_STRING)
        val RegEnumValueW: (
            hKey: HKEY,
            dwIndex: Int,
            lpValueName: FFIPointer?,
            lpcchValueName: FFIPointer?,
            lpReserved: FFIPointer?,
            lpType: FFIPointer?,
            lpData: FFIPointer?,
            lpcbData: FFIPointer?,
        ) -> Int by func(config = FFIFuncConfig.WIDE_STRING)
        val RegQueryInfoKeyW: (
            hKey: HKEY,
            lpClass: String?,
            lpcchClass: FFIPointer?,
            lpReserved: FFIPointer?,
            lpcSubKeys: FFIPointer?,
            lpcbMaxSubKeyLen: FFIPointer?,
            lpcbMaxClassLen: FFIPointer?,
            lpcValues: FFIPointer?,
            lpcbMaxValueNameLen: FFIPointer?,
            lpcbMaxValueLen: FFIPointer?,
            lpcbSecurityDescriptor: FFIPointer?,
            lpftLastWriteTime: FFIPointer?
        ) -> Int by func(config = FFIFuncConfig.WIDE_STRING)
        val RegEnumKeyExW: (
            hKey: HKEY,
            dwIndex: Int,
            lpName: FFIPointer?,
            lpcchName: FFIPointer?,
            lpReserved: FFIPointer?,
            lpClass: FFIPointer?,
            lpcchClass: FFIPointer?,
            lpftLastWriteTime: FFIPointer?,
        ) -> Int by func(config = FFIFuncConfig.WIDE_STRING)
    }

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

    private const val KEY_QUERY_VALUE = 0x0001
    private const val KEY_SET_VALUE = 0x0002
    private const val KEY_CREATE_SUB_KEY = 0x0004
    private const val KEY_ENUMERATE_SUB_KEYS = 0x0008
    private const val KEY_NOTIFY = 0x0010
    private const val KEY_CREATE_LINK = 0x0020
    private const val KEY_WOW64_32KEY = 0x0200
    private const val KEY_WOW64_64KEY = 0x0100
    private const val KEY_WOW64_RES = 0x0300

    private const val DELETE = 0x00010000
    private const val READ_CONTROL = 0x00020000
    private const val WRITE_DAC = 0x00040000
    private const val WRITE_OWNER = 0x00080000
    private const val SYNCHRONIZE = 0x00100000

    private const val STANDARD_RIGHTS_REQUIRED = 0x000F0000
    private const val STANDARD_RIGHTS_READ: Int = READ_CONTROL
    private const val STANDARD_RIGHTS_WRITE: Int = READ_CONTROL
    private const val STANDARD_RIGHTS_EXECUTE: Int = READ_CONTROL
    private const val STANDARD_RIGHTS_ALL = 0x001F0000
    private const val SPECIFIC_RIGHTS_ALL = 0x0000FFFF
    private const val KEY_READ: Int = (STANDARD_RIGHTS_READ or KEY_QUERY_VALUE
        or KEY_ENUMERATE_SUB_KEYS or (KEY_NOTIFY and SYNCHRONIZE.inv()))
    private const val KEY_WRITE: Int = STANDARD_RIGHTS_WRITE or KEY_SET_VALUE or (KEY_CREATE_SUB_KEY and SYNCHRONIZE.inv())
    private const val KEY_EXECUTE = KEY_READ and SYNCHRONIZE.inv()
    private const val KEY_ALL_ACCESS: Int = (STANDARD_RIGHTS_ALL or KEY_QUERY_VALUE or KEY_SET_VALUE
        or KEY_CREATE_SUB_KEY or KEY_ENUMERATE_SUB_KEYS or KEY_NOTIFY
        or KEY_CREATE_LINK) and SYNCHRONIZE.inv()
    private const val REG_OPTION_RESERVED = 0x00000000
    private const val REG_OPTION_NON_VOLATILE = 0x00000000
    private const val REG_OPTION_VOLATILE = 0x00000001
    private const val REG_OPTION_CREATE_LINK = 0x00000002
    private const val REG_OPTION_BACKUP_RESTORE = 0x00000004
    private const val REG_OPTION_OPEN_LINK = 0x00000008
    private const val REG_LEGAL_OPTION = (REG_OPTION_RESERVED or REG_OPTION_NON_VOLATILE
        or REG_OPTION_VOLATILE or REG_OPTION_CREATE_LINK
        or REG_OPTION_BACKUP_RESTORE or REG_OPTION_OPEN_LINK)
    private const val REG_CREATED_NEW_KEY = 0x00000001
    private const val REG_OPENED_EXISTING_KEY = 0x00000002
    private const val REG_STANDARD_FORMAT = 1
    private const val REG_LATEST_FORMAT = 2
    private const val REG_NO_COMPRESSION = 4
    private const val REG_WHOLE_HIVE_VOLATILE = 0x00000001
    private const val REG_REFRESH_HIVE = 0x00000002
    private const val REG_NO_LAZY_FLUSH = 0x00000004
    private const val REG_FORCE_RESTORE = 0x00000008
    private const val REG_APP_HIVE = 0x00000010
    private const val REG_PROCESS_PRIVATE = 0x00000020
    private const val REG_START_JOURNAL = 0x00000040
    private const val REG_HIVE_EXACT_FILE_GROWTH = 0x00000080
    private const val REG_HIVE_NO_RM = 0x00000100
    private const val REG_HIVE_SINGLE_LOG = 0x00000200
    private const val REG_FORCE_UNLOAD = 1
    private const val REG_NOTIFY_CHANGE_NAME = 0x00000001
    private const val REG_NOTIFY_CHANGE_ATTRIBUTES = 0x00000002
    private const val REG_NOTIFY_CHANGE_LAST_SET = 0x00000004
    private const val REG_NOTIFY_CHANGE_SECURITY = 0x00000008
    private const val REG_NOTIFY_THREAD_AGNOSTIC = 0x10000000

    private const val REG_LEGAL_CHANGE_FILTER = (REG_NOTIFY_CHANGE_NAME
        or REG_NOTIFY_CHANGE_ATTRIBUTES or REG_NOTIFY_CHANGE_LAST_SET
        or REG_NOTIFY_CHANGE_SECURITY or REG_NOTIFY_THREAD_AGNOSTIC)
    private const val REG_NONE = 0
    private const val REG_SZ = 1
    private const val REG_EXPAND_SZ = 2
    private const val REG_BINARY = 3
    private const val REG_DWORD = 4
    private const val REG_DWORD_LITTLE_ENDIAN = 4
    private const val REG_DWORD_BIG_ENDIAN = 5
    private const val REG_LINK = 6
    private const val REG_MULTI_SZ = 7
    private const val REG_RESOURCE_LIST = 8
    private const val REG_FULL_RESOURCE_DESCRIPTOR = 9
    private const val REG_RESOURCE_REQUIREMENTS_LIST = 10
    private const val REG_QWORD = 11
    private const val REG_QWORD_LITTLE_ENDIAN = 11

    private const val ERROR_SUCCESS = 0
    private const val ERROR_INVALID_FUNCTION = 1 // dderror
    private const val ERROR_FILE_NOT_FOUND = 2
    private const val ERROR_PATH_NOT_FOUND = 3
    private const val ERROR_TOO_MANY_OPEN_FILES = 4
    private const val ERROR_ACCESS_DENIED = 5
    private const val ERROR_INVALID_HANDLE = 6
    private const val ERROR_INSUFFICIENT_BUFFER = 122
    private const val ERROR_MORE_DATA = 234 // dderror

    private const val RRF_RT_ANY = 0x0000ffff
    private const val RRF_RT_DWORD = 0x00000018
    private const val RRF_RT_QWORD = 0x00000048
    private const val RRF_RT_REG_BINARY = 0x00000008
    private const val RRF_RT_REG_DWORD = 0x00000010
    private const val RRF_RT_REG_EXPAND_SZ = 0x00000004
    private const val RRF_RT_REG_MULTI_SZ = 0x00000020
    private const val RRF_RT_REG_NONE = 0x00000001
    private const val RRF_RT_REG_QWORD = 0x00000040
    private const val RRF_RT_REG_SZ = 0x00000002
    private const val REG_PROCESS_APPKEY = 0x00000001

    private fun parsePath(path: String): Pair<Int, String>? {
        val rpath = normalizePath(path)
        if (rpath.isEmpty()) return null
        val rootKeyStr = rpath.substringBefore('\\')
        val keyPath = rpath.substringAfter('\\', "")
        val rootKey = KEY_MAP[rootKeyStr.uppercase()] ?: error("Invalid rootKey '${rootKeyStr}', it should start with HKEY_ and be known")
        return rootKey to keyPath
    }

    private fun parsePathWithValue(path: String): Triple<Int, String, String>? {
        val (root, keyPath) = parsePath(path) ?: return null
        val keyPathPath = keyPath.substringBeforeLast('\\')
        val valueName = keyPath.substringAfterLast('\\', "")
        return Triple(root, keyPathPath, valueName)
    }

    private fun normalizePath(path: String) = path.trim('/').replace('/', '\\')

    val isSupported: Boolean get() = Platform.isWindows && FFILib.isFFISupported

    private fun parsePathEx(path: String): Pair<HKEY, String>? =
        parsePath(path)?.let { Pair(it.first, it.second) }

    private fun parsePathWithValueEx(path: String): Triple<HKEY, String, String>? =
        parsePathWithValue(path)?.let { Triple(it.first, it.second, it.third) }

    class RegistryException(val errorCode: Int) : Exception("Win32 error $errorCode")

    private fun listSubKeys(hKey: HKEY): List<String> = ffiScoped {
        val lpcSubKeys = allocBytes(4)
        val lpcMaxSubKeyLen = allocBytes(4)
        checkSuccess(Advapi32.RegQueryInfoKeyW(
            hKey, null, null, null, lpcSubKeys,
            lpcMaxSubKeyLen, null, null, null, null, null, null
        ))
        val numSubkeys = lpcSubKeys.getS32()
        val maxSubKey = lpcMaxSubKeyLen.getS32()
        val keys = ArrayList<String>(numSubkeys)
        val name = allocBytes((maxSubKey + 1) * 2)
        val lpcchValueName = allocBytes(4)
        for (i in 0 until numSubkeys) {
            lpcchValueName.set32(maxSubKey + 1)
            checkSuccess(Advapi32.RegEnumKeyExW(
                hKey, i, name, lpcchValueName,
                null, null, null, null
            ))
            keys.add(name.getWideStringz())
        }
        return keys
    }

    private fun listValues(hKey: HKEY): Map<String, Any?> = ffiScoped {
        val lpcValues = allocBytes(4)
        val lpcMaxValueNameLen = allocBytes(4)
        val lpcMaxValueLen = allocBytes(4)
        checkSuccess(Advapi32.RegQueryInfoKeyW(
            hKey, null, null, null,
            null, null, null, lpcValues, lpcMaxValueNameLen,
            lpcMaxValueLen, null, null
        ))
        val nvalues = lpcValues.getS32()
        val maxValueNameLen = lpcMaxValueNameLen.getS32()
        val maxValueLen = lpcMaxValueLen.getS32()
        val namePtr = allocBytes((maxValueNameLen + 1) * 2)
        val lpcchValueName = allocBytes(4)
        val valuePtr = allocBytes((maxValueLen + 1) * 2)
        val lpcbData = allocBytes(4)
        val lpType = allocBytes(4)

        val out = LinkedHashMap<String, Any?>()
        //println("nvalues=$nvalues")
        for (n in 0 until nvalues) {
            lpcchValueName.set32(maxValueNameLen + 1)
            lpcbData.set32(maxValueLen + 1)
            checkSuccess(Advapi32.RegEnumValueW(
                hKey, n, namePtr, lpcchValueName,
                null, lpType, valuePtr, lpcbData
            ))
            val name = namePtr.getWideStringz()
            val valueSize = lpcbData.getS32()
            val type = lpType.getS32()
            //println("listValues=$nvalues: name=$name, value=$valuePtr")

            out[name] = convertValue(type, valuePtr, valueSize)
        }
        return out
    }

    fun listSubKeys(path: String): List<String> {
        val (root, keyPath) = parsePathEx(path) ?: return KEY_MAP.keys.toList()

        return openUseKey(root, keyPath, KEY_READ) { key -> listSubKeys(key) }
    }

    fun listValues(path: String): Map<String, Any?> = ffiScoped {
        val (root, keyPath) = parsePathEx(path) ?: return emptyMap()
        return openUseKey(root, keyPath, KEY_READ) { key -> listValues(key) }
    }

    fun getValue(path: String): Any? {
        val (root, keyPathPath, valueName) = parsePathWithValueEx(path) ?: return null
        return ffiScoped {
            val lpType = allocBytes(4)
            val lpcbData = allocBytes(4)
            val v = Advapi32.RegGetValueW(root, keyPathPath, valueName, RRF_RT_ANY, lpType, null, lpcbData)
            if (v == ERROR_FILE_NOT_FOUND) return null
            checkSuccess(v, extraValid = ERROR_INSUFFICIENT_BUFFER)
            val bytesSize = lpcbData.getS32()
            val dataPtr = allocBytes(bytesSize)
            checkSuccess(Advapi32.RegGetValueW(root, keyPathPath, valueName, RRF_RT_ANY, lpType, dataPtr, lpcbData))
            val type = lpType.getS32()
            convertValue(type, dataPtr, bytesSize)
        }
    }

    private fun convertValue(type: Int, dataPtr: FFIPointer, bytesSize: Int): Any? {
        return when (type) {
            REG_NONE -> null
            REG_DWORD -> dataPtr.getS32()
            REG_QWORD -> dataPtr.getS64()
            REG_BINARY -> dataPtr.getByteArray(bytesSize)
            REG_SZ, REG_EXPAND_SZ -> dataPtr.getByteArray(bytesSize).toString(Charsets.UTF16_LE).trimEnd('\u0000')
            //REG_MULTI_SZ -> TODO()
            else -> TODO("type=$type, bytesSize=$bytesSize")
        }
    }

    fun setValue(path: String, value: Any?) {
        val (root, keyName, valueName) = parsePathWithValueEx(path) ?: return
        openUseKey(root, keyName, KEY_READ or KEY_WRITE) { key ->
            ffiScoped {
                when (value) {
                    null -> Advapi32.RegSetValueExW(key, valueName, 0, REG_NONE, null, 0)
                    is Int -> Advapi32.RegSetValueExW(key, valueName, 0, REG_DWORD, allocBytes(4).also { it.set32(value) }, 4)
                    is Long -> Advapi32.RegSetValueExW(key, valueName, 0, REG_QWORD, allocBytes(8).also { it.set64(value) }, 8)
                    is ByteArray, is List<*>, is String -> {
                        val kind = when (value) {
                            is ByteArray -> REG_BINARY
                            is List<*> -> REG_MULTI_SZ
                            else -> REG_SZ
                        }
                        val data: ByteArray = when (value) {
                            is ByteArray -> value
                            is List<*> -> value.map { it.toString() }.joinToString("") { "$value\u0000" }.toByteArray(Charsets.UTF16_LE)
                            else -> "$value\u0000".toByteArray(Charsets.UTF16_LE)
                        }
                        Advapi32.RegSetValueExW(key, valueName, 0, kind, allocBytes(data.size) { data[it] }, data.size)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun deleteValue(path: String) {
        val (root, keyName, valueName) = parsePathWithValueEx(path) ?: return
        openUseKey(root, keyName, KEY_READ or KEY_WRITE) { key ->
            checkSuccess(Advapi32.RegDeleteValueW(key, valueName))
        }
    }

    fun deleteKey(path: String) {
        val (root, keyName) = parsePathEx(path) ?: return
        checkSuccess(Advapi32.RegDeleteKeyW(root, keyName))
    }

    private fun checkSuccess(rc: Int, extraValid: Int = ERROR_SUCCESS): Int {
        if (rc != ERROR_SUCCESS && rc != extraValid) throw RegistryException(rc)
        return rc
    }

    fun createKey(path: String): Boolean {
        val (root, keyName) = parsePathEx(path) ?: return false
        return ffiScoped {
            //return Advapi32Util.registryCreateKey(root, keyName)
            val samDesiredExtra = 0
            val phkResult = allocBytes(4)
            val lpdwDisposition = allocBytes(4)
            checkSuccess(Advapi32.RegCreateKeyExW(
                root, keyName, 0, null,
                REG_OPTION_NON_VOLATILE, KEY_READ or samDesiredExtra, null, phkResult,
                lpdwDisposition
            ))
            checkSuccess(Advapi32.RegCloseKey(phkResult.getS32()))
            REG_CREATED_NEW_KEY == lpdwDisposition.getS32()
        }
    }

    fun hasKey(path: String): Boolean {
        val (root, keyName) = parsePathEx(path) ?: return false

        return try {
            openUseKey(root, keyName, KEY_READ) {}
            true
        } catch (e: RegistryException) {
            if (e.errorCode != ERROR_FILE_NOT_FOUND) throw e
            false
        }
    }

    private inline fun <T> openUseKey(hKey: HKEY, keyName: String, samDesired: Int, block: (key: HKEY) -> T): T {
        return ffiScoped {
            val phkKey = allocBytes(8)
            checkSuccess(Advapi32.RegOpenKeyExW(hKey, keyName, 0, samDesired, phkKey))
            val key = phkKey.getS32()
            try {
                block(key)
            } finally {
                checkSuccess(Advapi32.RegCloseKey(key))
            }
        }
    }

    fun hasValue(path: String): Boolean = getValue(path) != null
    fun listValueKeys(path: String): List<String> = listValues(path).keys.toList()
}
