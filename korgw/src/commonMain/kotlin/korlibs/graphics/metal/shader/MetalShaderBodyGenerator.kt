package korlibs.graphics.metal.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.io.util.*

internal class MetalShaderBodyGenerator(
    val kind: ShaderType? = null
) : Program.Visitor<String>(""), BaseMetalShaderGenerator {
    val temps = LinkedHashSet<Temp>()
    val programIndenter = Indenter()

    override fun visit(stms: Program.Stm.Stms) {
        for (stm in stms.stms) {
            visit(stm)
        }
    }

    override fun visit(stm: Program.Stm.Set) {
        programIndenter.line("${visit(stm.to)} = ${visit(stm.from)};")
    }

    override fun visit(stm: Program.Stm.Discard) {
        programIndenter.line("discard_fragment();")
    }

    override fun visit(stm: Program.Stm.Break) {
        programIndenter.line("break;")
    }

    override fun visit(stm: Program.Stm.Continue) {
        programIndenter.line("continue;")
    }

    override fun visit(stm: Program.Stm.Return) {
        val result = stm.result
        if (result != null) {
            programIndenter.line("return ${visit(result)};")
        } else {
            programIndenter.line("return;")
        }
    }

    override fun visit(operand: Program.Vector): String =
        typeToString(operand.type) + "(" + operand.ops.joinToString(", ") { visit(it) } + ")"

    override fun visit(operand: Program.Unop): String = "(" + operand.op + "(" + visit(operand.right) + ")" + ")"
    override fun visit(operand: Program.Binop): String =
        "(" + visit(operand.left) + " " + operand.op + " " + visit(operand.right) + ")"

    override fun visit(func: Program.BaseFunc): String =
        func.name + "(" + func.ops.joinToString(", ") { visit(it) } + ")"

    override fun visit(ternary: Program.Ternary): String =
        "((${visit(ternary.cond)}) ? (${visit(ternary.otrue)}) : (${visit(ternary.ofalse)}))"

    override fun visit(stm: Program.Stm.If) {
        programIndenter.apply {
            line("if (${visit(stm.cond)})") {
                visit(stm.tbody)
            }
            if (stm.fbody != null) {
                line("else") {
                    visit(stm.fbody!!)
                }
            }
        }
    }

    override fun visit(stm: Program.Stm.ForSimple) {
        programIndenter.apply {
            val varType = typeToString(stm.loopVar.type)
            val loopVar = visit(stm.loopVar)
            val min = visit(stm.min)
            val maxExclusive = visit(stm.maxExclusive)
            line("for ($varType $loopVar = ($min); $loopVar < ($maxExclusive); $loopVar++)") {
                visit(stm.body)
            }
        }
    }

    override fun visit(stm: Program.Stm.Raw) = TODO()

    override fun visit(operand: Variable): String {
        super.visit(operand)
        return when (operand) {
            is Output -> when (kind) {
                ShaderType.VERTEX -> "out.position"
                ShaderType.FRAGMENT -> "out"
                else -> error("unreachable statement")
            }

            else -> when (operand) {
                is Varying -> when (kind) {
                    ShaderType.VERTEX -> "out.${operand.name}"
                    else -> "in.${operand.name}"
                }

                else -> operand.name
            }
        }
    }

    override fun visit(temp: Temp): String {
        temps += temp
        return super.visit(temp)
    }

    override fun visit(operand: Program.IntLiteral): String = "${operand.value}"

    override fun visit(operand: Program.FloatLiteral): String {
        val str = "${operand.value}"
        return if (str.contains('.')) str else "$str.0"
    }

    override fun visit(operand: Program.BoolLiteral): String = "${operand.value}"
    override fun visit(operand: Program.Swizzle): String = visit(operand.left) + "." + operand.swizzle
    override fun visit(operand: Program.ArrayAccess): String = visit(operand.left) + "[" + visit(operand.index) + "]"
}
