package korlibs.io.lang

expect open class IOException(msg: String) : Exception
expect open class EOFException(msg: String) : IOException
expect open class FileNotFoundException(msg: String) : IOException
class FileAlreadyExistsException(msg: String) : IOException(msg)
