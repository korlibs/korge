import org.junit.*

class NewBinPackerTest {
    @Test
    fun test() {
        val options = NewBinPacker.IOption(
            smart = true,
            pot = true,
            square = false,
            allowRotation = true,
            tag = false,
            border = 5
        ) // Set packing options
        val packer = NewBinPacker.MaxRectsPacker(10000, 10000, 2, options) // width, height, padding, options

        val input = listOf( // any object with width & height is OK since v2.1.0
            NewBinPacker.Rectangle(width = 600, height = 20, name = "tree", allowRotation = true),
            NewBinPacker.Rectangle(width = 600, height = 20, name = "flower", allowRotation = true),
            NewBinPacker.Rectangle(width = 2000, height = 2000, name = "oversized background", allowRotation = true),
            NewBinPacker.Rectangle(width = 1300, height = 1000, name = "background", allowRotation = true),
            NewBinPacker.Rectangle(width = 1200, height = 1000, name = "overlay", allowRotation = true)
        )

        packer.addArray(input) // Start packing with input array
        packer.next() // Start a new packer bin
        packer.addArray(input.drop(2)) // Adding to the new bin
        for (bin in packer.bins) {
            println("----")
            println(bin.rects.joinToString("\n"))
        }

        // Reuse packer
        val bins = packer.save()
        packer.load(bins)
        packer.addArray(input)
    }
}
