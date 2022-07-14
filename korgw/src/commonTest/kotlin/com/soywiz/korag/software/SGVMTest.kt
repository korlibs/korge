package com.soywiz.korag.software

import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.FragmentShaderDefault
import com.soywiz.korag.shader.Attribute
import com.soywiz.korag.shader.FragmentShader
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.VarType
import com.soywiz.korag.shader.Varying
import com.soywiz.korag.shader.VertexLayout
import com.soywiz.korag.shader.VertexShader
import kotlin.test.Test
import kotlin.test.assertEquals

class SGVMTest {
    @Test
    fun test() {
        val vm = SGVM(
            SGVMProgram {
                op(SGVMOpcode.FONE, 3, 0)
                op(SGVMOpcode.FADD, 3, 4, 0, 0)
                op(SGVMOpcode.END)
            }
        ).execute()

        /*
        run {
            val TOTAL_ITERS = 200_000_000
            val concurrency = CONCURRENCY_COUNT
            //val concurrency = 1
            val vms = (0 until concurrency).map { vm.clone() }
            val itersPerVm = TOTAL_ITERS / vms.size
            parallelForeach(vms.size) {
                val vm = vms[it]
                println(currentThreadId)
                for (n in 0 until itersPerVm) vm.execute()
            }
        }
        */

        assertEquals(1f, vm.freg[0])
        assertEquals(1f, vm.freg[1])
        assertEquals(1f, vm.freg[2])
        assertEquals(0f, vm.freg[3])
        assertEquals(2f, vm.freg[4])
        assertEquals(2f, vm.freg[5])
        assertEquals(2f, vm.freg[6])
        assertEquals(0f, vm.freg[7])
    }

    @Test
    fun test2() {
        val vm = executeShader {
            SET(out["x"], 2f.lit)
            SET(out["y"], 1f.lit)
            SET(out["z"], -(1f.lit))
            SET(out["w"], -(out["x"]))
        }
        assertEquals(2f, vm.freg[0])
        assertEquals(1f, vm.freg[1])
        assertEquals(-1f, vm.freg[2])
        assertEquals(-2f, vm.freg[3])
    }

    @Test
    fun test3() {
        val vm = executeShader {
            SET(out["xy"], vec2(1f.lit, 2f.lit))
        }
        assertEquals(1f, vm.freg[0])
        assertEquals(2f, vm.freg[1])
    }

    @Test
    fun test4() {
        val vm = executeShader {
            SET(out["zw"], vec2(1f.lit, 2f.lit))
            SET(out["xy"], out["zw"])
        }
        assertEquals(1f, vm.freg[0])
        assertEquals(2f, vm.freg[1])
    }

    @Test
    fun test5() {
        val vm = executeShader {
            //SET(out["zw"], vec2(1f.lit, 2f.lit))
            SET(out["x"], min(1f.lit, 2f.lit))
            SET(out["y"], max(1f.lit, 2f.lit))
            SET(out["wz"], abs(vec2((-4f).lit, (-3f).lit)))
        }
        assertEquals(1f, vm.freg[0])
        assertEquals(2f, vm.freg[1])
        assertEquals(3f, vm.freg[2])
        assertEquals(4f, vm.freg[3])
    }

    @Test
    fun test5b() {
        val vm = executeShader {
            SET(out["x"], 1f.lit)
            SET(out, out)
        }
        assertEquals(1f, vm.freg[0])
    }

    @Test
    fun test6() {
        executeShader {
            PUT(DefaultShaders.FRAGMENT_DEBUG)
        }
        executeShader {
            PUT(DefaultShaders.VERTEX_DEFAULT)
        }
    }

    @Test
    fun test7a() {
        executeShader {
            PUT(ShaderTest.PROGRAM_NOPRE.fragment)
        }
        executeShader {
            PUT(ShaderTest.PROGRAM_NOPRE.vertex)
        }
    }

    @Test
    fun test7b() {
        executeShader {
            PUT(ShaderTest.PROGRAM_NOPRE.fragment)
            //PUT(ShaderTest.PROGRAM_PRE.fragment)
        }
        executeShader {
            PUT(ShaderTest.PROGRAM_PRE.vertex)
        }
    }

    @Test
    fun test8() {
        val vm = executeShader {
            SET(out["wzyx"], vec4(1f.lit, 2f.lit, 3f.lit, 4f.lit)) // 4, 3, 2, 1
            SET(out["wzyx"], out["yywx"])
        }
        assertEquals(listOf(4f, 1f, 3f, 3f), vm.freg.slice(0 until 4).toList())
    }

    fun executeShader(callback: Program.Builder.() -> Unit): SGVM {
        val allocator = ShaderAllocator()
        val s2vm = ShaderToSGVM(allocator).handle(VertexShader(callback))
        println(s2vm.allocator.allocatedNames.keys)
        println(s2vm.allocator.allocatedNames)
        println(s2vm.allocator.currentIndex)
        val vm = s2vm.toProgram()
        return vm.execute()
    }
}

object ShaderTest {
    val a_ColMul = DefaultShaders.a_Col
    val a_ColAdd = Attribute("a_Col2", VarType.Byte4, normalized = true)

    val v_ColMul = DefaultShaders.v_Col
    val v_ColAdd = Varying("v_Col2", VarType.Byte4)

    val LAYOUT = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex, a_ColMul, a_ColAdd)
    val VERTEX = VertexShader {
        DefaultShaders.apply {
            SET(v_Tex, a_Tex)
            SET(v_ColMul, a_ColMul)
            SET(v_ColAdd, a_ColAdd)
            SET(out, (u_ProjMat * u_ViewMat) * vec4(DefaultShaders.a_Pos, 0f.lit, 1f.lit))
        }
    }

    val FRAGMENT_PRE = buildTextureLookupFragment(premultiplied = true)

    val FRAGMENT_NOPRE = buildTextureLookupFragment(premultiplied = false)

    val PROGRAM_PRE = Program(
        vertex = VERTEX,
        fragment = FRAGMENT_PRE,
        name = "BatchBuilder2D.Premultiplied.Tinted"
    )

    val PROGRAM_NOPRE = Program(
        vertex = VERTEX,
        fragment = FRAGMENT_NOPRE,
        name = "BatchBuilder2D.NoPremultiplied.Tinted"
    )

    fun getTextureLookupProgram(premultiplied: Boolean) = if (premultiplied) PROGRAM_PRE else PROGRAM_NOPRE

    //val PROGRAM_NORMAL = Program(
    //	vertex = VERTEX,
    //	fragment = FragmentShader {
    //		SET(out, texture2D(DefaultShaders.u_Tex, DefaultShaders.v_Tex["xy"])["rgba"] * v_Col2["rgba"])
    //		SET(out, out + v_Col2)
    //		// Required for shape masks:
    //		IF(out["a"] le 0f.lit) { DISCARD() }
    //	},
    //	name = "BatchBuilder2D.Tinted"
    //)

    fun getTextureLookupFragment(premultiplied: Boolean) = if (premultiplied) FRAGMENT_PRE else FRAGMENT_NOPRE

    /**
     * Builds a [FragmentShader] for textured and colored drawing that works matching if the texture is [premultiplied]
     */
    fun buildTextureLookupFragment(premultiplied: Boolean) = FragmentShaderDefault {
        SET(out, texture2D(u_Tex, v_Tex["xy"]))
        if (!premultiplied) {
            SET(out["rgb"], out["rgb"] * out["a"])
        }

        // @TODO: Kotlin.JS bug?
        //SET(out, (out["rgba"] * v_ColMul["rgba"]) + ((v_ColAdd["rgba"] - vec4(.5f, .5f, .5f, .5f)) * 2f))
        SET(out, (out["rgba"] * v_ColMul["rgba"]) + ((v_ColAdd["rgba"] - vec4(.5f.lit, .5f.lit, .5f.lit, .5f.lit)) * 2f.lit))

        //SET(out, t_Temp1)
        // Required for shape masks:
        IF(out["a"] le 0f.lit) { DISCARD() }
    }
}
