@file:OptIn(ExperimentalCompilerApi::class)

package korlibs.korge.kotlin.plugin

import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*

class KorgeCommandLineProcessor : CommandLineProcessor {
    companion object {
        private const val OPTION_STRING = "string"
        private const val OPTION_FILE = "file"

        val ARG_STRING = CompilerConfigurationKey<String>(OPTION_STRING)
        val ARG_FILE = CompilerConfigurationKey<String>(OPTION_FILE)
    }

    override val pluginId: String = BuildConfig.KOTLIN_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = OPTION_STRING,
            valueDescription = "string",
            description = "sample string argument",
            required = false,
        ),
        CliOption(
            optionName = OPTION_FILE,
            valueDescription = "file",
            description = "sample file argument",
            required = false,
        ),
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        return when (option.optionName) {
            OPTION_STRING -> configuration.put(ARG_STRING, value)
            OPTION_FILE -> configuration.put(ARG_FILE, value)
            else -> throw IllegalArgumentException("Unexpected config option ${option.optionName}")
        }
    }
}
