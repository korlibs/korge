package com.soywiz.korag

import com.soywiz.korag.annotation.KoragExperimental
import com.soywiz.korag.shader.*

fun ProgramWithDefault(
	vertex: VertexShader = DefaultShaders.VERTEX_DEFAULT,
	fragment: FragmentShader = DefaultShaders.FRAGMENT_SOLID_COLOR,
	name: String = "program"
): Program = Program(vertex, fragment, name)

interface IDefaultShaders {
    val u_Tex: Uniform get() = DefaultShaders.u_Tex
    val u_ProjMat: Uniform get() = DefaultShaders.u_ProjMat
    val u_ViewMat: Uniform get() = DefaultShaders.u_ViewMat
    val a_Pos: Attribute get() = DefaultShaders.a_Pos
    val a_Tex: Attribute get() = DefaultShaders.a_Tex
    val a_Col: Attribute get() = DefaultShaders.a_Col
    val v_Tex: Varying get() = DefaultShaders.v_Tex
    val v_Col: Varying get() = DefaultShaders.v_Col
    val t_Temp0: Temp get() = DefaultShaders.t_Temp0
    val t_Temp1: Temp get() = DefaultShaders.t_Temp1
    val t_TempMat2: Temp get() = DefaultShaders.t_TempMat2
    val textureUnit: AG.TextureUnit get() = DefaultShaders.textureUnit
}

object DefaultShaders {
    // from korge
	val u_Tex: Uniform = Uniform("u_Tex", VarType.TextureUnit)

	val u_ProjMat: Uniform = Uniform("u_ProjMat", VarType.Mat4)
	val u_ViewMat: Uniform = Uniform("u_ViewMat", VarType.Mat4)
	val a_Pos: Attribute = Attribute("a_Pos", VarType.Float2, normalized = false, precision = Precision.HIGH)
	val a_Tex: Attribute = Attribute("a_Tex", VarType.Float2, normalized = false, precision = Precision.MEDIUM)
	val a_Col: Attribute = Attribute("a_Col", VarType.Byte4, normalized = true, precision = Precision.LOW)
	val v_Tex: Varying = Varying("v_Tex", VarType.Float2, precision = Precision.MEDIUM)
	val v_Col: Varying = Varying("v_Col", VarType.Byte4)

	val t_Temp0: Temp = Temp(0, VarType.Float4)
	val t_Temp1: Temp = Temp(1, VarType.Float4)
    val t_TempMat2: Temp = Temp(2, VarType.Mat2)

	val textureUnit: AG.TextureUnit = AG.TextureUnit()

	val LAYOUT_DEFAULT: VertexLayout = VertexLayout(a_Pos, a_Tex, a_Col)

	val VERTEX_DEFAULT: VertexShader = VertexShader {
		SET(v_Tex, a_Tex)
		SET(v_Col, a_Col)
		SET(out, u_ProjMat * u_ViewMat * vec4(a_Pos, 0f.lit, 1f.lit))
	}

	val FRAGMENT_DEBUG: FragmentShader = FragmentShader {
		out set vec4(1f.lit, 1f.lit, 0f.lit, 1f.lit)
	}

	val FRAGMENT_SOLID_COLOR: FragmentShader = FragmentShader {
		out set v_Col
	}

	val PROGRAM_TINTED_TEXTURE: Program = Program(
		vertex = VERTEX_DEFAULT,
		fragment = FragmentShader {
			//t_Temp1 set texture2D(u_Tex, v_Tex["xy"])
			//t_Temp1["xyz"] set t_Temp1["xyz"] / t_Temp1["w"]
			//out set (t_Temp1 * v_Col)
			//out set (texture2D(u_Tex, v_Tex["xy"])["bgra"] * v_Col)
			SET(out, texture2D(u_Tex, v_Tex["xy"])["rgba"] * v_Col)
		},
		name = "PROGRAM_TINTED_TEXTURE"
	)

	val PROGRAM_TINTED_TEXTURE_PREMULT: Program = Program(
		vertex = VERTEX_DEFAULT,
		fragment = FragmentShader {
			//t_Temp1 set texture2D(u_Tex, v_Tex["xy"])
			//t_Temp1["xyz"] set t_Temp1["xyz"] / t_Temp1["w"]
			//out set (t_Temp1 * v_Col)
			//out set (texture2D(u_Tex, v_Tex["xy"])["bgra"] * v_Col)
			SET(t_Temp0, texture2D(u_Tex, v_Tex["xy"]))
			SET(t_Temp0["rgb"], t_Temp0["rgb"] / t_Temp0["a"])
			SET(out, t_Temp0["rgba"] * v_Col)
		},
		name = "PROGRAM_TINTED_TEXTURE"
	)

	val PROGRAM_SOLID_COLOR: Program = Program(
		vertex = VERTEX_DEFAULT,
		fragment = FRAGMENT_SOLID_COLOR,
		name = "PROGRAM_SOLID_COLOR"
	)

	val LAYOUT_DEBUG: VertexLayout = VertexLayout(a_Pos)

	val PROGRAM_DEBUG: Program = Program(
		vertex = VertexShader {
			SET(out, vec4(a_Pos, 0f.lit, 1f.lit))
		},
		fragment = FragmentShader {
			out set vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit)
		},
		name = "PROGRAM_DEBUG"
	)

	val PROGRAM_DEBUG_WITH_PROJ: Program = Program(
		vertex = VertexShader {
			SET(out, u_ProjMat * vec4(a_Pos, 0f.lit, 1f.lit))
		},
		fragment = FragmentShader {
			SET(out, vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit))
		},
		name = "PROGRAM_DEBUG_WITH_PROJ"
	)

	val PROGRAM_DEFAULT: Program get() = PROGRAM_TINTED_TEXTURE_PREMULT

	inline operator fun invoke(callback: DefaultShaders.() -> Unit): DefaultShaders = this.apply(callback)
}

class ProgramBuilderDefault : Program.Builder(), IDefaultShaders

inline fun VertexShaderDefault(callback: ProgramBuilderDefault.() -> Unit): VertexShader =
    VertexShader(ProgramBuilderDefault().also(callback)._buildFuncs())

inline fun FragmentShaderDefault(callback: ProgramBuilderDefault.() -> Unit): FragmentShader =
    FragmentShader(ProgramBuilderDefault().also(callback)._buildFuncs())
