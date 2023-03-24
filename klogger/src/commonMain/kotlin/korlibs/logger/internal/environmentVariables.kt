package korlibs.logger.internal

internal expect val miniEnvironmentVariables: Map<String, String> //= mapOf()
private var _miniEnvironmentVariablesUC: Map<String, String>? = null

internal val miniEnvironmentVariablesUC: Map<String, String> get() {
    if (_miniEnvironmentVariablesUC == null) {
        _miniEnvironmentVariablesUC = miniEnvironmentVariables.mapKeys { it.key.uppercase() }
    }
    return _miniEnvironmentVariablesUC!!
}