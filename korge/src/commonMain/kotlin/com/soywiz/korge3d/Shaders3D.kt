package com.soywiz.korge3d

import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korge.internal.*

@Korge3DExperimental
open class Shaders3D {

	//@ThreadLocal
	private val programCache = LinkedHashMap<String, Program>()

	var printShaders = false

	@Suppress("RemoveCurlyBracesFromTemplate")
	fun getProgram3D(nlights: Int, nweights: Int, meshMaterial: Material3D?, hasTexture: Boolean): Program {
		return programCache.getOrPut("program_L${nlights}_W${nweights}_M${meshMaterial?.kind}_T${hasTexture}") {
			StandardShader3D(nlights, nweights, meshMaterial, hasTexture).program.apply {
				if (printShaders) {
					println(GlslGenerator(kind = ShaderType.VERTEX).generate(this.vertex.stm))
					println(GlslGenerator(kind = ShaderType.FRAGMENT).generate(this.fragment.stm))
				}
			}
		}
	}


	companion object {
		val u_Shininess = Uniform("u_shininess", VarType.Float1)
		val u_IndexOfRefraction = Uniform("u_indexOfRefraction", VarType.Float1)
		val u_AmbientColor = Uniform("u_ambientColor", VarType.Float4)
		val u_ProjMat = Uniform("u_ProjMat", VarType.Mat4)
		val u_ViewMat = Uniform("u_ViewMat", VarType.Mat4)
		val u_BindShapeMatrix = Uniform("u_BindShapeMat", VarType.Mat4)
		val u_BindShapeMatrixInv = Uniform("u_BindShapeMatInv", VarType.Mat4)
		val u_ModMat = Uniform("u_ModMat", VarType.Mat4)
		val u_NormMat = Uniform("u_NormMat", VarType.Mat4)
		//val MAX_BONE_MATS = 16
		val MAX_BONE_MATS = 64
		val u_BoneMats = Uniform("u_BoneMats", VarType.Mat4, arrayCount = MAX_BONE_MATS)
		val u_TexUnit = Uniform("u_TexUnit", VarType.TextureUnit)
		val a_pos = Attribute("a_Pos", VarType.Float3, normalized = false, precision = Precision.HIGH)
		val a_norm = Attribute("a_Norm", VarType.Float3, normalized = false, precision = Precision.HIGH)
		val a_tex = Attribute("a_TexCoords", VarType.Float2, normalized = false, precision = Precision.MEDIUM)
		val a_boneIndex = Array(4) { Attribute("a_BoneIndex$it", VarType.Float4, normalized = false) }
		val a_weight = Array(4) { Attribute("a_Weight$it", VarType.Float4, normalized = false) }
		val a_col = Attribute("a_Col", VarType.Float3, normalized = true)
		val v_col = Varying("v_Col", VarType.Float3)

		val v_Pos = Varying("v_Pos", VarType.Float3, precision = Precision.HIGH)
		val v_Norm = Varying("v_Norm", VarType.Float3, precision = Precision.HIGH)
		val v_TexCoords = Varying("v_TexCoords", VarType.Float2, precision = Precision.MEDIUM)

		val v_Temp1 = Varying("v_Temp1", VarType.Float4)

		val programColor3D = Program(
			vertex = VertexShader {
				SET(v_col, a_col)
				SET(out, u_ProjMat * u_ModMat * u_ViewMat * vec4(a_pos, 1f.lit))
			},
			fragment = FragmentShader {
				SET(out, vec4(v_col, 1f.lit))
				//SET(out, vec4(1f.lit, 1f.lit, 1f.lit, 1f.lit))
			},
			name = "programColor3D"
		)

		val lights = (0 until 4).map { LightAttributes(it) }

		val emission = MaterialLightUniform("emission")
		val ambient = MaterialLightUniform("ambient")
		val diffuse = MaterialLightUniform("diffuse")
		val specular = MaterialLightUniform("specular")


		val layoutPosCol = VertexLayout(a_pos, a_col)

		private val FLOATS_PER_VERTEX = layoutPosCol.totalSize / Int.SIZE_BYTES /*Float.SIZE_BYTES is not defined*/
	}

	class LightAttributes(val id: Int) {
		val u_sourcePos = Uniform("light${id}_pos", VarType.Float3)
		val u_color = Uniform("light${id}_color", VarType.Float4)
		val u_attenuation = Uniform("light${id}_attenuation", VarType.Float3)
	}

	class MaterialLightUniform(val kind: String) {
		//val mat = Material3D
		val u_color = Uniform("u_${kind}_color", VarType.Float4)
		val u_texUnit = Uniform("u_${kind}_texUnit", VarType.TextureUnit)
	}
}

@Korge3DExperimental
data class StandardShader3D(
	override val nlights: Int,
	override val nweights: Int,
	override val meshMaterial: Material3D?,
	override val hasTexture: Boolean
) : AbstractStandardShader3D()

@Korge3DExperimental
abstract class AbstractStandardShader3D() : BaseShader3D() {
	abstract val nlights: Int
	abstract val nweights: Int
	abstract val meshMaterial: Material3D?
	abstract val hasTexture: Boolean

	override fun Program.Builder.vertex() = Shaders3D.run {
		val modelViewMat = createTemp(VarType.Mat4)
		val normalMat = createTemp(VarType.Mat4)

		val boneMatrix = createTemp(VarType.Mat4)

		val localPos = createTemp(VarType.Float4)
		val localNorm = createTemp(VarType.Float4)
		val skinPos = createTemp(VarType.Float4)

		if (nweights == 0) {
			//SET(boneMatrix, mat4Identity())
			SET(localPos, vec4(a_pos, 1f.lit))
			SET(localNorm, vec4(a_norm, 0f.lit))
		} else {
			SET(localPos, vec4(0f.lit))
			SET(localNorm, vec4(0f.lit))
			SET(skinPos, u_BindShapeMatrix * vec4(a_pos["xyz"], 1f.lit))
			for (wIndex in 0 until nweights) {
				IF(getBoneIndex(wIndex) ge 0.lit) {
					SET(boneMatrix, getBone(wIndex))
					SET(localPos, localPos + boneMatrix * vec4(skinPos["xyz"], 1f.lit) * getWeight(wIndex))
					SET(localNorm, localNorm + boneMatrix * vec4(a_norm, 0f.lit) * getWeight(wIndex))
				}
			}
			SET(localPos, u_BindShapeMatrixInv * localPos)
		}

		SET(modelViewMat, u_ModMat * u_ViewMat)
		SET(normalMat, u_NormMat)
		SET(v_Pos, vec3(modelViewMat * (vec4(localPos["xyz"], 1f.lit))))
		SET(v_Norm, vec3(normalMat * normalize(vec4(localNorm["xyz"], 1f.lit))))
		if (hasTexture) SET(v_TexCoords, a_tex["xy"])
		SET(out, u_ProjMat * vec4(v_Pos, 1f.lit))
	}

	override fun Program.Builder.fragment() {
		val meshMaterial = meshMaterial
		if (meshMaterial != null) {
			computeMaterialLightColor(out, Shaders3D.diffuse, meshMaterial.diffuse)
		} else {
			SET(out, vec4(.5f.lit, .5f.lit, .5f.lit, 1f.lit))
		}

		for (n in 0 until nlights) {
			addLight(Shaders3D.lights[n], out)
		}
		//SET(out, vec4(v_Temp1.x, v_Temp1.y, v_Temp1.z, 1f.lit))
	}

	open fun Program.Builder.computeMaterialLightColor(out: Operand, uniform: Shaders3D.MaterialLightUniform, light: Material3D.Light) {
		when (light) {
			is Material3D.LightColor -> {
				SET(out, uniform.u_color)
			}
			is Material3D.LightTexture -> {
				SET(out, vec4(texture2D(uniform.u_texUnit, Shaders3D.v_TexCoords["xy"])["rgb"], 1f.lit))
			}
			else -> error("Unsupported MateriaList: $light")
		}
	}

	fun Program.Builder.mat4Identity() = Program.Func("mat4",
		1f.lit, 0f.lit, 0f.lit, 0f.lit,
		0f.lit, 1f.lit, 0f.lit, 0f.lit,
		0f.lit, 0f.lit, 1f.lit, 0f.lit,
		0f.lit, 0f.lit, 0f.lit, 1f.lit
	)


	open fun Program.Builder.addLight(light: Shaders3D.LightAttributes, out: Operand) {
		val v = Shaders3D.v_Pos
		val N = Shaders3D.v_Norm

		val L = createTemp(VarType.Float3)
		val E = createTemp(VarType.Float3)
		val R = createTemp(VarType.Float3)

		val attenuation = createTemp(VarType.Float1)
		val dist = createTemp(VarType.Float1)
		val NdotL = createTemp(VarType.Float1)
		val lightDir = createTemp(VarType.Float3)

		SET(L, normalize(light.u_sourcePos["xyz"] - v))
		SET(E, normalize(-v)) // we are in Eye Coordinates, so EyePos is (0,0,0)
		SET(R, normalize(-reflect(L, N)))

		val constantAttenuation = light.u_attenuation.x
		val linearAttenuation = light.u_attenuation.y
		val quadraticAttenuation = light.u_attenuation.z
		SET(lightDir, light.u_sourcePos["xyz"] - Shaders3D.v_Pos)
		SET(dist, length(lightDir))
		//SET(dist, length(vec3(4f.lit, 1f.lit, 6f.lit) - vec3(0f.lit, 0f.lit, 0f.lit)))

		SET(attenuation, 1f.lit / (constantAttenuation + linearAttenuation * dist + quadraticAttenuation * dist * dist))
		//SET(attenuation, 1f.lit / (1f.lit + 0f.lit * dist + 0.00111109f.lit * dist * dist))
		//SET(attenuation, 0.9.lit)
		SET(NdotL, max(dot(normalize(N), normalize(lightDir)), 0f.lit))

		IF(NdotL ge 0f.lit) {
			SET(out["rgb"], out["rgb"] + (light.u_color["rgb"] * NdotL + Shaders3D.u_AmbientColor["rgb"]) * attenuation * Shaders3D.u_Shininess)
		}
		//SET(out["rgb"], out["rgb"] * attenuation)
		//SET(out["rgb"], out["rgb"] + clamp(light.diffuse * max(dot(N, L), 0f.lit), 0f.lit, 1f.lit)["rgb"])
		//SET(out["rgb"], out["rgb"] + clamp(light.specular * pow(max(dot(R, E), 0f.lit), 0.3f.lit * u_Shininess), 0f.lit, 1f.lit)["rgb"])
	}

	fun Program.Builder.getBoneIndex(index: Int): Operand = int(Shaders3D.a_boneIndex[index / 4][index % 4])
	fun Program.Builder.getWeight(index: Int): Operand = Shaders3D.a_weight[index / 4][index % 4]
	fun Program.Builder.getBone(index: Int): Operand = Shaders3D.u_BoneMats[getBoneIndex(index)]
}

@Korge3DExperimental
abstract class BaseShader3D {
	abstract fun Program.Builder.vertex()
	abstract fun Program.Builder.fragment()
	val program by lazy {
		Program(
			vertex = VertexShader { vertex() },
			fragment = FragmentShader { fragment() },
			name = this@BaseShader3D.toString()
		)
	}
}

private fun Program.Builder.transpose(a: Operand) = Program.Func("transpose", a)
private fun Program.Builder.inverse(a: Operand) = Program.Func("inverse", a)
private fun Program.Builder.int(a: Operand) = Program.Func("int", a)
private operator fun Operand.get(index: Operand) = Program.ArrayAccess(this, index)
