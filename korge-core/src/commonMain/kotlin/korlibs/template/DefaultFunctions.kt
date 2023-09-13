package korlibs.template

import korlibs.template.internal.umod

@Suppress("unused")
object DefaultFunctions {
    val Cycle = TeFunction("cycle") { args ->
        val list = args.getOrNull(0).toDynamicList()
        val index = args.getOrNull(1).toDynamicInt()
        list[index umod list.size]
    }

    val Range = TeFunction("range") { args ->
        val left = args.getOrNull(0)
        val right = args.getOrNull(1)
        val step = (args.getOrNull(2) ?: 1).toDynamicInt()
        if (left is Number || right is Number) {
            val l = left.toDynamicInt()
            val r = right.toDynamicInt()
            ((l..r) step step).toList()
        } else {
            TODO("Unsupported '$left'/'$right' for ranges")
        }
    }


    val Parent = TeFunction("parent") {
        //ctx.tempDropTemplate {
        val blockName = currentBlock?.name

        if (blockName != null) {
            captureRaw {
                currentBlock?.parent?.eval(this)
            }
        } else {
            ""
        }
    }

    val ALL = listOf(Cycle, Range, Parent)
}
