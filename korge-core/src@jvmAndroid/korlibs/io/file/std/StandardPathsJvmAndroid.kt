package korlibs.io.file.std

open class StandardPathsJvmAndroid : StandardPathsBase {
    // File(".").absolutePath
    override val cwd: String get() = System.getProperty("user.dir")
    override val temp: String get() = System.getProperty("java.io.tmpdir")
}
