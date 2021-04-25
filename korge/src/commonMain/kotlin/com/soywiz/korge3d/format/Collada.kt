package com.soywiz.korge3d.format

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.internal.max2
import com.soywiz.korge3d.*
import com.soywiz.korge3d.animation.*
import com.soywiz.korge3d.internal.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.math.*
import kotlin.math.*

@Korge3DExperimental
suspend fun VfsFile.readColladaLibrary(loadTextures: Boolean = true): Library3D {
    return ColladaParser.parse(readXml()).also { if (loadTextures) it.loadTextures() }
        .also { it.instantiateMaterials() }
}

@Korge3DExperimental
class ColladaParser {
    interface SourceParam {
        val name: String
    }

    data class MatrixSourceParam(override val name: String, val matrices: Array<Matrix3D>) : SourceParam
    data class FloatSourceParam(override val name: String, val floats: FloatArrayList) : SourceParam
    data class NamesSourceParam(override val name: String, val names: ArrayList<String>) : SourceParam
    data class Source(val id: String, val params: FastStringMap<SourceParam>)
    data class Input(val semantic: String, val offset: Int, val source: Source, val indices: IntArrayList)
    data class Geometry(
        val id: String,
        val name: String,
        val inputs: FastStringMap<Input> = FastStringMap(),
        var materialId: String? = null
    )

    data class Skin(
        val controllerId: String,
        val controllerName: String,
        val inputs: FastStringMap<Input>,
        val vcounts: IntArrayList,
        val bindShapeMatrix: Matrix3D,
        val jointInputs: FastStringMap<Input>,
        val skinSource: String
    ) {
        val maxVcount = vcounts.map { it }.maxOrNull() ?: 0
        //fun toDef() = Library3D.SkinDef(bindShapeMatrix, skinSource, )
    }

    companion object {
        fun parse(xml: Xml): Library3D = ColladaParser().parse(xml)
    }

    fun parse(xml: Xml): Library3D = Library3D().apply {
        parseCameras(xml)
        parseLights(xml)
        parseImages(xml)
        parseEffects(xml)
        parseMaterials(xml)
        val geometries = parseGeometries(xml)
        parseAnimations(xml)
        val skins = parseControllers(xml)
        //for (skin in skins) skinDefs[skin.controllerId] = skin.toDef()
        generateGeometries(geometries, skins)
        parseVisualScenes(xml)
        parseScene(xml)
    }

    fun Library3D.generateGeometries(geometries: List<Geometry>, skins: List<Skin>) {
        val geomIdToSkin = FastStringMap<Skin>()

        for (skin in skins) {
            geomIdToSkin[skin.skinSource] = skin
        }

        for (geom in geometries) {
            val px = FloatArrayList()
            val py = FloatArrayList()
            val pz = FloatArrayList()

            val nx = FloatArrayList()
            val ny = FloatArrayList()
            val nz = FloatArrayList()

            val u0 = FloatArrayList()
            val v0 = FloatArrayList()

            val weightIndices = Array(16) { FloatArrayList() }
            val weightWeights = Array(16) { FloatArrayList() }

            val VERTEX = geom.inputs["VERTEX"] ?: run {
                println("WARNING.ColladaParser.Library3D.generateGeometries: Do not have vertices!")
                null
            } ?: return
            val VERTEX_indices = VERTEX.indices

            for (pname in listOf("X", "Y", "Z")) {
                val p = (VERTEX.source.params[pname] as? FloatSourceParam)?.floats
                val array = when (pname) {
                    "X" -> px
                    "Y" -> py
                    "Z" -> pz
                    else -> TODO()
                }
                if (p != null) {
                    //println(VERTEX.indices)
                    VERTEX.indices.fastForEach { index ->
                        array.add(p[index])
                    }
                }
            }

            val NORMAL = geom.inputs["NORMAL"]
            if (NORMAL != null) {
                for (pname in listOf("X", "Y", "Z")) {
                    val p = (NORMAL.source.params[pname] as? FloatSourceParam)?.floats
                    val array = when (pname) {
                        "X" -> nx
                        "Y" -> ny
                        "Z" -> nz
                        else -> TODO()
                    }
                    if (p != null) {
                        NORMAL.indices.fastForEach { index -> array.add(p[index]) }
                    }
                }
            }

            val TEXCOORD = geom.inputs["TEXCOORD"]
            if (TEXCOORD != null) {
                for (pname in listOf("S", "T")) {
                    val p = (TEXCOORD.source.params[pname] as? FloatSourceParam)?.floats
                    val array = when (pname) {
                        "S" -> u0
                        "T" -> v0
                        else -> TODO()
                    }
                    if (p != null) {
                        TEXCOORD.indices.fastForEach { index -> array.add(p[index]) }
                    }
                }
            }

            val skin = geomIdToSkin[geom.id]

            val maxWeights: Int

            if (skin != null) {
                val joint = skin.inputs["JOINT"] ?: error("Can't find JOINT")
                val weight = skin.inputs["WEIGHT"] ?: error("Can't find WEIGHT")
                maxWeights = skin.maxVcount.nextMultipleOf(4)
                var pos = 0

                //if (maxWeights > 4) error("Too much weights for the current implementation $maxWeights > 4")

                val jointSrcParam = joint.source.params["JOINT"] as NamesSourceParam
                val weightSrcParam = weight.source.params["WEIGHT"] as FloatSourceParam

                val jointsToIndex = FastStringMap<Int>()
                jointSrcParam.names.fastForEachWithIndex { index, value ->
                    jointsToIndex[value] = index
                }

                for (vcount in skin.vcounts) {
                    //println("-- vcount=$vcount")
                    var weightSum = 0.0
                    for (n in 0 until vcount) {
                        //joint.source = joint.indices[pos]
                        val jointIndex = joint.indices[pos]
                        //val jointtName = jointSrcParam.names[jointIndex]
                        val w = weightSrcParam.floats[weight.indices[pos]]
                        //println("$jointName[$joinIndex]: $weight")
                        weightIndices[n].add(jointIndex.toFloat())
                        weightWeights[n].add(w)
                        weightSum += w
                        pos++
                    }
                    for (n in vcount until maxWeights) {
                        //weightIndices[n].add(-1f)
                        weightIndices[n].add(0f)
                        weightWeights[n].add(0f)
                    }
                    if (abs(weightSum - 1.0) > 0.0001) {
                        println("Not normalized weights: weightSum=$weightSum, weightWeights=${weightWeights.toList()}")
                    }
                }
                //println("jointSrcParam: $jointSrcParam")
                //println("weightSrcParam: $weightSrcParam")
                //println("joint: $joint")
                //println("weight: $weight")
                //println("---")
            } else {
                maxWeights = 0
            }

            val skinDef = if (skin != null) {
                val JOINT = skin.jointInputs["JOINT"] ?: error("Can't find JOINT")
                val INV_BIND_MATRIX = skin.jointInputs["INV_BIND_MATRIX"] ?: error("Can't find INV_BIND_MATRIX")
                val JOINT_NAMES = (JOINT.source.params["JOINT"] as? NamesSourceParam)?.names
                    ?: error("Can't find JOINT.JOINT")
                val TRANSFORM = (INV_BIND_MATRIX.source.params["TRANSFORM"] as? MatrixSourceParam)?.matrices
                    ?: error("Can't find INV_BIND_MATRIX.TRANSFORM")
                val skin = Library3D.SkinDef(
                    skin.controllerId,
                    skin.controllerName,
                    skin.bindShapeMatrix,
                    skin.skinSource,
                    JOINT_NAMES.zip(TRANSFORM).withIndex().map {
                        Library3D.BoneDef(
                            it.index,
                            it.value.first,
                            it.value.second
                        )
                    })
                skin.bones.fastForEach { it.skin = skin }
                skinDefs[skin.controllerId] = skin
                skin
            } else {
                null
            }


            // @TODO: We should use separate components
            val combinedVertexData = floatArrayListOf()
            val combinedIndexData = ShortArrayList()

            val hasNormals = (nx.size >= px.size)
            val hasTexture = TEXCOORD != null
            for (n in 0 until px.size) {
                combinedVertexData.add(px[n])
                combinedVertexData.add(py[n])
                combinedVertexData.add(pz[n])
                if (hasNormals) {
                    combinedVertexData.add(nx[n])
                    combinedVertexData.add(ny[n])
                    combinedVertexData.add(nz[n])
                }
                if (hasTexture) {
                    combinedVertexData.add(u0[n])
                    combinedVertexData.add(1f - v0[n])
                }
                if (maxWeights > 0) {
                    for (m in 0 until maxWeights) {
                        combinedVertexData.add(weightIndices[m][VERTEX_indices[n]])
                    }
                }
                if (maxWeights > 0) {
                    for (m in 0 until maxWeights) {
                        combinedVertexData.add(weightWeights[m][VERTEX_indices[n]])
                    }
                }
                combinedIndexData.add(n.toShort())
            }

            //println(combinedData.toString())

            val materialDef = geom.materialId?.let { materialDefs[it] }

            geometryDefs[geom.id] = Library3D.GeometryDef(
                Mesh3D(
                    //combinedData.toFloatArray().toFBuffer(),
                    listOf(BufferWithVertexLayout(
                        buffer = combinedVertexData.toFBuffer(),
                        layout = VertexLayout(buildList {
                            add(Shaders3D.a_pos)
                            if (hasNormals) add(Shaders3D.a_norm)
                            if (hasTexture) add(Shaders3D.a_tex)
                            for (n in 0 until 4) if (maxWeights > n * 4) add(Shaders3D.a_boneIndex[n])
                            for (n in 0 until 4) if (maxWeights > n * 4) add(Shaders3D.a_weight[n])
                        }),
                    )),
                    combinedIndexData.toFBuffer(),
                    AG.IndexType.USHORT,
                    combinedIndexData.size,
                    null,
                    AG.DrawType.TRIANGLES,
                    hasTexture = hasTexture,
                    maxWeights = maxWeights
                ).apply {
                    if (skinDef != null) {
                        this.skin = skinDef.toSkin()
                    }
                },
                skin = skinDef,
                material = materialDef
            )
            log { "px: $px" }
            log { "py: $py" }
            log { "pz: $pz" }
            log { "nx: $nx" }
            log { "ny: $ny" }
            log { "nz: $nz" }
        }
    }

    fun Library3D.parseScene(xml: Xml) {
        val scene = xml["scene"]
        for (instance_visual_scene in scene["instance_visual_scene"]) {
            val id = instance_visual_scene.str("url").trim('#')
            val scene = scenes[id]
            if (scene != null) {
                mainScene.children += scene
            }
        }
    }

    fun parseControllers(xml: Xml): List<Skin> {
        val skins = arrayListOf<Skin>()
        for (controller in xml["library_controllers"]["controller"]) {
            val controllerId = controller.str("id")
            val controllerName = controller.str("name")
            val skin = controller["skin"].firstOrNull()
            if (skin != null) {
                val skinSource = skin.str("source")
                val bindShapeMatrix = skin["bind_shape_matrix"].firstOrNull()?.text?.reader()?.readMatrix3D()
                    ?: Matrix3D()
                for (source in parseSources(skin, this@ColladaParser.sourceArrayParams)) {
                    sources[source.id] = source
                }
                val jointsXml = skin["joints"].firstOrNull()
                val jointInputs = FastStringMap<Input>()
                if (jointsXml != null) {
                    for (input in jointsXml["input"]) {
                        val semantic = input.str("semantic")
                        val sourceId = input.str("source").trim('#')
                        val offset = input.int("offset")
                        val source = sources[sourceId] ?: continue
                        jointInputs[semantic] = Input(semantic, offset, source, intArrayListOf())
                    }
                }
                val vertexWeightsXml = skin["vertex_weights"].firstOrNull()
                val inputs = arrayListOf<Input>()
                if (vertexWeightsXml != null) {
                    val count = vertexWeightsXml.int("count")
                    val vcount = (vertexWeightsXml["vcount"].firstOrNull()?.text ?: "").reader().readInts()
                    val v = (vertexWeightsXml["v"].firstOrNull()?.text ?: "").reader().readInts()
                    for (input in vertexWeightsXml["input"]) {
                        val semantic = input.str("semantic")
                        val sourceId = input.str("source").trim('#')
                        val offset = input.int("offset")
                        val source = sources[sourceId] ?: continue
                        inputs += Input(semantic, offset, source, intArrayListOf())
                    }
                    val stride = (inputs.map { it.offset }.maxOrNull() ?: 0) + 1

                    for (i in inputs) {
                        for (n in 0 until v.size / stride) {
                            i.indices += v[n * stride + i.offset]
                        }
                    }

                    skins += Skin(
                        controllerId,
                        controllerName,
                        inputs.associate { it.semantic to it }.toMap().toFast(),
                        vcount,
                        bindShapeMatrix,
                        jointInputs,
                        skinSource.trim('#')
                    )
                }
            }
        }
        return skins
    }

    fun Library3D.parseMaterials(xml: Xml) {
        for (materialXml in xml["library_materials"]["material"]) {
            val materialId = materialXml.str("id")
            val materialName = materialXml.str("name")
            val effects = materialXml["instance_effect"].map { instance_effect ->
                effectDefs[instance_effect.str("url").trim('#')]
            }.filterNotNull()
            val material = Library3D.MaterialDef(materialId, materialName, effects)
            materialDefs[materialId] = material
        }
    }

    fun Library3D.parseLightKindType(
        xml: Xml?,
        lightKind: String,
        params: FastStringMap<Any>
    ): Library3D.LightKindDef? {
        if (xml == null) return null
        val nodeName = xml.nameLC
        val colorXml = xml["color"].firstOrNull()
        if (colorXml != null) {
            val sid = colorXml.str("sid")
            val f = colorXml.text.reader().readFloats()
            return Library3D.LightColorDef(sid, RGBA.float(f[0], f[1], f[2], f[3]), lightKind)
        }
        val textureXml = xml["texture"].firstOrNull()
        if (textureXml != null) {
            return Library3D.LightTexDef(
                nodeName,
                params[textureXml.str("texture")] as? Library3D.EffectParamSampler2D?,
                lightKind
            )
        }
        return null
    }

    fun Library3D.parseEffects(xml: Xml) {
        for (effectXml in xml["library_effects"]["effect"]) {
            val effectId = effectXml.str("id")
            val effectName = effectXml.str("name")
            val profile_COMMONXml = effectXml["profile_COMMON"].firstOrNull()
            if (profile_COMMONXml != null) {
                var emission: Library3D.LightKindDef? = null
                var ambient: Library3D.LightKindDef? = null
                var diffuse: Library3D.LightKindDef? = null
                var specular: Library3D.LightKindDef? = null
                var shininess: Float? = null
                var index_of_refraction: Float? = null
                val params = FastStringMap<Any>()

                for (nodeXml in profile_COMMONXml.allNodeChildren) {
                    when (nodeXml.nameLC) {
                        "newparam" -> {
                            val sid = nodeXml.str("sid")
                            val surfaceXml = nodeXml["surface"].firstOrNull()
                            if (surfaceXml != null) {
                                val surfaceType = surfaceXml.str("type")
                                val initFrom = surfaceXml["init_from"].text
                                val image = imageDefs[initFrom]
                                params[sid] = Library3D.EffectParamSurface(surfaceType, image)
                            }
                            val sampler2DSource = nodeXml["sampler2D"]["source"].firstText
                            if (sampler2DSource != null) {
                                params[sid] =
                                    Library3D.EffectParamSampler2D(params[sampler2DSource] as? Library3D.EffectParamSurface?)
                            }
                        }
                        "technique" -> {
                            val sid = nodeXml.str("sid")
                            for (tech in nodeXml.allNodeChildren) {
                                when (tech.nameLC) {
                                    "gouraud", "phong", "lambert" -> { // Smooth lighting
                                        emission =
                                            parseLightKindType(tech["emission"].firstOrNull(), tech.nameLC, params)
                                                ?: emission
                                        ambient = parseLightKindType(tech["ambient"].firstOrNull(), tech.nameLC, params)
                                            ?: ambient
                                        diffuse = parseLightKindType(tech["diffuse"].firstOrNull(), tech.nameLC, params)
                                            ?: diffuse
                                        specular =
                                            parseLightKindType(tech["specular"].firstOrNull(), tech.nameLC, params)
                                                ?: specular
                                        shininess = tech["shininess"]["float"].firstText?.toFloatOrNull()?.div(100f) ?: shininess
                                        index_of_refraction =
                                            tech["index_of_refraction"]["float"].firstText?.toFloatOrNull()
                                                ?: index_of_refraction
                                    }
                                    "extra" -> Unit
                                    else -> println("WARNING: Unsupported library_effects.effect.profile_COMMON.technique.${tech.nameLC}")
                                }
                            }
                        }
                        "extra" -> Unit
                        else -> {
                            println("WARNING: Unsupported library_effects.effect.profile_COMMON.${nodeXml.nameLC}")
                        }
                    }
                }

                effectDefs[effectId] = Library3D.StandardEffectDef(
                    effectId,
                    effectName,
                    emission,
                    ambient,
                    diffuse,
                    specular,
                    shininess,
                    index_of_refraction
                )
            }
        }
    }

    fun Library3D.parseImages(xml: Xml) {
        for (image in xml["library_images"]["image"]) {
            val imageId = image.str("id")
            val imageName = image.str("name")
            val initFrom = image["init_from"].text.trim()
            imageDefs[imageId] = Library3D.ImageDef(imageId, imageName, initFrom)
        }
    }

    fun Library3D.parseAnimations(xml: Xml) {
        val sources = FastStringMap<SourceParam>()
        parseAnimationNode(sources, xml["library_animations"]["animation"])
    }

    private fun Library3D.parseAnimationNode(sources: FastStringMap<SourceParam>, animationXmls: Iterable<Xml>) {
        for (animationXml in animationXmls) {
            val srcs = parseSources(animationXml, sources)
            val animationId = animationXml.str("id")
            val sourcesById = srcs.associateBy { it.id }
            val samplerXml = animationXml["sampler"].firstOrNull()
            val inputParams = FastStringMap<Source?>()
            if (samplerXml != null) {
                val samplerId = samplerXml.str("id")
                for (inputXml in samplerXml["input"]) {
                    val inputSemantic = inputXml.str("semantic")
                    val inputSourceId = inputXml.str("source").trim('#')
                    inputParams[inputSemantic] = sourcesById[inputSourceId]
                }
                //println("$samplerId -> $inputParams")
            }
            val channelXml = animationXml["channel"].firstOrNull()
            if (channelXml != null) {
                val channelSource = channelXml.str("source").trim('#')
                val channelTargetInfo = channelXml.str("target").split('/', limit = 2)
                val channelTarget = channelTargetInfo.getOrElse(0) { "" }
                val channelProp = channelTargetInfo.getOrElse(1) { "" }

                val times = inputParams.getFloats("INPUT", "TIME")
                    ?: error("Can't find INPUT.TIME for animationId=$animationId")
                val interpolations = inputParams.getStrings("INTERPOLATION", "INTERPOLATION")
                    ?: error("Can't find INTERPOLATION.INTERPOLATION for animationId=$animationId")
                //val transforms = inputParams.getMatrices("OUTPUT", "TRANSFORM")
                val outputSourceParam = inputParams["OUTPUT"]?.params?.values?.first()
                val matrices = (outputSourceParam as? MatrixSourceParam?)?.matrices
                val floats = (outputSourceParam as? FloatSourceParam?)?.floats?.toFloatArray()

                //println("$channelSource -> $channelTarget")
                val frames = Animation3D.Frames(
                    seconds = times,
                    interpolations = interpolations,
                    matrices = matrices,
                    floats = floats
                )
                animationDefs[animationId] = Animation3D(
                    animationId,
                    channelTarget, channelProp,
                    frames
                )
            }
            parseAnimationNode(sources, animationXml["animation"])
        }
    }

    fun FastStringMap<Source?>.getMatrices(a: String, b: String): Array<Matrix3D>? =
        (this[a]?.params?.get(b) as? MatrixSourceParam?)?.matrices

    fun FastStringMap<Source?>.getStrings(a: String, b: String): Array<String>? =
        (this[a]?.params?.get(b) as? NamesSourceParam?)?.names?.toTypedArray()

    fun FastStringMap<Source?>.getFloats(a: String, b: String): FloatArray? =
        (this[a]?.params?.get(b) as? FloatSourceParam?)?.floats?.toFloatArray()

    fun Library3D.parseLights(xml: Xml) {
        for (light in xml["library_lights"]["light"]) {
            var lightDef: Library3D.LightDef? = null
            val id = light.str("id")
            val name = light.str("name")
            log { "Light id=$id, name=$name" }
            for (technique in light["technique_common"].allNodeChildren) {
                when (technique.nameLC) {
                    "point" -> {
                        val color = technique["color"].firstOrNull()?.text?.reader()?.readVector3D()
                            ?: Vector3D(1, 1, 1)
                        val constant_attenuation =
                            technique["constant_attenuation"].firstOrNull()?.text?.toDoubleOrNull()
                                ?: 1.0
                        val linear_attenuation = technique["linear_attenuation"].firstOrNull()?.text?.toDoubleOrNull()
                            ?: 0.0
                        val quadratic_attenuation =
                            technique["quadratic_attenuation"].firstOrNull()?.text?.toDoubleOrNull()
                                ?: 0.00111109
                        lightDef = Library3D.PointLightDef(
                            RGBA.float(color.x, color.y, color.z, 1f),
                            constant_attenuation,
                            linear_attenuation,
                            quadratic_attenuation
                        )
                    }
                    "ambient" -> {
                        val color = technique["color"].firstOrNull()?.text?.reader()?.readVector3D()
                            ?: Vector3D(1, 1, 1)
                        lightDef = Library3D.AmbientLightDef(RGBA.float(color.x, color.y, color.z, 1f))
                    }
                    else -> {
                        println("WARNING: Unsupported light.technique_common.${technique.nameLC}")
                    }
                }
            }
            if (lightDef != null) {
                lightDefs[id] = lightDef
            }
        }
    }

    fun Library3D.parseCameras(xml: Xml) {
        for (camera in xml["library_cameras"]["camera"]) {
            val id = camera.getString("id") ?: "Unknown"
            val name = camera.getString("name") ?: "Unknown"
            var persp: Library3D.PerspectiveCameraDef? = null
            for (v in camera["optics"]["technique_common"].allChildren) {
                when (v.nameLC) {
                    "_text_" -> Unit
                    "perspective" -> {
                        val xfov = v["xfov"].firstOrNull()?.text?.toDoubleOrNull() ?: 45.0
                        val znear = v["znear"].firstOrNull()?.text?.toDoubleOrNull() ?: 0.01
                        val zfar = v["zfar"].firstOrNull()?.text?.toDoubleOrNull() ?: 100.0
                        persp = Library3D.PerspectiveCameraDef(xfov.degrees, znear, zfar)
                    }
                    else -> {
                        log { "Unsupported camera technique ${v.nameLC}" }
                    }
                }
            }

            cameraDefs[id] = persp ?: Library3D.CameraDef()
            log { "Camera id=$id, name=$name, persp=$persp" }
        }
    }

    fun Library3D.parseGeometries(xml: Xml): List<Geometry> {
        val geometries = arrayListOf<Geometry>()

        for (geometry in xml["library_geometries"]["geometry"]) {
            val id = geometry.getString("id") ?: "unknown"
            val name = geometry.getString("name") ?: "unknown"
            val geom = Geometry(id, name)
            geometries += geom
            log { "Geometry id=$id, name=$name" }
            for (mesh in geometry["mesh"]) {
                for (source in parseSources(mesh, this@ColladaParser.sourceArrayParams)) {
                    sources[source.id] = source
                }

                for (vertices in mesh["vertices"]) {
                    val verticesId = vertices.getString("id") ?: vertices.getString("name") ?: "unknown"
                    log { "vertices: $vertices" }
                    for (input in vertices["input"]) {
                        val semantic = input.str("semantic", "UNKNOWN")
                        val source = input.getString("source")?.trim('#') ?: "unknown"
                        val rsource = sources[source]
                        if (rsource != null) {
                            sources[verticesId] = rsource
                        }
                    }
                }

                log { "SOURCES.KEYS: " + sources.keys }
                log { "SOURCES: ${sources.keys.map { it to sources[it] }.toMap()}" }

                val triangles = mesh["triangles"]?.firstOrNull() ?: mesh["polylist"]?.firstOrNull()

                if (triangles != null) {
                    if (triangles.nameLC != "triangles") {
                        println("WARNING: polylist instead of triangles not fully implemented!")
                    }
                    val trianglesCount = triangles.getInt("count") ?: 0
                    geom.materialId = triangles.getString("material")

                    log { "triangles: $triangles" }
                    var stride = 1
                    val inputs = arrayListOf<Input>()
                    for (input in triangles["input"]) {
                        val offset = input.getInt("offset") ?: 0
                        stride = max2(stride, offset + 1)

                        val semantic = input.getString("semantic") ?: "unknown"
                        val source = input.getString("source")?.trim('#') ?: "unknown"
                        val rsource = sources[source] ?: continue
                        inputs += Input(semantic, offset, rsource, intArrayListOf())
                        log { "INPUT: semantic=$semantic, source=$source, offset=$offset, source=$rsource" }
                    }
                    val pdata = (triangles["p"].firstOrNull()?.text ?: "").reader().readInts()
                    //println("P: " + pdata.toList())
                    for (input in inputs) {
                        log { "INPUT: semantic=${input.semantic}, trianglesCount=$trianglesCount, stride=$stride, offset=${input.offset}" }
                        for (n in 0 until trianglesCount * 3) {
                            input.indices.add(pdata[input.offset + n * stride])
                        }
                        log { "  - ${input.indices}" }
                    }
                    for (input in inputs) {
                        geom.inputs[input.semantic] = input
                    }
                }
            }
        }

        return geometries
    }

    fun Library3D.parseVisualScenes(xml: Xml) {
        val instancesById = FastStringMap<Library3D.Instance3D>()
        for (vscene in xml["library_visual_scenes"]["visual_scene"]) {
            val scene = Library3D.Scene3D(this)
            scene.id = vscene.str("id")
            scene.name = vscene.str("name")
            for (node in vscene["node"]) {
                val instance = parseVisualSceneNode(node, instancesById)
                scene.children += instance
            }
            scenes[scene.id] = scene
        }
    }

    fun Library3D.parseVisualSceneNode(
        node: Xml,
        instancesById: FastStringMap<Library3D.Instance3D>
    ): Library3D.Instance3D {
        val instance = Library3D.Instance3D(this)
        var location: Vector3D? = null
        var scale: Vector3D? = null
        var rotationX: Vector3D? = null
        var rotationY: Vector3D? = null
        var rotationZ: Vector3D? = null

        instance.id = node.str("id")
        instance.sid = node.getString("sid")
        instance.name = node.str("name")
        instance.type = node.str("type")

        instancesById[instance.id] = instance

        for (child in node.allNodeChildren) {
            when (child.nameLC) {
                "matrix" -> {
                    val sid = child.str("sid")
                    when (sid) {
                        "transform" -> instance.transform.copyFrom(child.text.reader().readMatrix3D())
                        else -> println("WARNING: Unsupported node.matrix.sid=$sid")
                    }
                }
                "translate" -> {
                    val sid = child.str("sid")
                    when (sid) {
                        "location" -> location = child.text.reader().readVector3D()
                        else -> println("WARNING: Unsupported node.translate.sid=$sid")
                    }
                }
                "rotate" -> {
                    val sid = child.str("sid")
                    when (sid) {
                        "rotationX" -> rotationX = child.text.reader().readVector3D()
                        "rotationY" -> rotationY = child.text.reader().readVector3D()
                        "rotationZ" -> rotationZ = child.text.reader().readVector3D()
                        else -> println("WARNING: Unsupported node.rotate.sid=$sid")
                    }
                }
                "scale" -> {
                    val sid = child.str("sid")
                    when (sid) {
                        "scale" -> scale = child.text.reader().readVector3D()
                        else -> println("WARNING: Unsupported node.scale.sid=$sid")
                    }
                }
                "instance_camera" -> {
                    val cameraId = child.str("url").trim('#')
                    instance.def = cameraDefs[cameraId]
                }
                "instance_light" -> {
                    val lightId = child.str("url").trim('#')
                    instance.def = lightDefs[lightId]
                }
                "instance_geometry" -> {
                    val geometryId = child.str("url").trim('#')
                    val geometryName = child.str("name")
                    instance.def = geometryDefs[geometryId]
                }
                "node" -> {
                    val childInstance = parseVisualSceneNode(child, instancesById)
                    instance.children.add(childInstance)
                }
                "instance_controller" -> {
                    val skinId = child.str("url").trim('#')
                    val skeletonId = child["skeleton"].firstText?.trim('#') ?: ""
                    val skin = skinDefs[skinId]
                    val skeleton = instancesById[skeletonId]
                    instance.def = geometryDefs[skin?.skinSource ?: ""]
                    instance.skin = skin
                    instance.skeleton = skeleton
                    instance.skeletonId = skeletonId
                }
                "extra" -> {
                }
                else -> {
                    println("WARNING: Unsupported node.${child.nameLC}")
                }
            }
        }
        if (location != null || scale != null || rotationX != null || rotationY != null || rotationZ != null) {
            val trns = location ?: Vector3D(0, 0, 0, 1)
            val scl = scale ?: Vector3D(1, 1, 1)
            val rotX = rotationX ?: Vector3D(1, 0, 0, 0)
            val rotY = rotationY ?: Vector3D(0, 1, 0, 0)
            val rotZ = rotationZ ?: Vector3D(0, 0, 1, 0)
            rotationVectorToEulerRotation(rotX)

            instance.transform.setTRS(
                trns,
                Quaternion().setTo(
                    combine(
                        rotationVectorToEulerRotation(rotX),
                        rotationVectorToEulerRotation(rotY),
                        rotationVectorToEulerRotation(rotZ)
                    )
                ),
                scl
            )
        }
        return instance
    }

    private fun combine(
        a: EulerRotation,
        b: EulerRotation,
        c: EulerRotation,
        out: EulerRotation = EulerRotation()
    ): EulerRotation {
        return out.setTo(a.x + b.x + c.x, a.y + b.y + c.y, a.z + b.z + c.z)
    }

    private fun rotationVectorToEulerRotation(vec: Vector3D, out: EulerRotation = EulerRotation()): EulerRotation {
        val degrees = vec.w.degrees
        return out.setTo(degrees * vec.x, degrees * vec.y, degrees * vec.z)
    }

    val sourceArrayParams = FastStringMap<SourceParam>()
    val sources = FastStringMap<Source>()

    fun parseSources(xml: Xml, arraySourceParams: FastStringMap<SourceParam>): List<Source> {
        val sources = arrayListOf<Source>()
        for (source in xml["source"]) {
            val sourceId = source.str("id")
            val sourceParams = FastStringMap<SourceParam>()
            for (item in source.allNodeChildren) {
                when (item.nameLC) {
                    "float_array", "name_array" -> {
                        val arrayId = item.str("id")
                        val arrayCount = item.int("count")
                        val arrayDataStr = item.text
                        val arrayDataReader = arrayDataStr.reader()
                        val arraySourceParam = when (item.nameLC) {
                            "float_array" -> FloatSourceParam(
                                arrayId,
                                arrayDataReader.readFloats(FloatArrayList(arrayCount))
                            )
                            "name_array" -> NamesSourceParam(arrayId, arrayDataReader.readIds(ArrayList(arrayCount)))
                            else -> TODO()
                        }
                        arraySourceParams[arraySourceParam.name] = arraySourceParam
                    }
                    "technique_common" -> {
                        for (accessor in item["accessor"]) {
                            val accessorArrayId = accessor.str("source").trim('#')
                            val offset = accessor.int("offset")
                            val count = accessor.int("count")
                            val stride = accessor.int("stride")
                            val data = arraySourceParams[accessorArrayId]
                            if (data != null) {
                                for ((index, param) in accessor["param"].withIndex()) {
                                    val paramName = param.str("name")
                                    val paramType = param.str("type")
                                    val paramOffset = param.int("offset", index)
                                    val totalOffset = offset + paramOffset

                                    when (paramType) {
                                        "float" -> {
                                            val floats = (data as FloatSourceParam).floats.data
                                            val out = FloatArray(count)
                                            for (n in 0 until count) out[n] = floats[(n * stride) + totalOffset]
                                            sourceParams[paramName] = FloatSourceParam(paramName, FloatArrayList(*out))
                                        }
                                        "float4x4" -> {
                                            val floats = (data as FloatSourceParam).floats.data
                                            val out = Array(count) { Matrix3D() }
                                            for (n in 0 until count) {
                                                out[n].setFromColladaData(floats, (n * stride) + totalOffset)
                                                //mat.transpose()
                                            }
                                            sourceParams[paramName] = MatrixSourceParam(paramName, out)
                                        }
                                        "name" -> {
                                            val data = (data as NamesSourceParam).names
                                            val out = ArrayList<String>(count)
                                            for (n in 0 until count) out.add(data[(n * stride) + totalOffset])
                                            sourceParams[paramName] = NamesSourceParam(paramName, out)
                                        }
                                        else -> error("Unsupported paramType=$paramType")
                                    }

                                    //params[paramName] =
                                }
                            }
                        }
                    }
                    "extra" -> {
                    }
                    else -> {
                        error("Unsupported tag <${item.nameLC}> in <source>")
                    }
                }
            }
            sources += Source(sourceId, sourceParams)
        }
        return sources
    }

    inline fun log(str: () -> String) {
        // DO NOTHING
    }
}

//fun com.soywiz.korma.geom.Matrix3D.setFromColladaData(f: FloatArray, o: Int) = setColumns(
//private fun Matrix3D.setFromColladaData(f: FloatArray, o: Int) = setColumns4x4(f, o)
private fun Matrix3D.setFromColladaData(f: FloatArray, o: Int) = setRows4x4(f, o)

private fun StrReader.readVector3D(): Vector3D {
    val f = readFloats(FloatArrayList())
    return when {
        f.size == 4 -> Vector3D(f[0], f[1], f[2], f[3])
        else -> Vector3D(f[0], f[1], f[2])
    }
}

private fun StrReader.readMatrix3D(): Matrix3D {
    val f = readFloats(FloatArrayList())
    if (f.size == 16) {
        //return com.soywiz.korma.geom.Matrix3D().setRows(
        return Matrix3D().setFromColladaData(f.data, 0)
    } else {
        error("Invalid matrix size ${f.size} : str='$str'")
    }
}

private fun <T : Any> Map<String, T>.toFast() = FastStringMap<T>().apply {
    @Suppress("MapGetWithNotNullAssertionOperator")
    for (k in this@toFast.keys) {
        this[k] = this@toFast[k]!!
    }
}
