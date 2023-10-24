package korlibs.annotations

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class DeprecatedParameter(
    val reason: String
)
