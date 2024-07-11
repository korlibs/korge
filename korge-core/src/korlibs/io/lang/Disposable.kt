package korlibs.io.lang

import kotlinx.coroutines.*

@Deprecated("", replaceWith = ReplaceWith("kotlinx.coroutines.DisposableHandle"))
typealias Disposable = DisposableHandle

fun AutoCloseable.toDisposable(): DisposableHandle = DisposableHandle { this.close() }
