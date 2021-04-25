package com.soywiz.korag

import com.soywiz.korag.shader.*

fun ProgramWithDefault(
	vertex: VertexShader = DefaultShaders.VERTEX_DEFAULT,
	fragment: FragmentShader = DefaultShaders.FRAGMENT_SOLID_COLOR,
	name: String = "program"
): Program = Program(vertex, fragment, name)

object DefaultShaders {
	val u_Tex = Uniform("u_Tex", VarType.TextureUnit)

	val u_ProjMat = Uniform("u_ProjMat", VarType.Mat4)
	val u_ViewMat = Uniform("u_ViewMat", VarType.Mat4)
	val a_Pos = Attribute("a_Pos", VarType.Float2, normalized = false, precision = Precision.HIGH)
	val a_Tex = Attribute("a_Tex", VarType.Float2, normalized = false, precision = Precision.MEDIUM)
	val a_Col = Attribute("a_Col", VarType.Byte4, normalized = true, precision = Precision.LOW)
	val v_Tex = Varying("v_Tex", VarType.Float2, precision = Precision.MEDIUM)
	val v_Col = Varying("v_Col", VarType.Byte4)

	val t_Temp0 = Temp(0, VarType.Float4)
	val t_Temp1 = Temp(1, VarType.Float4)
    val t_TempMat2 = Temp(2, VarType.Mat2)

	val textureUnit = AG.TextureUnit()

	val LAYOUT_DEFAULT = VertexLayout(a_Pos, a_Tex, a_Col)

	val VERTEX_DEFAULT = VertexShader {
		SET(v_Tex, a_Tex)
		SET(v_Col, a_Col)
		SET(out, u_ProjMat * u_ViewMat * vec4(a_Pos, 0f.lit, 1f.lit))
	}

	val FRAGMENT_DEBUG = FragmentShader {
		out set vec4(1f.lit, 1f.lit, 0f.lit, 1f.lit)
	}

	val FRAGMENT_SOLID_COLOR = FragmentShader {
		out set v_Col
	}

	val PROGRAM_TINTED_TEXTURE = Program(
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

	val PROGRAM_TINTED_TEXTURE_PREMULT = Program(
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

	val PROGRAM_SOLID_COLOR = Program(
		vertex = VERTEX_DEFAULT,
		fragment = FRAGMENT_SOLID_COLOR,
		name = "PROGRAM_SOLID_COLOR"
	)

	val LAYOUT_DEBUG = VertexLayout(a_Pos)

	val PROGRAM_DEBUG = Program(
		vertex = VertexShader {
			SET(out, vec4(a_Pos, 0f.lit, 1f.lit))
		},
		fragment = FragmentShader {
			out set vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit)
		},
		name = "PROGRAM_DEBUG"
	)

	val PROGRAM_DEBUG_WITH_PROJ = Program(
		vertex = VertexShader {
			SET(out, u_ProjMat * vec4(a_Pos, 0f.lit, 1f.lit))
		},
		fragment = FragmentShader {
			SET(out, vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit))
		},
		name = "PROGRAM_DEBUG_WITH_PROJ"
	)

	val PROGRAM_DEFAULT by lazy { PROGRAM_TINTED_TEXTURE_PREMULT }

	inline operator fun invoke(callback: DefaultShaders.() -> Unit) = this.apply(callback)
}
