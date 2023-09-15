package korlibs.io.process

import korlibs.io.file.VfsProcessHandler

expect suspend fun posixExec(
    path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler
): Int
