package korlibs.template

class RawString(val str: String, val contentType: String? = null) {
    override fun toString(): String = str
}
