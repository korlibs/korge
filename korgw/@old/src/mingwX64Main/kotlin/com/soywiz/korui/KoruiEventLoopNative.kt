package com.soywiz.korui

import com.soywiz.korio.async.*
import com.soywiz.korui.light.*
import com.soywiz.korio.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.file.*
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.kds.*
import com.soywiz.kgl.*
import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.*
import platform.opengl32.*
import kotlin.reflect.KClass
import com.soywiz.korio.async.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import com.soywiz.korev.Key

@UseExperimental(InternalCoroutinesApi::class)
class MyNativeCoroutineDispatcher() : CoroutineDispatcher(), Delay, Closeable {
    override fun dispatchYield(context: CoroutineContext, block: Runnable): Unit = dispatch(context, block)

    class TimedTask(val ms: DateTime, val continuation: CancellableContinuation<Unit>)

    val tasks = Queue<Runnable>()
    val timedTasks = PriorityQueue<TimedTask>(Comparator<TimedTask> { a, b -> a.ms.compareTo(b.ms) })

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        tasks.enqueue(block)
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>): Unit {
        val task = TimedTask(DateTime.now() + timeMillis.milliseconds, continuation)
        continuation.invokeOnCancellation {
            timedTasks.remove(task)
        }
        timedTasks.add(task)
    }

    fun executeStep() {
        val now = DateTime.now()
        while (timedTasks.isNotEmpty() && now >= timedTasks.head.ms) {
            timedTasks.removeHead().continuation.resume(Unit)
        }

        while (tasks.isNotEmpty()) {
            val task = tasks.dequeue()
            task.run()
        }
    }

    override fun close() {

    }

    override fun toString(): String = "MyNativeCoroutineDispatcher"
}

@ThreadLocal
val myNativeCoroutineDispatcher: MyNativeCoroutineDispatcher = MyNativeCoroutineDispatcher()

actual val KoruiDispatcher: CoroutineDispatcher get() = myNativeCoroutineDispatcher

class NativeKoruiContext(
	val ag: AG,
	val light: LightComponents
	//, val app: NSApplication
) : KoruiContext()

class NativeLightComponents(val nkcAg: AG) : LightComponents() {
	val frameHandle = Any()

	override fun create(type: LightType, config: Any?): LightComponentInfo {
		@Suppress("REDUNDANT_ELSE_IN_WHEN")
		val handle: Any = when (type) {
			LightType.FRAME -> frameHandle
			LightType.CONTAINER -> Any()
			LightType.BUTTON -> Any()
			LightType.IMAGE -> Any()
			LightType.PROGRESS -> Any()
			LightType.LABEL -> Any()
			LightType.TEXT_FIELD -> Any()
			LightType.TEXT_AREA -> Any()
			LightType.CHECK_BOX -> Any()
			LightType.SCROLL_PANE -> Any()
			LightType.AGCANVAS -> nkcAg.nativeComponent
			else -> throw UnsupportedOperationException("Type: $type")
		}
		return LightComponentInfo(handle).apply {
			this.ag = nkcAg
		}
	}

	val eds = arrayListOf<Pair<KClass<*>, EventDispatcher>>()

	fun <T : Event> dispatch(clazz: KClass<T>, e: T) {
		for ((eclazz, ed) in eds) {
			if (eclazz == clazz) {
				ed.dispatch(clazz, e)
			}
		}
	}

	inline fun <reified T : Event> dispatch(e: T) = dispatch(T::class, e)

	override fun <T : Event> registerEventKind(c: Any, clazz: KClass<T>, ed: EventDispatcher): Closeable {
		val pair = Pair(clazz, ed)

		if (c === frameHandle || c === nkcAg.nativeComponent) {
			eds += pair
			return Closeable { eds -= pair }
		}

		return DummyCloseable
	}

	override suspend fun dialogOpenFile(c: Any, filter: String): VfsFile {
		// openSelectFile(initialDir: String? = null, filters: List<FileFilter> = listOf(FileFilter("All (*.*)", "*.*")), hwnd: HWND? = null)
		val selectedFile = openSelectFile(hwnd = hwnd)
		if (selectedFile != null) {
			return com.soywiz.korio.file.std.localVfs(selectedFile)
		} else {
			throw com.soywiz.korio.lang.CancelException()
		}
	}
}

@ThreadLocal
var hwnd: HWND? = null

data class WindowConfig(
	val width: Int = 640,
	val height: Int = 480,
	val title: String = "Korui"
)

@ThreadLocal
val agNativeComponent = Any()

@ThreadLocal
val ag: AG = AGOpenglFactory.create(agNativeComponent).create(agNativeComponent, AGConfig())

@ThreadLocal
val light = NativeLightComponents(ag)

@ThreadLocal
val ctx = NativeKoruiContext(ag, light)

@ThreadLocal
val windowConfig = WindowConfig()

internal actual suspend fun KoruiWrap(entry: suspend (KoruiContext) -> Unit) {
	memScoped {
		// https://www.khronos.org/opengl/wiki/Creating_an_OpenGL_Context_(WGL)

		val windowTitle = windowConfig.title
		val windowWidth = windowConfig.width
		val windowHeight = windowConfig.height

		val wc = alloc<WNDCLASSW>()

		val clazzName = "oglkotlinnative"
		val clazzNamePtr = clazzName.wcstr.getPointer(this@memScoped)
		wc.lpfnWndProc = staticCFunction(::WndProc)
		wc.hInstance = null
		wc.hbrBackground = COLOR_BACKGROUND.uncheckedCast()

		val hInstance = GetModuleHandleA(null)
		//FindResourceA(null, null, 124)
		//wc.hIcon = LoadIconAFunc(hInstance, 1033)
		//wc.hIcon = LoadIconAFunc(hInstance, 1)
		//wc.hIcon = LoadIconAFunc(hInstance, 32512)
		//wc.hIcon = LoadIconAFunc(null, 32512) // IDI_APPLICATION - MAKEINTRESOURCE(32512)

		wc.lpszClassName = clazzNamePtr.reinterpret()
		wc.style = CS_OWNDC.convert()
		if (RegisterClassW(wc.ptr).toInt() == 0) {
			return
		}

		val screenWidth = GetSystemMetrics(SM_CXSCREEN)
		val screenHeight = GetSystemMetrics(SM_CYSCREEN)
		hwnd = CreateWindowExW(
			WS_EX_CLIENTEDGE.convert(),
			clazzName,
			windowTitle,
			(WS_OVERLAPPEDWINDOW or WS_VISIBLE).convert(),
			kotlin.math.min(kotlin.math.max(0, (screenWidth - windowWidth) / 2), screenWidth - 16).convert(),
			kotlin.math.min(kotlin.math.max(0, (screenHeight - windowHeight) / 2), screenHeight - 16).convert(),
			windowWidth.convert(),
			windowHeight.convert(),
			null, null, null, null
		)
		println("ERROR: " + GetLastError())

		ShowWindow(hwnd, SW_SHOWNORMAL.convert())

		//SetTimer(hwnd, 1, 1000 / 60, staticCFunction(::WndTimer))
	}

	runBlocking {
		var running = true
		launch(KoruiDispatcher) {
			try {
				entry(ctx)
			} catch (e: Throwable) {
				println(e)
				running = false
			}
		}

		memScoped {
			val fps = 60
			val msPerFrame = 1000 / fps
			val msg = alloc<MSG>()
			//var start = milliStamp()
			var prev = DateTime.nowUnixLong()
			while (running) {
				while (
					PeekMessageW(
						msg.ptr,
						null,
						0.convert(),
						0.convert(),
						PM_REMOVE.convert()
					).toInt() != 0
				) {
					TranslateMessage(msg.ptr)
					DispatchMessageW(msg.ptr)
				}
				//val now = milliStamp()
				//val elapsed = now - start
				//val sleepTime = kotlin.math.max(0L, (16 - elapsed)).toInt()
				//println("SLEEP: sleepTime=$sleepTime, start=$start, now=$now, elapsed=$elapsed")
				//start = now
				//Sleep(sleepTime)
				val now = DateTime.nowUnixLong()
				val elapsed = now - prev
				//println("$prev, $now, $elapsed")
				val sleepTime = kotlin.math.max(0L, (msPerFrame - elapsed)).toInt()
				prev = now

				Sleep(sleepTime.convert())
				myNativeCoroutineDispatcher.executeStep()
				tryRender()
			}
		}
	}
}

@ThreadLocal
var glRenderContext: HGLRC? = null

// @TODO: when + cases with .toUInt() or .convert() didn't work
val _WM_CREATE: UINT = WM_CREATE.convert()
val _WM_SIZE: UINT = WM_SIZE.convert()
val _WM_QUIT: UINT = WM_QUIT.convert()
val _WM_MOUSEMOVE: UINT = WM_MOUSEMOVE.convert()
val _WM_LBUTTONDOWN: UINT = WM_LBUTTONDOWN.convert()
val _WM_MBUTTONDOWN: UINT = WM_MBUTTONDOWN.convert()
val _WM_RBUTTONDOWN: UINT = WM_RBUTTONDOWN.convert()
val _WM_LBUTTONUP: UINT = WM_LBUTTONUP.convert()
val _WM_MBUTTONUP: UINT = WM_MBUTTONUP.convert()
val _WM_RBUTTONUP: UINT = WM_RBUTTONUP.convert()
val _WM_KEYDOWN: UINT = WM_KEYDOWN.convert()
val _WM_KEYUP: UINT = WM_KEYUP.convert()
val _WM_CLOSE: UINT = WM_CLOSE.convert()

@Suppress("UNUSED_PARAMETER")
fun WndProc(hWnd: HWND?, message: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
	//println("WndProc: $hWnd, $message, $wParam, $lParam")
	when (message) {
		_WM_CREATE -> {
			memScoped {
				val pfd = alloc<PIXELFORMATDESCRIPTOR>()
				pfd.nSize = PIXELFORMATDESCRIPTOR.size.convert()
				pfd.nVersion = 1.convert()
				pfd.dwFlags = (PFD_DRAW_TO_WINDOW or PFD_SUPPORT_OPENGL or PFD_DOUBLEBUFFER).convert()
				pfd.iPixelType = PFD_TYPE_RGBA.convert()
				pfd.cColorBits = 32.convert()
				pfd.cDepthBits = 24.convert()
				pfd.cStencilBits = 8.convert()
				pfd.iLayerType = PFD_MAIN_PLANE.convert()
				val hDC = GetDC(hWnd)
				val letWindowsChooseThisPixelFormat = ChoosePixelFormat(hDC, pfd.ptr)

				SetPixelFormat(hDC, letWindowsChooseThisPixelFormat, pfd.ptr)
				glRenderContext = wglCreateContext(hDC)
				wglMakeCurrent(hDC, glRenderContext)

				val wglSwapIntervalEXT = wglGetProcAddressAny("wglSwapIntervalEXT")
						.uncheckedCast<CPointer<CFunction<Function1<Int, Int>>>?>()

				println("wglSwapIntervalEXT: $wglSwapIntervalEXT")
				wglSwapIntervalEXT?.invoke(0)

				println("GL_CONTEXT: $glRenderContext")
			}
		}
		_WM_SIZE -> {
			var width = 0
			var height = 0
			memScoped {
				val rect = alloc<RECT>()
				GetClientRect(hWnd, rect.ptr)
				width = (rect.right - rect.left).convert()
				height = (rect.bottom - rect.top).convert()
			}
			//val width = (lParam.toInt() ushr 0) and 0xFFFF
			//val height = (lParam.toInt() ushr 16) and 0xFFFF
			resized(width, height)
		}
		_WM_QUIT -> {
			kotlin.system.exitProcess(0.convert())
		}
		_WM_MOUSEMOVE -> {
			val x = (lParam.toInt() ushr 0) and 0xFFFF
			val y = (lParam.toInt() ushr 16) and 0xFFFF
			mouseMove(x, y)
		}
		_WM_LBUTTONDOWN -> mouseButton(0, true)
		_WM_MBUTTONDOWN -> mouseButton(1, true)
		_WM_RBUTTONDOWN -> mouseButton(2, true)
		_WM_LBUTTONUP -> mouseButton(0, false)
		_WM_MBUTTONUP -> mouseButton(1, false)
		_WM_RBUTTONUP -> mouseButton(2, false)
		_WM_KEYDOWN -> keyUpdate(wParam.toInt(), true)
		_WM_KEYUP -> keyUpdate(wParam.toInt(), false)
		_WM_CLOSE -> {
			kotlin.system.exitProcess(0)
		}
	}
	return DefWindowProcW(hWnd, message, wParam, lParam)
}

val reshapeEvent = com.soywiz.korev.ReshapeEvent()

fun resized(width: Int, height: Int) {
	ag.resized(width, height)
	light.dispatch(reshapeEvent.apply {
		this.width = width
		this.height = height
	})

	tryRender()
}

fun tryRender() {
	if (hwnd != null && glRenderContext != null) {
		val hdc = GetDC(hwnd)
		//println("render")
		wglMakeCurrent(hdc, glRenderContext)
		//renderFunction()
		ag.onRender(ag)
		SwapBuffers(hdc)
	}
}

val keyEvent = com.soywiz.korev.KeyEvent()

fun keyUpdate(keyCode: Int, down: Boolean) {
	// @TODO: KeyEvent.Tpe.TYPE
	light.dispatch(keyEvent.apply {
		this.type = if (down) com.soywiz.korev.KeyEvent.Type.DOWN else com.soywiz.korev.KeyEvent.Type.UP
		this.id = 0
		this.key = KEYS[keyCode] ?: com.soywiz.korev.Key.UNKNOWN
		this.keyCode = keyCode
		this.character = keyCode.toChar()
	})
}

val mevent = com.soywiz.korev.MouseEvent()
private fun mouseEvent(etype: com.soywiz.korev.MouseEvent.Type, ex: Int, ey: Int, ebutton: Int) {
	light.dispatch(mevent.apply {
		this.type = etype
		this.x = ex
		this.y = ey
		this.buttons = 1 shl ebutton
		this.isAltDown = false
		this.isCtrlDown = false
		this.isShiftDown = false
		this.isMetaDown = false
		//this.scaleCoords = false
	})
}

val COMDLG32_DLL: HMODULE? by lazy { LoadLibraryA("comdlg32.dll") }

val GetOpenFileNameWFunc by lazy {
	GetProcAddress(COMDLG32_DLL, "GetOpenFileNameW") as CPointer<CFunction<Function1<CPointer<OPENFILENAMEW>, BOOL>>>
}

data class FileFilter(val name: String, val pattern: String)

fun openSelectFile(initialDir: String? = null, filters: List<FileFilter> = listOf(FileFilter("All (*.*)", "*.*")), hwnd: HWND? = null): String? = memScoped {
	val szFileSize = 1024
	val szFile = allocArray<WCHARVar>(szFileSize + 1)
	val ofn = alloc<OPENFILENAMEW>().apply {
		lStructSize = OPENFILENAMEW.size.convert()
		hwndOwner = hwnd
		lpstrFile = szFile.reinterpret()
		nMaxFile = szFileSize.convert()
		lpstrFilter = (filters.flatMap { listOf(it.name, it.pattern) }.joinToString("\u0000") + "\u0000").wcstr.ptr.reinterpret()
		nFilterIndex = 1.convert()
		lpstrFileTitle = null
		nMaxFileTitle = 0.convert()
		lpstrInitialDir = if (initialDir != null) initialDir.wcstr.ptr.reinterpret() else null
		Flags = (OFN_PATHMUSTEXIST or OFN_FILEMUSTEXIST).convert()
	}
	val res = GetOpenFileNameWFunc(ofn.ptr.reinterpret())
	if (res.toInt() != 0) szFile.reinterpret<ShortVar>().toKString() else null
}

@ThreadLocal
private var mouseX: Int = 0

@ThreadLocal
private var mouseY: Int = 0

//@ThreadLocal
//private val buttons = BooleanArray(16)

fun mouseMove(x: Int, y: Int) {
	mouseX = x
	mouseY = y
	SetCursor(ARROW_CURSOR)
	mouseEvent(com.soywiz.korev.MouseEvent.Type.MOVE, mouseX, mouseY, 0)
}

fun mouseButton(button: Int, down: Boolean) {
	//buttons[button] = down
	if (down) {
		mouseEvent(com.soywiz.korev.MouseEvent.Type.DOWN, mouseX, mouseY, button)
	} else {
		mouseEvent(com.soywiz.korev.MouseEvent.Type.UP, mouseX, mouseY, button)
		mouseEvent(com.soywiz.korev.MouseEvent.Type.CLICK, mouseX, mouseY, button) // @TODO: Conditionally depending on the down x,y & time
	}
}

//val String.glstr: CPointer<GLcharVar> get() = this.cstr.reinterpret()
val OPENGL32_DLL_MODULE: HMODULE? by lazy { LoadLibraryA("opengl32.dll") }

fun wglGetProcAddressAny(name: String): PROC? {
	return wglGetProcAddress(name)
		?: GetProcAddress(OPENGL32_DLL_MODULE, name)
		?: throw RuntimeException("Can't find GL function: '$name'")
}

val USER32_DLL by lazy { LoadLibraryA("User32.dll") }
val LoadCursorAFunc by lazy {
	GetProcAddress(
		USER32_DLL,
		"LoadCursorA"
	).uncheckedCast<CPointer<CFunction<Function2<Int, Int, HCURSOR?>>>>()
}

val LoadIconAFunc by lazy {
	GetProcAddress(
		USER32_DLL,
		"LoadIconA"
	).uncheckedCast<CPointer<CFunction<Function2<HMODULE?, Int, HICON?>>>>()
}

val FindResourceAFunc by lazy {
	GetProcAddress(
		USER32_DLL,
		"FindResourceA"
	).uncheckedCast<CPointer<CFunction<Function2<HMODULE?, Int, HICON?>>>>()
}

//val ARROW_CURSOR by lazy { LoadCursorA(null, 32512.uncheckedCast<CPointer<ByteVar>>().reinterpret()) }
val ARROW_CURSOR by lazy { LoadCursorAFunc(0, 32512) }

const val VK_ABNT_C1 = 0xC1
const val VK_ABNT_C2 = 0xC2
const val VK_ADD = 0x6B
const val VK_ATTN = 0xF6
const val VK_BACK = 0x08
const val VK_CANCEL = 0x03
const val VK_CLEAR = 0x0C
const val VK_CRSEL = 0xF7
const val VK_DECIMAL = 0x6E
const val VK_DIVIDE = 0x6F
const val VK_EREOF = 0xF9
const val VK_ESCAPE = 0x1B
const val VK_EXECUTE = 0x2B
const val VK_EXSEL = 0xF8
const val VK_ICO_CLEAR = 0xE6
const val VK_ICO_HELP = 0xE3
const val VK_KEY_0 = 0x30
const val VK_KEY_1 = 0x31
const val VK_KEY_2 = 0x32
const val VK_KEY_3 = 0x33
const val VK_KEY_4 = 0x34
const val VK_KEY_5 = 0x35
const val VK_KEY_6 = 0x36
const val VK_KEY_7 = 0x37
const val VK_KEY_8 = 0x38
const val VK_KEY_9 = 0x39
const val VK_KEY_A = 0x41
const val VK_KEY_B = 0x42
const val VK_KEY_C = 0x43
const val VK_KEY_D = 0x44
const val VK_KEY_E = 0x45
const val VK_KEY_F = 0x46
const val VK_KEY_G = 0x47
const val VK_KEY_H = 0x48
const val VK_KEY_I = 0x49
const val VK_KEY_J = 0x4A
const val VK_KEY_K = 0x4B
const val VK_KEY_L = 0x4C
const val VK_KEY_M = 0x4D
const val VK_KEY_N = 0x4E
const val VK_KEY_O = 0x4F
const val VK_KEY_P = 0x50
const val VK_KEY_Q = 0x51
const val VK_KEY_R = 0x52
const val VK_KEY_S = 0x53
const val VK_KEY_T = 0x54
const val VK_KEY_U = 0x55
const val VK_KEY_V = 0x56
const val VK_KEY_W = 0x57
const val VK_KEY_X = 0x58
const val VK_KEY_Y = 0x59
const val VK_KEY_Z = 0x5A
const val VK_MULTIPLY = 0x6A
const val VK_NONAME = 0xFC
const val VK_NUMPAD0 = 0x60
const val VK_NUMPAD1 = 0x61
const val VK_NUMPAD2 = 0x62
const val VK_NUMPAD3 = 0x63
const val VK_NUMPAD4 = 0x64
const val VK_NUMPAD5 = 0x65
const val VK_NUMPAD6 = 0x66
const val VK_NUMPAD7 = 0x67
const val VK_NUMPAD8 = 0x68
const val VK_NUMPAD9 = 0x69
const val VK_OEM_1 = 0xBA
const val VK_OEM_102 = 0xE2
const val VK_OEM_2 = 0xBF
const val VK_OEM_3 = 0xC0
const val VK_OEM_4 = 0xDB
const val VK_OEM_5 = 0xDC
const val VK_OEM_6 = 0xDD
const val VK_OEM_7 = 0xDE
const val VK_OEM_8 = 0xDF
const val VK_OEM_ATTN = 0xF0
const val VK_OEM_AUTO = 0xF3
const val VK_OEM_AX = 0xE1
const val VK_OEM_BACKTAB = 0xF5
const val VK_OEM_CLEAR = 0xFE
const val VK_OEM_COMMA = 0xBC
const val VK_OEM_COPY = 0xF2
const val VK_OEM_CUSEL = 0xEF
const val VK_OEM_ENLW = 0xF4
const val VK_OEM_FINISH = 0xF1
const val VK_OEM_FJ_LOYA = 0x95
const val VK_OEM_FJ_MASSHOU = 0x93
const val VK_OEM_FJ_ROYA = 0x96
const val VK_OEM_FJ_TOUROKU = 0x94
const val VK_OEM_JUMP = 0xEA
const val VK_OEM_MINUS = 0xBD
const val VK_OEM_PA1 = 0xEB
const val VK_OEM_PA2 = 0xEC
const val VK_OEM_PA3 = 0xED
const val VK_OEM_PERIOD = 0xBE
const val VK_OEM_PLUS = 0xBB
const val VK_OEM_RESET = 0xE9
const val VK_OEM_WSCTRL = 0xEE
const val VK_PA1 = 0xFD
const val VK_PACKET = 0xE7
const val VK_PLAY = 0xFA
const val VK_PROCESSKEY = 0xE5
const val VK_RETURN = 0x0D
const val VK_SELECT = 0x29
const val VK_SEPARATOR = 0x6C
const val VK_SPACE = 0x20
const val VK_SUBTRACT = 0x6D
const val VK_TAB = 0x09
const val VK_ZOOM = 0xFB
const val VK__none_ = 0xFF
const val VK_ACCEPT = 0x1E
const val VK_APPS = 0x5D
const val VK_BROWSER_BACK = 0xA6
const val VK_BROWSER_FAVORITES = 0xAB
const val VK_BROWSER_FORWARD = 0xA7
const val VK_BROWSER_HOME = 0xAC
const val VK_BROWSER_REFRESH = 0xA8
const val VK_BROWSER_SEARCH = 0xAA
const val VK_BROWSER_STOP = 0xA9
const val VK_CAPITAL = 0x14
const val VK_CONVERT = 0x1C
const val VK_DELETE = 0x2E
const val VK_DOWN = 0x28
const val VK_END = 0x23
const val VK_F1 = 0x70
const val VK_F10 = 0x79
const val VK_F11 = 0x7A
const val VK_F12 = 0x7B
const val VK_F13 = 0x7C
const val VK_F14 = 0x7D
const val VK_F15 = 0x7E
const val VK_F16 = 0x7F
const val VK_F17 = 0x80
const val VK_F18 = 0x81
const val VK_F19 = 0x82
const val VK_F2 = 0x71
const val VK_F20 = 0x83
const val VK_F21 = 0x84
const val VK_F22 = 0x85
const val VK_F23 = 0x86
const val VK_F24 = 0x87
const val VK_F3 = 0x72
const val VK_F4 = 0x73
const val VK_F5 = 0x74
const val VK_F6 = 0x75
const val VK_F7 = 0x76
const val VK_F8 = 0x77
const val VK_F9 = 0x78
const val VK_FINAL = 0x18
const val VK_HELP = 0x2F
const val VK_HOME = 0x24
const val VK_ICO_00 = 0xE4
const val VK_INSERT = 0x2D
const val VK_JUNJA = 0x17
const val VK_KANA = 0x15
const val VK_KANJI = 0x19
const val VK_LAUNCH_APP1 = 0xB6
const val VK_LAUNCH_APP2 = 0xB7
const val VK_LAUNCH_MAIL = 0xB4
const val VK_LAUNCH_MEDIA_SELECT = 0xB5
const val VK_LBUTTON = 0x01
const val VK_LCONTROL = 0xA2
const val VK_LEFT = 0x25
const val VK_LMENU = 0xA4
const val VK_LSHIFT = 0xA0
const val VK_LWIN = 0x5B
const val VK_MBUTTON = 0x04
const val VK_MEDIA_NEXT_TRACK = 0xB0
const val VK_MEDIA_PLAY_PAUSE = 0xB3
const val VK_MEDIA_PREV_TRACK = 0xB1
const val VK_MEDIA_STOP = 0xB2
const val VK_MODECHANGE = 0x1F
const val VK_NEXT = 0x22
const val VK_NONCONVERT = 0x1D
const val VK_NUMLOCK = 0x90
const val VK_OEM_FJ_JISHO = 0x92
const val VK_PAUSE = 0x13
const val VK_PRINT = 0x2A
const val VK_PRIOR = 0x21
const val VK_RBUTTON = 0x02
const val VK_RCONTROL = 0xA3
const val VK_RIGHT = 0x27
const val VK_RMENU = 0xA5
const val VK_RSHIFT = 0xA1
const val VK_RWIN = 0x5C
const val VK_SCROLL = 0x91
const val VK_SLEEP = 0x5F
const val VK_SNAPSHOT = 0x2C
const val VK_UP = 0x26
const val VK_VOLUME_DOWN = 0xAE
const val VK_VOLUME_MUTE = 0xAD
const val VK_VOLUME_UP = 0xAF
const val VK_XBUTTON1 = 0x05
const val VK_XBUTTON2 = 0x06

val KEYS = mapOf(
	VK_ABNT_C1 to Key.UNKNOWN,
	VK_ABNT_C2 to Key.UNKNOWN,
	VK_ADD to Key.UNKNOWN,
	VK_ATTN to Key.UNKNOWN,
	VK_BACK to Key.UNKNOWN,
	VK_CANCEL to Key.UNKNOWN,
	VK_CLEAR to Key.UNKNOWN,
	VK_CRSEL to Key.UNKNOWN,
	VK_DECIMAL to Key.UNKNOWN,
	VK_DIVIDE to Key.UNKNOWN,
	VK_EREOF to Key.UNKNOWN,
	VK_ESCAPE to Key.ESCAPE,
	VK_EXECUTE to Key.UNKNOWN,
	VK_EXSEL to Key.UNKNOWN,
	VK_ICO_CLEAR to Key.UNKNOWN,
	VK_ICO_HELP to Key.UNKNOWN,
	VK_KEY_0 to Key.N0,
	VK_KEY_1 to Key.N1,
	VK_KEY_2 to Key.N2,
	VK_KEY_3 to Key.N3,
	VK_KEY_4 to Key.N4,
	VK_KEY_5 to Key.N5,
	VK_KEY_6 to Key.N6,
	VK_KEY_7 to Key.N7,
	VK_KEY_8 to Key.N8,
	VK_KEY_9 to Key.N9,
	VK_KEY_A to Key.A,
	VK_KEY_B to Key.B,
	VK_KEY_C to Key.C,
	VK_KEY_D to Key.D,
	VK_KEY_E to Key.E,
	VK_KEY_F to Key.F,
	VK_KEY_G to Key.G,
	VK_KEY_H to Key.H,
	VK_KEY_I to Key.I,
	VK_KEY_J to Key.J,
	VK_KEY_K to Key.K,
	VK_KEY_L to Key.L,
	VK_KEY_M to Key.M,
	VK_KEY_N to Key.N,
	VK_KEY_O to Key.O,
	VK_KEY_P to Key.P,
	VK_KEY_Q to Key.Q,
	VK_KEY_R to Key.R,
	VK_KEY_S to Key.S,
	VK_KEY_T to Key.T,
	VK_KEY_U to Key.U,
	VK_KEY_V to Key.V,
	VK_KEY_W to Key.W,
	VK_KEY_X to Key.X,
	VK_KEY_Y to Key.Y,
	VK_KEY_Z to Key.Z,
	VK_MULTIPLY to Key.UNKNOWN,
	VK_NONAME to Key.UNKNOWN,
	VK_NUMPAD0 to Key.N0,
	VK_NUMPAD1 to Key.N1,
	VK_NUMPAD2 to Key.N2,
	VK_NUMPAD3 to Key.N3,
	VK_NUMPAD4 to Key.N4,
	VK_NUMPAD5 to Key.N5,
	VK_NUMPAD6 to Key.N6,
	VK_NUMPAD7 to Key.N7,
	VK_NUMPAD8 to Key.N8,
	VK_NUMPAD9 to Key.N9,
	VK_OEM_1 to Key.UNKNOWN,
	VK_OEM_102 to Key.UNKNOWN,
	VK_OEM_2 to Key.UNKNOWN,
	VK_OEM_3 to Key.UNKNOWN,
	VK_OEM_4 to Key.UNKNOWN,
	VK_OEM_5 to Key.UNKNOWN,
	VK_OEM_6 to Key.UNKNOWN,
	VK_OEM_7 to Key.UNKNOWN,
	VK_OEM_8 to Key.UNKNOWN,
	VK_OEM_ATTN to Key.UNKNOWN,
	VK_OEM_AUTO to Key.UNKNOWN,
	VK_OEM_AX to Key.UNKNOWN,
	VK_OEM_BACKTAB to Key.UNKNOWN,
	VK_OEM_CLEAR to Key.UNKNOWN,
	VK_OEM_COMMA to Key.UNKNOWN,
	VK_OEM_COPY to Key.UNKNOWN,
	VK_OEM_CUSEL to Key.UNKNOWN,
	VK_OEM_ENLW to Key.UNKNOWN,
	VK_OEM_FINISH to Key.UNKNOWN,
	VK_OEM_FJ_LOYA to Key.UNKNOWN,
	VK_OEM_FJ_MASSHOU to Key.UNKNOWN,
	VK_OEM_FJ_ROYA to Key.UNKNOWN,
	VK_OEM_FJ_TOUROKU to Key.UNKNOWN,
	VK_OEM_JUMP to Key.UNKNOWN,
	VK_OEM_MINUS to Key.UNKNOWN,
	VK_OEM_PA1 to Key.UNKNOWN,
	VK_OEM_PA2 to Key.UNKNOWN,
	VK_OEM_PA3 to Key.UNKNOWN,
	VK_OEM_PERIOD to Key.UNKNOWN,
	VK_OEM_PLUS to Key.UNKNOWN,
	VK_OEM_RESET to Key.UNKNOWN,
	VK_OEM_WSCTRL to Key.UNKNOWN,
	VK_PA1 to Key.UNKNOWN,
	VK_PACKET to Key.UNKNOWN,
	VK_PLAY to Key.UNKNOWN,
	VK_PROCESSKEY to Key.UNKNOWN,
	VK_RETURN to Key.ENTER,
	VK_SELECT to Key.UNKNOWN,
	VK_SEPARATOR to Key.UNKNOWN,
	VK_SPACE to Key.SPACE,
	VK_SUBTRACT to Key.UNKNOWN,
	VK_TAB to Key.TAB,
	VK_ZOOM to Key.UNKNOWN,
	VK__none_ to Key.UNKNOWN,
	VK_ACCEPT to Key.UNKNOWN,
	VK_APPS to Key.UNKNOWN,
	VK_BROWSER_BACK to Key.UNKNOWN,
	VK_BROWSER_FAVORITES to Key.UNKNOWN,
	VK_BROWSER_FORWARD to Key.UNKNOWN,
	VK_BROWSER_HOME to Key.UNKNOWN,
	VK_BROWSER_REFRESH to Key.UNKNOWN,
	VK_BROWSER_SEARCH to Key.UNKNOWN,
	VK_BROWSER_STOP to Key.UNKNOWN,
	VK_CAPITAL to Key.UNKNOWN,
	VK_CONVERT to Key.UNKNOWN,
	VK_DELETE to Key.DELETE,
	VK_DOWN to Key.DOWN,
	VK_END to Key.END,
	VK_F1 to Key.F1,
	VK_F2 to Key.F2,
	VK_F3 to Key.F3,
	VK_F4 to Key.F4,
	VK_F5 to Key.F5,
	VK_F6 to Key.F6,
	VK_F7 to Key.F7,
	VK_F8 to Key.F8,
	VK_F9 to Key.F9,
	VK_F10 to Key.F10,
	VK_F11 to Key.F11,
	VK_F12 to Key.F12,
	VK_F13 to Key.F13,
	VK_F14 to Key.F14,
	VK_F15 to Key.F15,
	VK_F16 to Key.F16,
	VK_F17 to Key.F17,
	VK_F18 to Key.F18,
	VK_F19 to Key.F19,
	VK_F20 to Key.F20,
	VK_F21 to Key.F21,
	VK_F22 to Key.F22,
	VK_F23 to Key.F23,
	VK_F24 to Key.F24,
	VK_FINAL to Key.UNKNOWN,
	VK_HELP to Key.UNKNOWN,
	VK_HOME to Key.HOME,
	VK_ICO_00 to Key.UNKNOWN,
	VK_INSERT to Key.INSERT,
	VK_JUNJA to Key.UNKNOWN,
	VK_KANA to Key.UNKNOWN,
	VK_KANJI to Key.UNKNOWN,
	VK_LAUNCH_APP1 to Key.UNKNOWN,
	VK_LAUNCH_APP2 to Key.UNKNOWN,
	VK_LAUNCH_MAIL to Key.UNKNOWN,
	VK_LAUNCH_MEDIA_SELECT to Key.UNKNOWN,
	VK_LBUTTON to Key.UNKNOWN,
	VK_LCONTROL to Key.LEFT_CONTROL,
	VK_LEFT to Key.LEFT,
	VK_LMENU to Key.UNKNOWN,
	VK_LSHIFT to Key.LEFT_SHIFT,
	VK_LWIN to Key.LEFT_SUPER,
	VK_MBUTTON to Key.UNKNOWN,
	VK_MEDIA_NEXT_TRACK to Key.UNKNOWN,
	VK_MEDIA_PLAY_PAUSE to Key.UNKNOWN,
	VK_MEDIA_PREV_TRACK to Key.UNKNOWN,
	VK_MEDIA_STOP to Key.UNKNOWN,
	VK_MODECHANGE to Key.UNKNOWN,
	VK_NEXT to Key.UNKNOWN,
	VK_NONCONVERT to Key.UNKNOWN,
	VK_NUMLOCK to Key.UNKNOWN,
	VK_OEM_FJ_JISHO to Key.UNKNOWN,
	VK_PAUSE to Key.PAUSE,
	VK_PRINT to Key.PRINT_SCREEN,
	VK_PRIOR to Key.UNKNOWN,
	VK_RBUTTON to Key.UNKNOWN,
	VK_RCONTROL to Key.RIGHT_CONTROL,
	VK_RIGHT to Key.RIGHT,
	VK_RMENU to Key.UNKNOWN,
	VK_RSHIFT to Key.RIGHT_SHIFT,
	VK_RWIN to Key.RIGHT_SUPER,
	VK_SCROLL to Key.SCROLL_LOCK,
	VK_SLEEP to Key.UNKNOWN,
	VK_SNAPSHOT to Key.UNKNOWN,
	VK_UP to Key.UP,
	VK_VOLUME_DOWN to Key.UNKNOWN,
	VK_VOLUME_MUTE to Key.UNKNOWN,
	VK_VOLUME_UP to Key.UNKNOWN,
	VK_XBUTTON1 to Key.UNKNOWN,
	VK_XBUTTON2 to Key.UNKNOWN
)
