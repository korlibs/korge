package korlibs.template

import korlibs.template.dynamic.Dynamic2
import korlibs.template.internal.Json
import korlibs.template.internal.format
import korlibs.template.internal.quote
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

//@Suppress("unused")
object DefaultFilters {
    val Capitalize = Filter("capitalize") { subject.toDynamicString().toLowerCase().capitalize() }
    val Join = Filter("join") {
        subject.toDynamicList().joinToString(args[0].toDynamicString()) { it.toDynamicString() }
    }
    val First = Filter("first") { subject.toDynamicList().firstOrNull() }
    val Last = Filter("last") { subject.toDynamicList().lastOrNull() }
    val Split = Filter("split") { subject.toDynamicString().split(args[0].toDynamicString())  }
    val Concat = Filter("concat") { subject.toDynamicString() + args[0].toDynamicString()  }
    val Length = Filter("length") { subject.dynamicLength() }
    val Quote = Filter("quote") { subject.toDynamicString().quote() }
    val Raw = Filter("raw") { RawString(subject.toDynamicString()) }
    val Replace = Filter("replace") { subject.toDynamicString().replace(args[0].toDynamicString(), args[1].toDynamicString()) }
    val Reverse =
        Filter("reverse") { (subject as? String)?.reversed() ?: subject.toDynamicList().reversed() }

    val Slice = Filter("slice") {
        val lengthArg = args.getOrNull(1)
        val start = args.getOrNull(0).toDynamicInt()
        val length = lengthArg?.toDynamicInt() ?: subject.dynamicLength()
        if (subject is String) {
            val str = subject.toDynamicString()
            str.slice(start.coerceIn(0, str.length) until (start + length).coerceIn(0, str.length))
        } else {
            val list = subject.toDynamicList()
            list.slice(start.coerceIn(0, list.size) until (start + length).coerceIn(0, list.size))
        }
    }

    val Sort = Filter("sort") {
        if (args.isEmpty()) {
            subject.toDynamicList().sortedBy { it.toDynamicString() }
        } else {
            subject.toDynamicList()
                .map { it to Dynamic2.accessAny(it, args[0], mapper).toDynamicString() }
                .sortedBy { it.second }
                .map { it.first }
        }
    }
    val Trim = Filter("trim") { subject.toDynamicString().trim() }

    val Lower = Filter("lower") { subject.toDynamicString().toLowerCase() }
    val Upper = Filter("upper") { subject.toDynamicString().toUpperCase() }
    val Downcase = Filter("downcase") { subject.toDynamicString().toLowerCase() }
    val Upcase = Filter("upcase") { subject.toDynamicString().toUpperCase() }

    val Merge = Filter("merge") {
        val arg = args.getOrNull(0)
        subject.toDynamicList() + arg.toDynamicList()
    }
    val JsonEncode = Filter("json_encode") {
        Json.stringify(subject)
    }
    val Format = Filter("format") {
        subject.toDynamicString().format(*(args.toTypedArray() as Array<out Any>))
    }
    // EXTRA from Kotlin
    val Chunked = Filter("chunked") {
        subject.toDynamicList().chunked(args[0].toDynamicInt())
    }
    val WhereExp = Filter("where_exp") {
        val ctx = this.context
        val list = this.subject.toDynamicList()
        val itemName = if (args.size >= 2) args[0].toDynamicString() else "it"
        val itemExprStr = args.last().toDynamicString()
        val itemExpr = ExprNode.parse(itemExprStr, FilePosContext(FileContext("", itemExprStr), 0))

        ctx.createScope {
            list.filter {
                ctx.scope.set(itemName, it)
                itemExpr.eval(ctx).toDynamicBool()
            }
        }
    }
    val Where = Filter("where") {
        val itemName = args[0]
        val itemValue = args[1]
        subject.toDynamicList().filter { Dynamic2.contains(Dynamic2.accessAny(it, itemName, mapper), itemValue) }

    }
    val Map = Filter("map") {
        val key = this.args[0].toDynamicString()
        this.subject.toDynamicList().map { Dynamic2.accessAny(it, key, mapper) }
    }
    val Size = Filter("size") { subject.dynamicLength() }
    val Uniq = Filter("uniq") {
        this.toDynamicList().distinct()
    }

    val Abs = Filter("abs") {
        val subject = subject
        when (subject) {
            is Int -> subject.absoluteValue
            is Double -> subject.absoluteValue
            is Long -> subject.absoluteValue
            else -> subject.toDynamicDouble().absoluteValue
        }
    }

    val AtMost = Filter("at_most") {
        val l = subject.toDynamicNumber()
        val r = args[0].toDynamicNumber()
        if (l >= r) r else l
    }

    val AtLeast = Filter("at_least") {
        val l = subject.toDynamicNumber()
        val r = args[0].toDynamicNumber()
        if (l <= r) r else l
    }

    val Ceil = Filter("ceil") {
        ceil(subject.toDynamicNumber().toDouble()).toDynamicCastToType(subject)
    }
    val Floor = Filter("floor") {
        floor(subject.toDynamicNumber().toDouble()).toDynamicCastToType(subject)
    }
    val Round = Filter("round") {
        round(subject.toDynamicNumber().toDouble()).toDynamicCastToType(subject)
    }
    val Times = Filter("times") {
        (subject.toDynamicDouble() * args[0].toDynamicDouble()).toDynamicCastToType(combineTypes(subject, args[0]))
    }
    val Modulo = Filter("modulo") {
        (subject.toDynamicDouble() % args[0].toDynamicDouble()).toDynamicCastToType(combineTypes(subject, args[0]))
    }
    val DividedBy = Filter("divided_by") {
        (subject.toDynamicDouble() / args[0].toDynamicDouble()).toDynamicCastToType(combineTypes(subject, args[0]))
    }
    val Minus = Filter("minus") {
        (subject.toDynamicDouble() - args[0].toDynamicDouble()).toDynamicCastToType(combineTypes(subject, args[0]))
    }
    val Plus = Filter("plus") {
        (subject.toDynamicDouble() + args[0].toDynamicDouble()).toDynamicCastToType(combineTypes(subject, args[0]))
    }
    val Default = Filter("default") {
        if (subject == null || subject == false || subject == "") args[0] else subject
    }

    val ALL = listOf(
        // String
        Capitalize, Lower, Upper, Downcase, Upcase, Quote, Raw, Replace, Trim,
        // Array
        Join, Split, Concat, WhereExp, Where, First, Last, Map, Size, Uniq, Length, Chunked, Sort, Merge,
        // Array/String
        Reverse,  Slice,
        // Math
        Abs, AtMost, AtLeast, Ceil, Floor, Round, Times, Modulo, DividedBy, Minus, Plus,
        // Any
        JsonEncode, Format
    )
}
