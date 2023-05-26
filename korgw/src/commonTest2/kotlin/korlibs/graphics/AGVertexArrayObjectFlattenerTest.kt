package korlibs.graphics

import korlibs.graphics.shader.*
import korlibs.graphics.shader.Attribute
import korlibs.memory.*
import kotlin.test.*

class AGVertexArrayObjectFlattenerTest {

    //TODO: add tests
    @Test
    fun flatten() {
    }

    @Test
    fun should_find_correct_data_when_extract_data_from_AGBuffer() {
        //Given buffer incremental numbers
        val buffer = AGBuffer().upload(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        //And vertex layout
        val vertexLayout = VertexLayout(
            Attribute("none", VarType.UByte2, normalized = false, precision = Precision.HIGH, fixedLocation = 0),
            Attribute("none2", VarType.UByte2, normalized = false, precision = Precision.HIGH, fixedLocation = 0)
        )
        val expectedExtractedData = listOf(
            listOf(1, 2, 5, 6, 9, 10),
            listOf(3, 4, 7, 8, 11, 12)
        )

        //When extract data of attribute of attributes
        vertexLayout.items.forEachIndexed { attributeIndex, attribute ->
            val extractedData = buffer.extractDataOf(attribute, vertexLayout)
            val expectedValues = expectedExtractedData[attributeIndex]

            //Then
            expectedValues.forEachIndexed { index, expectedValue ->
                assertEquals(expectedValue, extractedData.mem!!.getUInt8(index))
            }
        }

    }
}
