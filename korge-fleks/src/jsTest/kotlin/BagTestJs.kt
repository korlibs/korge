import com.github.quillraven.fleks.collection.bag
import kotlin.test.Test
import kotlin.test.assertFailsWith

class GenericBagTestJs {
    @Test
    fun cannotGetStringValueOfInvalidOutOfBoundsIndex() {
        val bag = bag<String>(2)

        assertFailsWith<NoSuchElementException> { bag[2] }
    }
}
