package kotlin.native.internal.kmem

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class TypedIntrinsic(val kind: String)
