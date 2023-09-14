package korlibs.io.lang

inline fun assert(cond: Boolean) {
	if (!cond) throw AssertionError()
}

inline fun assert(cond: Boolean, message: () -> String) {
    if (!cond) throw AssertionError(message())
}

object Assert {
    inline fun <T> eq(expect: T, actual: T) {
        assert(expect == actual) { "Expected $expect to be $actual" }
    }
}
