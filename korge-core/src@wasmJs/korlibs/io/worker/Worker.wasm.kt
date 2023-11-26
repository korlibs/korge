package korlibs.io.worker

actual annotation class WorkerExport()

@PublishedApi
internal actual val workerImpl: _WorkerImpl = object : _WorkerImpl() {
}
