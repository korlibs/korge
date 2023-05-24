package korlibs.time

import java.io.*
import kotlin.test.*

class SerializableTest {
    fun <T> serializeDeserializeObject(instance: T): Any? {
        val bao = ByteArrayOutputStream().also { ObjectOutputStream(it).writeObject(instance) }.toByteArray()
        return ObjectInputStream(ByteArrayInputStream(bao)).readObject()
    }

    @Test
    fun test() {
        val time = 1_000_000L
        assertTrue { serializeDeserializeObject(DateTimeTz.fromUnixLocal(time)) is DateTimeTz }
    }
}
