package korlibs.logger.internal

internal actual val miniEnvironmentVariables: Map<String, String> by lazy { System.getenv() }
