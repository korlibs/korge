package korlibs.io.file.std

import korlibs.platform.*


// @TODO: DRY, try to use buildShellExecCommandLineArray
// @TODO: NodeJS fails on windows with special characters like & echo &
object ShellArgs {
    fun buildShellExecCommandLineForPopen(cmdAndArgs: List<String>): String = buildShellExecCommandLine(cmdAndArgs)
    fun buildShellExecCommandLineArrayForProcessBuilder(cmdAndArgs: List<String>): List<String> = buildShellExecCommandLineArray(cmdAndArgs)
    fun buildShellExecCommandLineArrayForExecl(cmdAndArgs: List<String>): List<String> = buildShellExecCommandLineArray(cmdAndArgs)
    fun buildShellExecCommandLineArrayForNodeSpawn(cmdAndArgs: List<String>): List<String> = (cmdAndArgs)

    fun buildShellExecCommandLineArray(cmdAndArgs: List<String>): List<String> = when {
        Platform.isWindows -> listOf("cmd", "/c", ShellArgs.escapeshellCommandWin(cmdAndArgs))
        Platform.isLinux -> listOf("/bin/sh", "-c", cmdAndArgs.joinToString(" ") { ShellArgs.escapeshellargUnix(it) })
        //OS.isLinux -> listOf("/bin/sh", "-c", "\"" + cmdAndArgs.joinToString(" ") { ShellArgs.escapeshellargUnix(it) } + "\"")
        //OS.isLinux -> listOf("/bin/sh", "-c", "'" + cmdAndArgs.joinToString(" ") { ShellArgs.escapeshellargUnix(it) }.replace("'", "'\"'\"'") + "'")
        else -> cmdAndArgs
    }

    fun buildShellExecCommandLine(cmdAndArgs: List<String>): String = when {
        Platform.isWindows -> cmdAndArgs.joinToString(" ") { ShellArgs.escapeshellargWin(it) }
        else -> "/bin/sh -c '" + cmdAndArgs.joinToString(" ") { ShellArgs.escapeshellargUnix(it) }.replace("'", "'\"'\"'") + "'"
    }

    fun escapeshellCommandUnix(args: List<String>): String {
        return escapeshellargUnix(args.joinToString(" ") { escapeshellargUnix(it) })
    }

    fun escapeshellargUnix(str: String): String {
        return buildString {
            append("'")
            for (c in str) {
                when (c) {
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    '\\' -> append("\\\\")
                    '\'' -> append("'\"'\"'") // https://stackoverflow.com/questions/1250079/how-to-escape-single-quotes-within-single-quoted-strings
                    else -> append(c)
                }
            }
            append("'")
        }
    }

    fun escapeshellCommandWin(args: List<String>): String {
        return "\"" + args.joinToString(" ") { escapeshellargWin(it) } + "\""
    }

    // https://sourcedaddy.com/windows-7/escaping-special-characters.html
    // https://stackoverflow.com/questions/17319224/escaping-illegal-characters-in-params
    fun escapeshellargWin(str: String): String {
        return buildString {
            for (c in str) {
                when (c) {
                    '<', '>', '(', ')', '&', '|', ',', ';', '^', '"', '\'', ' ', '\n', '\r', '\t' -> append('^')
                }
                append(c)
            }
        }
    }
}
