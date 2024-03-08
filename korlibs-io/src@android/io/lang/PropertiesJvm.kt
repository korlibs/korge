package korlibs.io.lang

actual object SystemProperties : Properties() {
    // Uses querystring on JS/Browser, and proper env vars in the rest
    override operator fun get(key: String): String? = System.getProperty(key)
    override operator fun set(key: String, value: String) { System.setProperty(key, value) }
    override fun remove(key: String) { System.clearProperty(key) }
    override fun getAll() = System.getProperties().toMap() as Map<String, String>
}
