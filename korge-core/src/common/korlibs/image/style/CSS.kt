@file:OptIn(KorimExperimental::class)

package korlibs.image.style

import korlibs.datastructure.*
import korlibs.image.annotation.*
import korlibs.image.color.*
import korlibs.io.lang.*
import korlibs.io.util.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.time.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.native.concurrent.*

@KorimExperimental
class CSS(val allRules: List<IRuleSet>, unit: Unit = Unit) {
    constructor(cssList: List<CSS>) : this(cssList.flatMap { it.allRules })

    val animationsById = allRules.filterIsInstance<KeyFrames>().associateBy { it.id.str }
    val rules = allRules.filterIsInstance<RuleSet>()
    val rulesForIds = rules.filter { it.selector is IdSelector }.associateBy { (it.selector as IdSelector).id }
    val rulesForClassNames = rules.filter { it.selector is ClassSelector }.associateBy { (it.selector as ClassSelector).className }

    override fun toString(): String = "CSS(rules = ${rules.size}, animations = ${animationsById.size})"

    interface Selector {
        val str: String
    }
    data class IdSelector(val token: Token) : Selector {
        override val str: String get() = token.str
        val id: String = str.substr(1)
    }
    data class ClassSelector(val token: Token) : Selector {
        override val str: String get() = token.str
        val className: String = str.substr(1)
    }
    data class UnknownSelector(val token: Token) : Selector {
        override val str: String get() = token.str
    }
    data class Expression(val expr: List<Token>) : Extra by Extra.Mixin() {
        val exprStr = this.expr.joinToString(" ") { it.str }
    }

    data class Declaration(val property: String, val expr: Expression?) {
    }
    interface IRuleSet

    data class Declarations(val declarations: List<Declaration>) : Extra by Extra.Mixin() {
        val declarationsMap = declarations.associate { it.property.lowercase() to it.expr }
        operator fun get(prop: String): Expression? = declarationsMap[prop]
    }

    data class RuleSet(val selector: Selector, val declarations: Declarations) : IRuleSet {
    }

    data class KeyFrame constructor(val ratio: Ratio, val declarations: Declarations) {
        operator fun get(key: String): Expression? = declarations.declarationsMap[key]
        val easing: Easing = this["animation-timing-function"]?.easing ?: Easing.LINEAR
        companion object {
            val DUMMY = KeyFrame(Ratio.ZERO, Declarations(emptyList()))
        }
    }

    data class InterpolationResult(var ratio: Double = 0.0, var k0: KeyFrame = KeyFrame.DUMMY, var k1: KeyFrame = KeyFrame.DUMMY) {
        val properties get() = k0.declarations.declarations.map { it.property }
    }

    data class Animation(val name: String, val duration: TimeSpan, val delay: TimeSpan = 0.seconds, val iterationCount: NumberOfTimes = NumberOfTimes.INFINITE, val direction: Boolean = true, val easing: Easing = Easing.LINEAR)

    data class KeyFrames(val id: Token, val partialKeyFrames: List<KeyFrame>) : IRuleSet {
        val propertyNames = partialKeyFrames.flatMap { it.declarations.declarations.map { it.property } }.distinct()
        val fullKeyFrames = FastArrayList<KeyFrame>()
        init {
            @Suppress("USELESS_CAST") val currentMap = propertyNames.associateWith { null as Expression? }.toMutableMap()
            for (frame in partialKeyFrames) {
                for ((key, value) in frame.declarations.declarationsMap) {
                    currentMap[key] = value
                }
                fullKeyFrames.add(KeyFrame(frame.ratio, Declarations(currentMap.map { Declaration(it.key, it.value) })))
            }
        }

        fun getAt(ratio: Float, out: InterpolationResult = InterpolationResult()): InterpolationResult = getAt(ratio.toRatio(), out)
        fun getAt(ratio: Double, out: InterpolationResult = InterpolationResult()): InterpolationResult = getAt(ratio.toRatio(), out)

        // @TODO: Optimize: bisect
        fun getAt(ratio: Ratio, out: InterpolationResult = InterpolationResult()): InterpolationResult {
            val firstFrameIndex = fullKeyFrames.indexOfFirst { it.ratio >= ratio }
            if (firstFrameIndex >= 0) {
                val firstFrame = fullKeyFrames[firstFrameIndex]
                out.k0 = when (firstFrame.ratio) {
                    ratio -> firstFrame
                    else -> fullKeyFrames.getOrNull(firstFrameIndex - 1) ?: firstFrame
                }
                out.k1 = firstFrame
                out.ratio = when {
                    out.k0.ratio == out.k1.ratio -> 0.0
                    else -> {
                        val easing = out.k0.easing
                        //val easing = Easing.LINEAR
                        easing(ratio.convertRange(out.k0.ratio, out.k1.ratio, Ratio.ZERO, Ratio.ONE).toDouble())
                    }
                }
            } else {
                out.k0 = KeyFrame.DUMMY
                out.k1 = KeyFrame.DUMMY
                out.ratio = 0.0
            }
            return out
        }

        companion object {
            fun selectorToRatio(selector: Selector): Ratio {
                val str = selector.str
                if (str == "from") return Ratio.ZERO
                if (str == "to") return Ratio.ONE
                if (!str.endsWith("%")) error("Invalid keyframe selector $selector")
                return CSS.parseRatio(str)
            }

            fun ruleSetToKeyFrame(rules: List<IRuleSet>): List<KeyFrame> {
                return rules.filterIsInstance<RuleSet>().map { KeyFrame(selectorToRatio(it.selector), it.declarations) }
            }
        }
    }

    companion object {
        fun parseCSS(str: String): CSS {
            return CSSReader(tokenize(str.reader()).reader()).parseCSS()
        }

        data class Token(val str: String)

        fun tokenize(str: String): List<Token> = tokenize(str.reader())
        fun tokenize(ss: StrReader): List<Token> {
            val out = arrayListOf<Token>()
            val buffer = StringBuilder()
            fun flush() {
                if (buffer.isEmpty()) return
                out.add(Token(buffer.toString()))
                buffer.clear()
            }
            while (ss.hasMore) {
                val c = ss.read()
                when (c) {
                    ' ', '\t', '\n', '\r' -> {
                        flush()
                    }
                    '/' -> {
                        // Comment
                        if (ss.peekChar() == '*') {
                            flush()
                            ss.skip()
                            ss.readUntil { ss.matchLit("*/") != null }
                            //ss.skip(2)
                        } else {
                            flush()
                            out.add(Token("$c"))
                        }
                    }
                    ',', ':', ';', '(', '[', ')', ']', '{', '}' -> {
                        flush()
                        out.add(Token("$c"))
                    }
                    '-' -> {
                        if (buffer.isNotEmpty() && buffer.toString().toDoubleOrNull() != null) {
                            flush()
                            buffer.append(c)
                        } else {
                            buffer.append(c)
                        }
                    }
                    else -> {
                        buffer.append(c)
                    }
                }
            }
            flush()
            return out
        }

        fun parseAnimation(str: List<Token>) {

        }

        fun parseNumberDropSuffix(str: String): Double? {
            return str.trimEnd { it != '.' && it !in '0'..'9'  }.toDoubleOrNull()
        }

        fun parseTime(str: String): TimeSpan? {
            if (str.endsWith("ms")) return str.removeSuffix("ms").toDoubleOrNull()?.milliseconds
            if (str.endsWith("s")) return str.removeSuffix("s").toDoubleOrNull()?.seconds
            return parseNumberDropSuffix(str)?.milliseconds
        }
        fun parseAngle(str: String): Angle? {
            if (str.endsWith("deg")) return str.removeSuffix("deg").toDoubleOrNull()?.degrees
            if (str.endsWith("rad")) return str.removeSuffix("rad").toDoubleOrNull()?.radians
            return parseNumberDropSuffix(str)?.degrees
        }

        fun parseUnit(str: String): Double? {
            // @TODO: We should propagate metrics to compute units like %, pt, em, vw, vh...
            if (str.endsWith("%")) return str.removeSuffix("%").toDoubleOrNull()?.div(100.0)
            if (str.endsWith("px")) return str.removeSuffix("px").toDoubleOrNull()
            if (str.endsWith("pt")) return str.removeSuffix("pt").toDoubleOrNull()
            return parseNumberDropSuffix(str)?.toDouble()
            //return str.toDoubleOrNull()
        }

        fun readParenthesisValuesIgnoringCommas(tr: ListReader<String>): List<String> {
            val args = arrayListOf<String>()
            tr.expect("(")
            while (true) {
                if (tr.peek() == ")") {
                    tr.read()
                    break
                }
                if (tr.peek() == ",") {
                    tr.read()
                    continue
                }
                args += tr.read()
            }
            return args
        }

        fun parseTransform(str: String): Matrix {
            val tokens = tokenize(str).map { it.str.lowercase() }
            val tr = ListReader(tokens)
            var out = Matrix()
            //println("Not implemented: parseTransform: $str: $tokens")
            while (tr.hasMore) {
                val id = tr.read().lowercase()
                val args = arrayListOf<String>()
                if (tr.peek() == "(") {
                    args += readParenthesisValuesIgnoringCommas(tr)
                }
                val doubleArgs = args.map { parseUnit(it) ?: 0.0 }
                fun double(index: Int) = doubleArgs.getOrElse(index) { 0.0 }
                when (id) {
                    "translate3d" -> {
                        println("Warning. CSS. translate3d not implemented")
                        //out.pretranslate(double(0), double(1), double(2))
                        out = out.pretranslated(double(0), double(1))
                    }
                    "translatey" -> out = out.pretranslated(0.0, double(0))
                    "translatex" -> out = out.pretranslated(double(0), 0.0)
                    "translatez" -> {
                        println("Warning. CSS. translatez not implemented")
                        out = out.pretranslated(0.0, 0.0)
                    }
                    "translate" -> out = out.pretranslated(double(0), double(1))
                    "scale" -> out = out.prescaled(double(0), if (doubleArgs.size >= 2) double(1) else double(0))
                    "matrix" -> out = out.premultiplied(Matrix(double(0), double(1), double(2), double(3), double(4), double(5)))
                    "rotate" -> {
                        if (doubleArgs.size >= 3) out = out.pretranslated(double(1), double(2))
                        val angle = parseAngle(args[0]) ?: 0.degrees
                        //println("angle=$angle, double=${double(0)}")
                        out = out.prerotated(angle)
                        if (doubleArgs.size >= 3) out = out.pretranslated(-double(1), -double(2))
                    }
                    else -> invalidOp("Unsupported transform $id : $args : $doubleArgs ($str)")
                }
                //println("ID: $id, args=$args")
            }
            return out
        }

        fun parseRatio(str: String, default: Ratio = Ratio.ZERO): Ratio {
            if (str.endsWith("%")) return Ratio((parseNumberDropSuffix(str) ?: 0.0) / 100.0)
            return when (str.lowercase()) {
                "from" -> Ratio.ZERO
                "to" -> Ratio.ONE
                else -> Ratio(parseNumberDropSuffix(str) ?: default.toDouble())
            }
        }

        fun parseColor(str: String): RGBA {
            return Colors[str, Colors.FUCHSIA]
        }

        fun parseEasing(str: String): Easing {
            fun parseEasing(tr: ListReader<String>): Easing {
                return when (tr.read()) {
                    "ease" -> Easing.cubic(0.25, 0.1, 0.25, 1.0)
                    "linear" -> Easing.cubic(0.0, 0.0, 1.0, 1.0)
                    "ease-in" -> Easing.cubic(0.42, 0.0, 1.0, 1.0)
                    "ease-out" -> Easing.cubic(0.0, 0.0, 0.58, 1.0)
                    "ease-in-out" -> Easing.cubic(0.42, 0.0, 0.58, 1.0)
                    "cubic-bezier" -> {
                        val doubleArgs = readParenthesisValuesIgnoringCommas(tr).map { parseUnit(it) ?: 0.0 }
                        Easing.cubic(doubleArgs[0], doubleArgs[1], doubleArgs[2], doubleArgs[3])
                    }
                    "jump-start", "start" -> Easing.EASE_CLAMP_START
                    "jump-end", "end" -> Easing.EASE_CLAMP_END
                    "step-start" -> Easing { 1f } //Easing.steps(1, Easing.EASE_CLAMP_START)
                    "step-end" -> Easing { 0f } //Easing.steps(1, Easing.EASE_CLAMP_END)
                    else -> TODO("${tr.peek(-1)}")
                }
            }
            return parseEasing(tokenize(str).map { it.str.lowercase() }.reader())
       }

        fun parseSizeAsDouble(size: String): Double {
            return size.filter { it !in 'a'..'z' && it !in 'A'..'Z' }.toDoubleOrNull() ?: 16.0
        }

        fun parseAnimation(str: String): CSS.Animation {
            val tokens = tokenize(str).reader()
            val id = tokens.read().str
            val duration = parseTime(tokens.read().str) ?: 1.seconds
            return Animation(id, duration)
        }
    }
}


class CSSReader(val tokens: ListReader<CSS.Companion.Token>) {
    fun parseCSS(): CSS {
        return CSS(parseRulesets())
    }

    fun parseRulesets(): List<CSS.IRuleSet> {
        val out = arrayListOf<CSS.IRuleSet>()
        while (tokens.hasMore) {
            out += parseRuleset()
        }
        return out
    }

    fun selectorFromToken(token: CSS.Companion.Token): CSS.Selector {
        if (token.str.startsWith("#")) return CSS.IdSelector(token)
        if (token.str.startsWith(".")) return CSS.ClassSelector(token)
        return CSS.UnknownSelector(token)
    }

    fun parseRuleset(): List<CSS.IRuleSet> {
        val token = tokens.peek()
        when {
            token.str.startsWith("@") -> {
                tokens.read()
                // at rule
                when (token.str) {
                    "@keyframes", "@-webkit-keyframes" -> {
                        val keyframesName = tokens.read()
                        val sets = arrayListOf<CSS.KeyFrame>()
                        tokens.expect(CSS.Companion.Token("{"))
                        while (tokens.peek() != CSS.Companion.Token("}")) {
                            val frame = CSS.KeyFrames.ruleSetToKeyFrame(parseRuleset())
                            if (frame != null) sets += frame
                        }
                        tokens.expect(CSS.Companion.Token("}"))

                        return listOf(CSS.KeyFrames(keyframesName, sets.sortedBy { it.ratio }))
                    }
                    else -> {
                        TODO("Unknown at rule: '${token}'")
                    }
                }
            }
            // Plain selector
            else -> {
                val selectors = arrayListOf<CSS.Selector>()
                while (tokens.hasMore) {
                    val token = tokens.read()
                    selectors += selectorFromToken(token)
                    if (tokens.peek().str == ",") {
                        tokens.skip()
                        continue
                    }
                    if (tokens.peek().str == "{") {
                        break
                    }
                }
                val decls = arrayListOf<CSS.Declaration>()
                tokens.expect(CSS.Companion.Token("{"))
                while (tokens.peek() != CSS.Companion.Token("}")) {
                    decls += parseDeclaration()
                }
                tokens.expect(CSS.Companion.Token("}"))
                val declsd = CSS.Declarations(decls)
                return selectors.map { CSS.RuleSet(it, declsd) }
            }
        }
    }

    fun parseDeclaration(): CSS.Declaration {
        val property = tokens.read().str.removePrefix("-webkit-")
        tokens.expect(CSS.Companion.Token(":"))
        val expression = parseExpression()
        return CSS.Declaration(property, expression)
    }

    fun parseExpression(): CSS.Expression {
        val out = arrayListOf<CSS.Companion.Token>()
        while (true) {
            val token = tokens.read()
            if (token.str == ";") {
                break
            }
            if (token.str == "}") {
                tokens.skip(-1)
                break
            }
            out.add(token)
        }
        return CSS.Expression(out)
    }
}

val CSS.Expression.color: RGBA by extraPropertyThis { CSS.parseColor(exprStr) }
val CSS.Expression.ratio: Ratio by extraPropertyThis { CSS.parseRatio(exprStr) }
val CSS.Expression.matrix: Matrix by extraPropertyThis { CSS.parseTransform(exprStr) }
val CSS.Expression.transform: MatrixTransform by extraPropertyThis { matrix.immutable.decompose() }
val CSS.Expression.easing: Easing by extraPropertyThis { CSS.parseEasing(exprStr) }
val CSS.Declarations.animation: CSS.Animation? by extraPropertyThis {
    this["animation"]?.let { CSS.parseAnimation(it.exprStr) }
}

fun CSS.InterpolationResult.getColor(key: String, default: RGBA = Colors.TRANSPARENT): RGBA =
    this.ratio.toRatio().interpolate(k0[key]?.color ?: default, k1[key]?.color ?: default)
fun CSS.InterpolationResult.getRatio(key: String, default: Ratio = Ratio.ZERO): Ratio =
    this.ratio.toRatio().interpolate(k0[key]?.ratio ?: default, k1[key]?.ratio ?: default)
fun CSS.InterpolationResult.getMatrix(key: String, default: Matrix = Matrix()): Matrix =
    this.ratio.toRatio().interpolate(k0[key]?.matrix?.takeIf { it.isNotNIL } ?: default, k1[key]?.matrix ?: default)
fun CSS.InterpolationResult.getTransform(key: String, default: MatrixTransform = MatrixTransform.IDENTITY): MatrixTransform =
    this.ratio.toRatio().interpolate(k0[key]?.transform ?: default, k1[key]?.transform ?: default)
fun CSS.InterpolationResult.getEasing(key: String, default: Easing = Easing.LINEAR): Easing =
    k0[key]?.easing ?: default
