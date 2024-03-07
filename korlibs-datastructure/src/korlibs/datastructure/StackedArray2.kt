package korlibs.datastructure

interface IStackedArray2<T> : IStackedArray2Base

interface IStackedArray2Base {
    /** Version of the data. Each change increments this. */
    val contentVersion: Int

    /** Annotation of where in [startX] this stack would be placed in a bigger container, not used for set or get methods */
    val startX: Int
    /** Annotation of where in [startY] this stack would be placed in a bigger container, not used for set or get methods */
    val startY: Int

    /** [width] of the data available here, get and set methods use values in the range x=0 until [width] */
    val width: Int
    /** [height] of the data available here, get and set methods use values in the range y=0 until [height] */
    val height: Int

    /** The maximum level of layers available on the whole stack */
    val maxLevel: Int

    /** Shortcut for [IStackedArray2Base.startX] + [IStackedArray2Base.width] */
    val endX: Int get() = startX + width
    /** Shortcut for [IStackedArray2Base.startY] + [IStackedArray2Base.height] */
    val endY: Int get() = startY + height

    /** Number of values available at this [x], [y] */
    fun getStackLevel(x: Int, y: Int): Int

    /** Removes the last value at [x], [y] */
    fun removeLast(x: Int, y: Int)

    /** Checks if [x] and [y] are inside this array in the range x=0 until [width] and y=0 until [height] ignoring startX and startY */
    fun inside(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < width && y < height

    ///** Duplicates the contents of this [IStackedArray2] keeping its contents data */
    //fun clone(): IStackedArray2<T>
    ///** The [emptyGeneric] value that will be returned if the specified cell it out of bounds, or empty */
    //val emptyGeneric: T
    ///** Sets the [value] at [x], [y] at [level], [startX] and [startY] are NOT used here so 0,0 means the top-left element */
    //fun setGeneric(x: Int, y: Int, level: Int, value: T)
    ///** Gets the value at [x], [y] at [level], [startX] and [startY] are NOT used here so 0,0 means the top-left element */
    //fun getGeneric(x: Int, y: Int, level: Int): T

    fun eachPosition(block: (x: Int, y: Int) -> Unit) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                block(x, y)
            }
        }
    }
}
