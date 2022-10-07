// To parse the JSON, install kotlin's serialization plugin and do:
//
// val json = Json(JsonConfiguration.Stable)
// val ldtk = json.parse(Ldtk.serializer(), jsonString)

package com.soywiz.korge.tiled.ldtk

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * This file is a JSON schema of files created by LDtk level editor (https://ldtk.io).
 *
 * This is the root of any Project JSON file. It contains:  - the project settings, - an
 * array of levels, - a group of definitions (that can probably be safely ignored for most
 * users).
 */
@Serializable
data class LDTKJson (
    /**
     * This object is not actually used by LDtk. It ONLY exists to force explicit references to
     * all types, to make sure QuickType finds them and integrate all of them. Otherwise,
     * Quicktype will drop types that are not explicitely used.
     */
    @SerialName("__FORCED_REFS")
    val forcedRefs: ForcedRefs? = null,

    /**
     * LDtk application build identifier.<br/>  This is only used to identify the LDtk version
     * that generated this particular project file, which can be useful for specific bug fixing.
     * Note that the build identifier is just the date of the release, so it's not unique to
     * each user (one single global ID per LDtk public release), and as a result, completely
     * anonymous.
     */
    @SerialName("appBuildId")
    val appBuildID: Double,

    /**
     * Number of backup files to keep, if the `backupOnSave` is TRUE
     */
    val backupLimit: Int,

    /**
     * If TRUE, an extra copy of the project will be created in a sub folder, when saving.
     */
    val backupOnSave: Boolean,

    /**
     * Project background color
     */
    val bgColor: String,

    /**
     * Default grid size for new layers
     */
    val defaultGridSize: Int,

    /**
     * Default background color of levels
     */
    val defaultLevelBgColor: String,

    /**
     * **WARNING**: this field will move to the `worlds` array after the "multi-worlds" update.
     * It will then be `null`. You can enable the Multi-worlds advanced project option to enable
     * the change immediately.<br/><br/>  Default new level height
     */
    val defaultLevelHeight: Int? = null,

    /**
     * **WARNING**: this field will move to the `worlds` array after the "multi-worlds" update.
     * It will then be `null`. You can enable the Multi-worlds advanced project option to enable
     * the change immediately.<br/><br/>  Default new level width
     */
    val defaultLevelWidth: Int? = null,

    /**
     * Default X pivot (0 to 1) for new entities
     */
    val defaultPivotX: Double,

    /**
     * Default Y pivot (0 to 1) for new entities
     */
    val defaultPivotY: Double,

    /**
     * A structure containing all the definitions of this project
     */
    val defs: Definitions,

    /**
     * If TRUE, a Tiled compatible file will also be generated along with the LDtk JSON file
     * (default is FALSE)
     */
    val exportTiled: Boolean,

    /**
     * If TRUE, one file will be saved for the project (incl. all its definitions) and one file
     * in a sub-folder for each level.
     */
    val externalLevels: Boolean,

    /**
     * An array containing various advanced flags (ie. options or other states). Possible
     * values: `DiscardPreCsvIntGrid`, `ExportPreCsvIntGridFormat`, `IgnoreBackupSuggest`,
     * `PrependIndexToLevelFileNames`, `MultiWorlds`, `UseMultilinesType`
     */
    val flags: List<Flag>,

    /**
     * Naming convention for Identifiers (first-letter uppercase, full uppercase etc.) Possible
     * values: `Capitalize`, `Uppercase`, `Lowercase`, `Free`
     */
    val identifierStyle: IdentifierStyle,

    /**
     * "Image export" option when saving project. Possible values: `None`, `OneImagePerLayer`,
     * `OneImagePerLevel`, `LayersAndLevels`
     */
    val imageExportMode: ImageExportMode,

    /**
     * File format version
     */
    val jsonVersion: String,

    /**
     * The default naming convention for level identifiers.
     */
    val levelNamePattern: String,

    /**
     * All levels. The order of this array is only relevant in `LinearHorizontal` and
     * `linearVertical` world layouts (see `worldLayout` value).<br/>  Otherwise, you should
     * refer to the `worldX`,`worldY` coordinates of each Level.
     */
    val levels: List<Level>,

    /**
     * If TRUE, the Json is partially minified (no indentation, nor line breaks, default is
     * FALSE)
     */
    @SerialName("minifyJson")
    val minifyJSON: Boolean,

    /**
     * Next Unique integer ID available
     */
    val nextUid: Int,

    /**
     * File naming pattern for exported PNGs
     */
    val pngFilePattern: String? = null,

    /**
     * If TRUE, a very simplified will be generated on saving, for quicker & easier engine
     * integration.
     */
    val simplifiedExport: Boolean,

    /**
     * This optional description is used by LDtk Samples to show up some informations and
     * instructions.
     */
    val tutorialDesc: String? = null,

    /**
     * **WARNING**: this field will move to the `worlds` array after the "multi-worlds" update.
     * It will then be `null`. You can enable the Multi-worlds advanced project option to enable
     * the change immediately.<br/><br/>  Height of the world grid in pixels.
     */
    val worldGridHeight: Int? = null,

    /**
     * **WARNING**: this field will move to the `worlds` array after the "multi-worlds" update.
     * It will then be `null`. You can enable the Multi-worlds advanced project option to enable
     * the change immediately.<br/><br/>  Width of the world grid in pixels.
     */
    val worldGridWidth: Int? = null,

    /**
     * **WARNING**: this field will move to the `worlds` array after the "multi-worlds" update.
     * It will then be `null`. You can enable the Multi-worlds advanced project option to enable
     * the change immediately.<br/><br/>  An enum that describes how levels are organized in
     * this project (ie. linearly or in a 2D space). Possible values: &lt;`null`&gt;, `Free`,
     * `GridVania`, `LinearHorizontal`, `LinearVertical`
     */
    val worldLayout: WorldLayout? = null,

    /**
     * This array is not used yet in current LDtk version (so, for now, it's always
     * empty).<br/><br/>In a later update, it will be possible to have multiple Worlds in a
     * single project, each containing multiple Levels.<br/><br/>What will change when "Multiple
     * worlds" support will be added to LDtk:<br/><br/> - in current version, a LDtk project
     * file can only contain a single world with multiple levels in it. In this case, levels and
     * world layout related settings are stored in the root of the JSON.<br/> - after the
     * "Multiple worlds" update, there will be a `worlds` array in root, each world containing
     * levels and layout settings. Basically, it's pretty much only about moving the `levels`
     * array to the `worlds` array, along with world layout related values (eg. `worldGridWidth`
     * etc).<br/><br/>If you want to start supporting this future update easily, please refer to
     * this documentation: https://github.com/deepnight/ldtk/issues/231
     */
    val worlds: List<World>
) {
    companion object {
        fun load(jsonString: String): LDTKJson = Json { ignoreUnknownKeys = true }.decodeFromString(jsonString)
    }
}

/**
 * If you're writing your own LDtk importer, you should probably just ignore *most* stuff in
 * the `defs` section, as it contains data that are mostly important to the editor. To keep
 * you away from the `defs` section and avoid some unnecessary JSON parsing, important data
 * from definitions is often duplicated in fields prefixed with a double underscore (eg.
 * `__identifier` or `__type`).  The 2 only definition types you might need here are
 * **Tilesets** and **Enums**.
 *
 * A structure containing all the definitions of this project
 */
@Serializable
data class Definitions (
    /**
     * All entities definitions, including their custom fields
     */
    val entities: List<EntityDefinition>,

    /**
     * All internal enums
     */
    val enums: List<EnumDefinition>,

    /**
     * Note: external enums are exactly the same as `enums`, except they have a `relPath` to
     * point to an external source file.
     */
    val externalEnums: List<EnumDefinition>,

    /**
     * All layer definitions
     */
    val layers: List<LayerDefinition>,

    /**
     * All custom fields available to all levels.
     */
    val levelFields: List<FieldDefinition>,

    /**
     * All tilesets
     */
    val tilesets: List<TilesetDefinition>
)

@Serializable
data class EntityDefinition (
    /**
     * Base entity color
     */
    val color: String,

    /**
     * Array of field definitions
     */
    val fieldDefs: List<FieldDefinition>,

    val fillOpacity: Double,

    /**
     * Pixel height
     */
    val height: Int,

    val hollow: Boolean,

    /**
     * User defined unique identifier
     */
    val identifier: String,

    /**
     * Only applies to entities resizable on both X/Y. If TRUE, the entity instance width/height
     * will keep the same aspect ratio as the definition.
     */
    val keepAspectRatio: Boolean,

    /**
     * Possible values: `DiscardOldOnes`, `PreventAdding`, `MoveLastOne`
     */
    val limitBehavior: LimitBehavior,

    /**
     * If TRUE, the maxCount is a "per world" limit, if FALSE, it's a "per level". Possible
     * values: `PerLayer`, `PerLevel`, `PerWorld`
     */
    val limitScope: LimitScope,

    val lineOpacity: Double,

    /**
     * Max instances count
     */
    val maxCount: Int,

    /**
     * An array of 4 dimensions for the up/right/down/left borders (in this order) when using
     * 9-slice mode for `tileRenderMode`.<br/>  If the tileRenderMode is not NineSlice, then
     * this array is empty.<br/>  See: https://en.wikipedia.org/wiki/9-slice_scaling
     */
    val nineSliceBorders: IntArray,

    /**
     * Pivot X coordinate (from 0 to 1.0)
     */
    val pivotX: Double,

    /**
     * Pivot Y coordinate (from 0 to 1.0)
     */
    val pivotY: Double,

    /**
     * Possible values: `Rectangle`, `Ellipse`, `Tile`, `Cross`
     */
    val renderMode: RenderMode,

    /**
     * If TRUE, the entity instances will be resizable horizontally
     */
    val resizableX: Boolean,

    /**
     * If TRUE, the entity instances will be resizable vertically
     */
    val resizableY: Boolean,

    /**
     * Display entity name in editor
     */
    val showName: Boolean,

    /**
     * An array of strings that classifies this entity
     */
    val tags: List<String>,

    val tileOpacity: Double,

    /**
     * An object representing a rectangle from an existing Tileset
     */
    val tileRect: TilesetRectangle? = null,

    /**
     * An enum describing how the the Entity tile is rendered inside the Entity bounds. Possible
     * values: `Cover`, `FitInside`, `Repeat`, `Stretch`, `FullSizeCropped`,
     * `FullSizeUncropped`, `NineSlice`
     */
    val tileRenderMode: TileRenderMode,

    /**
     * Tileset ID used for optional tile display
     */
    @SerialName("tilesetId")
    val tilesetID: Int? = null,

    /**
     * Unique Int identifier
     */
    val uid: Int,

    /**
     * Pixel width
     */
    val width: Int
)

/**
 * This section is mostly only intended for the LDtk editor app itself. You can safely
 * ignore it.
 */
@Serializable
data class FieldDefinition (
    /**
     * Human readable value type. Possible values: `Int, Float, String, Bool, Color,
     * ExternEnum.XXX, LocalEnum.XXX, Point, FilePath`.<br/>  If the field is an array, this
     * field will look like `Array<...>` (eg. `Array<Int>`, `Array<Point>` etc.)<br/>  NOTE: if
     * you enable the advanced option **Use Multilines type**, you will have "*Multilines*"
     * instead of "*String*" when relevant.
     */
    @SerialName("__type")
    val type: String,

    /**
     * Optional list of accepted file extensions for FilePath value type. Includes the dot:
     * `.ext`
     */
    val acceptFileTypes: List<String>? = null,

    /**
     * Possible values: `Any`, `OnlySame`, `OnlyTags`
     */
    val allowedRefs: AllowedRefs,

    val allowedRefTags: List<String>,
    val allowOutOfLevelRef: Boolean,

    /**
     * Array max length
     */
    val arrayMaxLength: Int? = null,

    /**
     * Array min length
     */
    val arrayMinLength: Int? = null,

    val autoChainRef: Boolean,

    /**
     * TRUE if the value can be null. For arrays, TRUE means it can contain null values
     * (exception: array of Points can't have null values).
     */
    val canBeNull: Boolean,

    /**
     * Default value if selected value is null or invalid.
     */
    val defaultOverride: JsonObject? = null,

    val editorAlwaysShow: Boolean,
    val editorCutLongValues: Boolean,

    /**
     * Possible values: `Hidden`, `ValueOnly`, `NameAndValue`, `EntityTile`, `Points`,
     * `PointStar`, `PointPath`, `PointPathLoop`, `RadiusPx`, `RadiusGrid`,
     * `ArrayCountWithLabel`, `ArrayCountNoLabel`, `RefLinkBetweenPivots`,
     * `RefLinkBetweenCenters`
     */
    val editorDisplayMode: EditorDisplayMode,

    /**
     * Possible values: `Above`, `Center`, `Beneath`
     */
    val editorDisplayPos: EditorDisplayPos,

    val editorTextPrefix: String? = null,
    val editorTextSuffix: String? = null,

    /**
     * User defined unique identifier
     */
    val identifier: String,

    /**
     * TRUE if the value is an array of multiple values
     */
    val isArray: Boolean,

    /**
     * Max limit for value, if applicable
     */
    val max: Double? = null,

    /**
     * Min limit for value, if applicable
     */
    val min: Double? = null,

    /**
     * Optional regular expression that needs to be matched to accept values. Expected format:
     * `/some_reg_ex/g`, with optional "i" flag.
     */
    val regex: String? = null,

    val symmetricalRef: Boolean,

    /**
     * Possible values: &lt;`null`&gt;, `LangPython`, `LangRuby`, `LangJS`, `LangLua`, `LangC`,
     * `LangHaxe`, `LangMarkdown`, `LangJson`, `LangXml`, `LangLog`
     */
    val textLanguageMode: TextLanguageMode? = null,

    /**
     * UID of the tileset used for a Tile
     */
    val tilesetUid: Int? = null,

    /**
     * Internal enum representing the possible field types. Possible values: F_Int, F_Float,
     * F_String, F_Text, F_Bool, F_Color, F_Enum(...), F_Point, F_Path, F_EntityRef, F_Tile
     */
    @SerialName("type")
    val fieldDefinitionType: String,

    /**
     * Unique Int identifier
     */
    val uid: Int,

    /**
     * If TRUE, the color associated with this field will override the Entity or Level default
     * color in the editor UI. For Enum fields, this would be the color associated to their
     * values.
     */
    val useForSmartColor: Boolean
)

/**
 * Possible values: `Any`, `OnlySame`, `OnlyTags`
 */
@Serializable
enum class AllowedRefs(val value: String) {
    AllowedRefsAny("Any"),
    OnlySame("OnlySame"),
    OnlyTags("OnlyTags");

    companion object : KSerializer<AllowedRefs> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.AllowedRefs", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): AllowedRefs = when (val value = decoder.decodeString()) {
            "Any"      -> AllowedRefsAny
            "OnlySame" -> OnlySame
            "OnlyTags" -> OnlyTags
            else       -> throw IllegalArgumentException("AllowedRefs could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: AllowedRefs) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * Possible values: `Hidden`, `ValueOnly`, `NameAndValue`, `EntityTile`, `Points`,
 * `PointStar`, `PointPath`, `PointPathLoop`, `RadiusPx`, `RadiusGrid`,
 * `ArrayCountWithLabel`, `ArrayCountNoLabel`, `RefLinkBetweenPivots`,
 * `RefLinkBetweenCenters`
 */
@Serializable
enum class EditorDisplayMode(val value: String) {
    ArrayCountNoLabel("ArrayCountNoLabel"),
    ArrayCountWithLabel("ArrayCountWithLabel"),
    EntityTile("EntityTile"),
    Hidden("Hidden"),
    NameAndValue("NameAndValue"),
    PointPath("PointPath"),
    PointPathLoop("PointPathLoop"),
    PointStar("PointStar"),
    Points("Points"),
    RadiusGrid("RadiusGrid"),
    RadiusPx("RadiusPx"),
    RefLinkBetweenCenters("RefLinkBetweenCenters"),
    RefLinkBetweenPivots("RefLinkBetweenPivots"),
    ValueOnly("ValueOnly");

    companion object : KSerializer<EditorDisplayMode> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.EditorDisplayMode", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): EditorDisplayMode = when (val value = decoder.decodeString()) {
            "ArrayCountNoLabel"     -> ArrayCountNoLabel
            "ArrayCountWithLabel"   -> ArrayCountWithLabel
            "EntityTile"            -> EntityTile
            "Hidden"                -> Hidden
            "NameAndValue"          -> NameAndValue
            "PointPath"             -> PointPath
            "PointPathLoop"         -> PointPathLoop
            "PointStar"             -> PointStar
            "Points"                -> Points
            "RadiusGrid"            -> RadiusGrid
            "RadiusPx"              -> RadiusPx
            "RefLinkBetweenCenters" -> RefLinkBetweenCenters
            "RefLinkBetweenPivots"  -> RefLinkBetweenPivots
            "ValueOnly"             -> ValueOnly
            else                    -> throw IllegalArgumentException("EditorDisplayMode could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: EditorDisplayMode) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * Possible values: `Above`, `Center`, `Beneath`
 */
@Serializable
enum class EditorDisplayPos(val value: String) {
    Above("Above"),
    Beneath("Beneath"),
    Center("Center");

    companion object : KSerializer<EditorDisplayPos> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.EditorDisplayPos", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): EditorDisplayPos = when (val value = decoder.decodeString()) {
            "Above"   -> Above
            "Beneath" -> Beneath
            "Center"  -> Center
            else      -> throw IllegalArgumentException("EditorDisplayPos could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: EditorDisplayPos) {
            return encoder.encodeString(value.value)
        }
    }
}

@Serializable
enum class TextLanguageMode(val value: String) {
    LangC("LangC"),
    LangHaxe("LangHaxe"),
    LangJS("LangJS"),
    LangJSON("LangJson"),
    LangLog("LangLog"),
    LangLua("LangLua"),
    LangMarkdown("LangMarkdown"),
    LangPython("LangPython"),
    LangRuby("LangRuby"),
    LangXML("LangXml");

    companion object : KSerializer<TextLanguageMode> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.TextLanguageMode", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): TextLanguageMode = when (val value = decoder.decodeString()) {
            "LangC"        -> LangC
            "LangHaxe"     -> LangHaxe
            "LangJS"       -> LangJS
            "LangJson"     -> LangJSON
            "LangLog"      -> LangLog
            "LangLua"      -> LangLua
            "LangMarkdown" -> LangMarkdown
            "LangPython"   -> LangPython
            "LangRuby"     -> LangRuby
            "LangXml"      -> LangXML
            else           -> throw IllegalArgumentException("TextLanguageMode could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: TextLanguageMode) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * Possible values: `DiscardOldOnes`, `PreventAdding`, `MoveLastOne`
 */
@Serializable
enum class LimitBehavior(val value: String) {
    DiscardOldOnes("DiscardOldOnes"),
    MoveLastOne("MoveLastOne"),
    PreventAdding("PreventAdding");

    companion object : KSerializer<LimitBehavior> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.LimitBehavior", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): LimitBehavior = when (val value = decoder.decodeString()) {
            "DiscardOldOnes" -> DiscardOldOnes
            "MoveLastOne"    -> MoveLastOne
            "PreventAdding"  -> PreventAdding
            else             -> throw IllegalArgumentException("LimitBehavior could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: LimitBehavior) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * If TRUE, the maxCount is a "per world" limit, if FALSE, it's a "per level". Possible
 * values: `PerLayer`, `PerLevel`, `PerWorld`
 */
@Serializable
enum class LimitScope(val value: String) {
    PerLayer("PerLayer"),
    PerLevel("PerLevel"),
    PerWorld("PerWorld");

    companion object : KSerializer<LimitScope> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.LimitScope", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): LimitScope = when (val value = decoder.decodeString()) {
            "PerLayer" -> PerLayer
            "PerLevel" -> PerLevel
            "PerWorld" -> PerWorld
            else       -> throw IllegalArgumentException("LimitScope could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: LimitScope) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * Possible values: `Rectangle`, `Ellipse`, `Tile`, `Cross`
 */
@Serializable
enum class RenderMode(val value: String) {
    Cross("Cross"),
    Ellipse("Ellipse"),
    Rectangle("Rectangle"),
    Tile("Tile");

    companion object : KSerializer<RenderMode> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.RenderMode", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): RenderMode = when (val value = decoder.decodeString()) {
            "Cross"     -> Cross
            "Ellipse"   -> Ellipse
            "Rectangle" -> Rectangle
            "Tile"      -> Tile
            else        -> throw IllegalArgumentException("RenderMode could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: RenderMode) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * This object represents a custom sub rectangle in a Tileset image.
 */
@Serializable
data class TilesetRectangle (
    /**
     * Height in pixels
     */
    val h: Int,

    /**
     * UID of the tileset
     */
    val tilesetUid: Int,

    /**
     * Width in pixels
     */
    val w: Int,

    /**
     * X pixels coordinate of the top-left corner in the Tileset image
     */
    val x: Int,

    /**
     * Y pixels coordinate of the top-left corner in the Tileset image
     */
    val y: Int
)

/**
 * An enum describing how the the Entity tile is rendered inside the Entity bounds. Possible
 * values: `Cover`, `FitInside`, `Repeat`, `Stretch`, `FullSizeCropped`,
 * `FullSizeUncropped`, `NineSlice`
 */
@Serializable
enum class TileRenderMode(val value: String) {
    Cover("Cover"),
    FitInside("FitInside"),
    FullSizeCropped("FullSizeCropped"),
    FullSizeUncropped("FullSizeUncropped"),
    NineSlice("NineSlice"),
    Repeat("Repeat"),
    Stretch("Stretch");

    companion object : KSerializer<TileRenderMode> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.TileRenderMode", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): TileRenderMode = when (val value = decoder.decodeString()) {
            "Cover"             -> Cover
            "FitInside"         -> FitInside
            "FullSizeCropped"   -> FullSizeCropped
            "FullSizeUncropped" -> FullSizeUncropped
            "NineSlice"         -> NineSlice
            "Repeat"            -> Repeat
            "Stretch"           -> Stretch
            else                -> throw IllegalArgumentException("TileRenderMode could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: TileRenderMode) {
            return encoder.encodeString(value.value)
        }
    }
}

@Serializable
data class EnumDefinition (
    val externalFileChecksum: String? = null,

    /**
     * Relative path to the external file providing this Enum
     */
    val externalRelPath: String? = null,

    /**
     * Tileset UID if provided
     */
    val iconTilesetUid: Int? = null,

    /**
     * User defined unique identifier
     */
    val identifier: String,

    /**
     * An array of user-defined tags to organize the Enums
     */
    val tags: List<String>,

    /**
     * Unique Int identifier
     */
    val uid: Int,

    /**
     * All possible enum values, with their optional Tile infos.
     */
    val values: List<EnumValueDefinition>
)

@Serializable
data class EnumValueDefinition (
    /**
     * An array of 4 Int values that refers to the tile in the tileset image: `[ x, y, width,
     * height ]`
     */
    @SerialName("__tileSrcRect")
    val tileSrcRect: IntArray? = null,

    /**
     * Optional color
     */
    val color: Int,

    /**
     * Enum value
     */
    val id: String,

    /**
     * The optional ID of the tile
     */
    @SerialName("tileId")
    val tileID: Int? = null
)

@Serializable
data class LayerDefinition (
    /**
     * Type of the layer (*IntGrid, Entities, Tiles or AutoLayer*)
     */
    @SerialName("__type")
    val type: String,

    /**
     * Contains all the auto-layer rule definitions.
     */
    val autoRuleGroups: List<AutoLayerRuleGroup>,

    val autoSourceLayerDefUid: Int? = null,

    /**
     * Opacity of the layer (0 to 1.0)
     */
    val displayOpacity: Double,

    /**
     * An array of tags to forbid some Entities in this layer
     */
    val excludedTags: List<String>,

    /**
     * Width and height of the grid in pixels
     */
    val gridSize: Int,

    /**
     * Height of the optional "guide" grid in pixels
     */
    val guideGridHei: Int,

    /**
     * Width of the optional "guide" grid in pixels
     */
    val guideGridWid: Int,

    val hideFieldsWhenInactive: Boolean,

    /**
     * Hide the layer from the list on the side of the editor view.
     */
    val hideInList: Boolean,

    /**
     * User defined unique identifier
     */
    val identifier: String,

    /**
     * Alpha of this layer when it is not the active one.
     */
    val inactiveOpacity: Double,

    /**
     * An array that defines extra optional info for each IntGrid value.<br/>  WARNING: the
     * array order is not related to actual IntGrid values! As user can re-order IntGrid values
     * freely, you may value "2" before value "1" in this array.
     */
    val intGridValues: List<IntGridValueDefinition>,

    /**
     * Parallax horizontal factor (from -1 to 1, defaults to 0) which affects the scrolling
     * speed of this layer, creating a fake 3D (parallax) effect.
     */
    val parallaxFactorX: Double,

    /**
     * Parallax vertical factor (from -1 to 1, defaults to 0) which affects the scrolling speed
     * of this layer, creating a fake 3D (parallax) effect.
     */
    val parallaxFactorY: Double,

    /**
     * If true (default), a layer with a parallax factor will also be scaled up/down accordingly.
     */
    val parallaxScaling: Boolean,

    /**
     * X offset of the layer, in pixels (IMPORTANT: this should be added to the `LayerInstance`
     * optional offset)
     */
    val pxOffsetX: Int,

    /**
     * Y offset of the layer, in pixels (IMPORTANT: this should be added to the `LayerInstance`
     * optional offset)
     */
    val pxOffsetY: Int,

    /**
     * An array of tags to filter Entities that can be added to this layer
     */
    val requiredTags: List<String>,

    /**
     * If the tiles are smaller or larger than the layer grid, the pivot value will be used to
     * position the tile relatively its grid cell.
     */
    val tilePivotX: Double,

    /**
     * If the tiles are smaller or larger than the layer grid, the pivot value will be used to
     * position the tile relatively its grid cell.
     */
    val tilePivotY: Double,

    /**
     * Reference to the default Tileset UID being used by this layer definition.<br/>
     * **WARNING**: some layer *instances* might use a different tileset. So most of the time,
     * you should probably use the `__tilesetDefUid` value found in layer instances.<br/>  Note:
     * since version 1.0.0, the old `autoTilesetDefUid` was removed and merged into this value.
     */
    val tilesetDefUid: Int? = null,

    /**
     * Type of the layer as Haxe Enum Possible values: `IntGrid`, `Entities`, `Tiles`,
     * `AutoLayer`
     */
    @SerialName("type")
    val layerDefinitionType: Type,

    /**
     * Unique Int identifier
     */
    val uid: Int
)

@Serializable
data class AutoLayerRuleGroup (
    val active: Boolean,

    /**
     * *This field was removed in 1.0.0 and should no longer be used.*
     */
    val collapsed: Boolean? = null,

    val isOptional: Boolean,
    val name: String,
    val rules: List<AutoLayerRuleDefinition>,
    val uid: Int
)

/**
 * This complex section isn't meant to be used by game devs at all, as these rules are
 * completely resolved internally by the editor before any saving. You should just ignore
 * this part.
 */
@Serializable
data class AutoLayerRuleDefinition (
    /**
     * If FALSE, the rule effect isn't applied, and no tiles are generated.
     */
    val active: Boolean,

    /**
     * When TRUE, the rule will prevent other rules to be applied in the same cell if it matches
     * (TRUE by default).
     */
    val breakOnMatch: Boolean,

    /**
     * Chances for this rule to be applied (0 to 1)
     */
    val chance: Double,

    /**
     * Checker mode Possible values: `None`, `Horizontal`, `Vertical`
     */
    val checker: Checker,

    /**
     * If TRUE, allow rule to be matched by flipping its pattern horizontally
     */
    val flipX: Boolean,

    /**
     * If TRUE, allow rule to be matched by flipping its pattern vertically
     */
    val flipY: Boolean,

    /**
     * Default IntGrid value when checking cells outside of level bounds
     */
    val outOfBoundsValue: Int? = null,

    /**
     * Rule pattern (size x size)
     */
    val pattern: IntArray,

    /**
     * If TRUE, enable Perlin filtering to only apply rule on specific random area
     */
    val perlinActive: Boolean,

    val perlinOctaves: Double,
    val perlinScale: Double,
    val perlinSeed: Double,

    /**
     * X pivot of a tile stamp (0-1)
     */
    val pivotX: Double,

    /**
     * Y pivot of a tile stamp (0-1)
     */
    val pivotY: Double,

    /**
     * Pattern width & height. Should only be 1,3,5 or 7.
     */
    val size: Int,

    /**
     * Array of all the tile IDs. They are used randomly or as stamps, based on `tileMode` value.
     */
    @SerialName("tileIds")
    val tileIDS: IntArray,

    /**
     * Defines how tileIds array is used Possible values: `Single`, `Stamp`
     */
    val tileMode: TileMode,

    /**
     * Unique Int identifier
     */
    val uid: Int,

    /**
     * X cell coord modulo
     */
    val xModulo: Int,

    /**
     * X cell start offset
     */
    val xOffset: Int,

    /**
     * Y cell coord modulo
     */
    val yModulo: Int,

    /**
     * Y cell start offset
     */
    val yOffset: Int
)

/**
 * Checker mode Possible values: `None`, `Horizontal`, `Vertical`
 */
@Serializable
enum class Checker(val value: String) {
    Horizontal("Horizontal"),
    None("None"),
    Vertical("Vertical");

    companion object : KSerializer<Checker> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.Checker", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): Checker = when (val value = decoder.decodeString()) {
            "Horizontal" -> Horizontal
            "None"       -> None
            "Vertical"   -> Vertical
            else         -> throw IllegalArgumentException("Checker could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: Checker) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * Defines how tileIds array is used Possible values: `Single`, `Stamp`
 */
@Serializable
enum class TileMode(val value: String) {
    Single("Single"),
    Stamp("Stamp");

    companion object : KSerializer<TileMode> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.TileMode", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): TileMode = when (val value = decoder.decodeString()) {
            "Single" -> Single
            "Stamp"  -> Stamp
            else     -> throw IllegalArgumentException("TileMode could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: TileMode) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * IntGrid value definition
 */
@Serializable
data class IntGridValueDefinition (
    val color: String,

    /**
     * User defined unique identifier
     */
    val identifier: String? = null,

    /**
     * The IntGrid value itself
     */
    val value: Int
)

/**
 * Type of the layer as Haxe Enum Possible values: `IntGrid`, `Entities`, `Tiles`,
 * `AutoLayer`
 */
@Serializable
enum class Type(val value: String) {
    AutoLayer("AutoLayer"),
    Entities("Entities"),
    IntGrid("IntGrid"),
    Tiles("Tiles");

    companion object : KSerializer<Type> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.Type", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): Type = when (val value = decoder.decodeString()) {
            "AutoLayer" -> AutoLayer
            "Entities"  -> Entities
            "IntGrid"   -> IntGrid
            "Tiles"     -> Tiles
            else        -> throw IllegalArgumentException("Type could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: Type) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * The `Tileset` definition is the most important part among project definitions. It
 * contains some extra informations about each integrated tileset. If you only had to parse
 * one definition section, that would be the one.
 */
@Serializable
data class TilesetDefinition (
    /**
     * Grid-based height
     */
    @SerialName("__cHei")
    val cHei: Int,

    /**
     * Grid-based width
     */
    @SerialName("__cWid")
    val cWid: Int,

    /**
     * The following data is used internally for various optimizations. It's always synced with
     * source image changes.
     */
    val cachedPixelData: JsonObject? = null,

    /**
     * An array of custom tile metadata
     */
    val customData: List<TileCustomMetadata>,

    /**
     * If this value is set, then it means that this atlas uses an internal LDtk atlas image
     * instead of a loaded one. Possible values: &lt;`null`&gt;, `LdtkIcons`
     */
    val embedAtlas: EmbedAtlas? = null,

    /**
     * Tileset tags using Enum values specified by `tagsSourceEnumId`. This array contains 1
     * element per Enum value, which contains an array of all Tile IDs that are tagged with it.
     */
    val enumTags: List<EnumTagValue>,

    /**
     * User defined unique identifier
     */
    val identifier: String,

    /**
     * Distance in pixels from image borders
     */
    val padding: Int,

    /**
     * Image height in pixels
     */
    val pxHei: Int,

    /**
     * Image width in pixels
     */
    val pxWid: Int,

    /**
     * Path to the source file, relative to the current project JSON file<br/>  It can be null
     * if no image was provided, or when using an embed atlas.
     */
    val relPath: String? = null,

    /**
     * Array of group of tiles selections, only meant to be used in the editor
     */
    val savedSelections: JsonArray,

    /**
     * Space in pixels between all tiles
     */
    val spacing: Int,

    /**
     * An array of user-defined tags to organize the Tilesets
     */
    val tags: List<String>,

    /**
     * Optional Enum definition UID used for this tileset meta-data
     */
    val tagsSourceEnumUid: Int? = null,

    val tileGridSize: Int,

    /**
     * Unique Intidentifier
     */
    val uid: Int
)

/**
 * In a tileset definition, user defined meta-data of a tile.
 */
@Serializable
data class TileCustomMetadata (
    val data: String,

    @SerialName("tileId")
    val tileID: Int
)

@Serializable
enum class EmbedAtlas(val value: String) {
    LdtkIcons("LdtkIcons");

    companion object : KSerializer<EmbedAtlas> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.EmbedAtlas", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): EmbedAtlas = when (val value = decoder.decodeString()) {
            "LdtkIcons" -> LdtkIcons
            else        -> throw IllegalArgumentException("EmbedAtlas could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: EmbedAtlas) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * In a tileset definition, enum based tag infos
 */
@Serializable
data class EnumTagValue (
    @SerialName("enumValueId")
    val enumValueID: String,

    @SerialName("tileIds")
    val tileIDS: IntArray
)

@Serializable
enum class Flag(val value: String) {
    DiscardPreCSVIntGrid("DiscardPreCsvIntGrid"),
    ExportPreCSVIntGridFormat("ExportPreCsvIntGridFormat"),
    IgnoreBackupSuggest("IgnoreBackupSuggest"),
    MultiWorlds("MultiWorlds"),
    PrependIndexToLevelFileNames("PrependIndexToLevelFileNames"),
    UseMultilinesType("UseMultilinesType");

    companion object : KSerializer<Flag> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.Flag", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): Flag = when (val value = decoder.decodeString()) {
            "DiscardPreCsvIntGrid"         -> DiscardPreCSVIntGrid
            "ExportPreCsvIntGridFormat"    -> ExportPreCSVIntGridFormat
            "IgnoreBackupSuggest"          -> IgnoreBackupSuggest
            "MultiWorlds"                  -> MultiWorlds
            "PrependIndexToLevelFileNames" -> PrependIndexToLevelFileNames
            "UseMultilinesType"            -> UseMultilinesType
            else                           -> throw IllegalArgumentException("Flag could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: Flag) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * This object is not actually used by LDtk. It ONLY exists to force explicit references to
 * all types, to make sure QuickType finds them and integrate all of them. Otherwise,
 * Quicktype will drop types that are not explicitely used.
 */
@Serializable
data class ForcedRefs (
    @SerialName("AutoLayerRuleGroup")
    val autoLayerRuleGroup: AutoLayerRuleGroup? = null,

    @SerialName("AutoRuleDef")
    val autoRuleDef: AutoLayerRuleDefinition? = null,

    @SerialName("Definitions")
    val definitions: Definitions? = null,

    @SerialName("EntityDef")
    val entityDef: EntityDefinition? = null,

    @SerialName("EntityInstance")
    val entityInstance: EntityInstance? = null,

    @SerialName("EntityReferenceInfos")
    val entityReferenceInfos: FieldInstanceEntityReference? = null,

    @SerialName("EnumDef")
    val enumDef: EnumDefinition? = null,

    @SerialName("EnumDefValues")
    val enumDefValues: EnumValueDefinition? = null,

    @SerialName("EnumTagValue")
    val enumTagValue: EnumTagValue? = null,

    @SerialName("FieldDef")
    val fieldDef: FieldDefinition? = null,

    @SerialName("FieldInstance")
    val fieldInstance: FieldInstance? = null,

    @SerialName("GridPoint")
    val gridPoint: FieldInstanceGridPoint? = null,

    @SerialName("IntGridValueDef")
    val intGridValueDef: IntGridValueDefinition? = null,

    @SerialName("IntGridValueInstance")
    val intGridValueInstance: IntGridValueInstance? = null,

    @SerialName("LayerDef")
    val layerDef: LayerDefinition? = null,

    @SerialName("LayerInstance")
    val layerInstance: LayerInstance? = null,

    @SerialName("Level")
    val level: Level? = null,

    @SerialName("LevelBgPosInfos")
    val levelBgPosInfos: LevelBackgroundPosition? = null,

    @SerialName("NeighbourLevel")
    val neighbourLevel: NeighbourLevel? = null,

    @SerialName("Tile")
    val tile: TileInstance? = null,

    @SerialName("TileCustomMetadata")
    val tileCustomMetadata: TileCustomMetadata? = null,

    @SerialName("TilesetDef")
    val tilesetDef: TilesetDefinition? = null,

    @SerialName("TilesetRect")
    val tilesetRect: TilesetRectangle? = null,

    @SerialName("World")
    val world: World? = null
)

@Serializable
data class EntityInstance (
    /**
     * Grid-based coordinates (`[x,y]` format)
     */
    @SerialName("__grid")
    val grid: IntArray,

    /**
     * Entity definition identifier
     */
    @SerialName("__identifier")
    val identifier: String,

    /**
     * Pivot coordinates  (`[x,y]` format, values are from 0 to 1) of the Entity
     */
    @SerialName("__pivot")
    val pivot: List<Double>,

    /**
     * The entity "smart" color, guessed from either Entity definition, or one its field
     * instances.
     */
    @SerialName("__smartColor")
    val smartColor: String,

    /**
     * Array of tags defined in this Entity definition
     */
    @SerialName("__tags")
    val tags: List<String>,

    /**
     * Optional TilesetRect used to display this entity (it could either be the default Entity
     * tile, or some tile provided by a field value, like an Enum).
     */
    @SerialName("__tile")
    val tile: TilesetRectangle? = null,

    /**
     * Reference of the **Entity definition** UID
     */
    val defUid: Int,

    /**
     * An array of all custom fields and their values.
     */
    val fieldInstances: List<FieldInstance>,

    /**
     * Entity height in pixels. For non-resizable entities, it will be the same as Entity
     * definition.
     */
    val height: Int,

    /**
     * Unique instance identifier
     */
    val iid: String,

    /**
     * Pixel coordinates (`[x,y]` format) in current level coordinate space. Don't forget
     * optional layer offsets, if they exist!
     */
    val px: IntArray,

    /**
     * Entity width in pixels. For non-resizable entities, it will be the same as Entity
     * definition.
     */
    val width: Int
)

@Serializable
data class FieldInstance (
    /**
     * Field definition identifier
     */
    @SerialName("__identifier")
    val identifier: String,

    /**
     * Optional TilesetRect used to display this field (this can be the field own Tile, or some
     * other Tile guessed from the value, like an Enum).
     */
    @SerialName("__tile")
    val tile: TilesetRectangle? = null,

    /**
     * Type of the field, such as `Int`, `Float`, `String`, `Enum(my_enum_name)`, `Bool`,
     * etc.<br/>  NOTE: if you enable the advanced option **Use Multilines type**, you will have
     * "*Multilines*" instead of "*String*" when relevant.
     */
    @SerialName("__type")
    val type: String,

    /**
     * Actual value of the field instance. The value type varies, depending on `__type`:<br/>
     * - For **classic types** (ie. Integer, Float, Boolean, String, Text and FilePath), you
     * just get the actual value with the expected type.<br/>   - For **Color**, the value is an
     * hexadecimal string using "#rrggbb" format.<br/>   - For **Enum**, the value is a String
     * representing the selected enum value.<br/>   - For **Point**, the value is a
     * [GridPoint](#ldtk-GridPoint) object.<br/>   - For **Tile**, the value is a
     * [TilesetRect](#ldtk-TilesetRect) object.<br/>   - For **EntityRef**, the value is an
     * [EntityReferenceInfos](#ldtk-EntityReferenceInfos) object.<br/><br/>  If the field is an
     * array, then this `__value` will also be a JSON array.
     */
    @SerialName("__value")
    val value: JsonElement?,

    /**
     * Reference of the **Field definition** UID
     */
    val defUid: Int,

    /**
     * Editor internal raw values
     */
    val realEditorValues: JsonArray
)

/**
 * This object is used in Field Instances to describe an EntityRef value.
 */
@Serializable
data class FieldInstanceEntityReference (
    /**
     * IID of the refered EntityInstance
     */
    val entityIid: String,

    /**
     * IID of the LayerInstance containing the refered EntityInstance
     */
    val layerIid: String,

    /**
     * IID of the Level containing the refered EntityInstance
     */
    val levelIid: String,

    /**
     * IID of the World containing the refered EntityInstance
     */
    val worldIid: String
)

/**
 * This object is just a grid-based coordinate used in Field values.
 */
@Serializable
data class FieldInstanceGridPoint (
    /**
     * X grid-based coordinate
     */
    val cx: Int,

    /**
     * Y grid-based coordinate
     */
    val cy: Int
)

/**
 * IntGrid value instance
 */
@Serializable
data class IntGridValueInstance (
    /**
     * Coordinate ID in the layer grid
     */
    @SerialName("coordId")
    val coordID: Int,

    /**
     * IntGrid value
     */
    val v: Int
)

@Serializable
data class LayerInstance (
    /**
     * Grid-based height
     */
    @SerialName("__cHei")
    val cHei: Int,

    /**
     * Grid-based width
     */
    @SerialName("__cWid")
    val cWid: Int,

    /**
     * Grid size
     */
    @SerialName("__gridSize")
    val gridSize: Int,

    /**
     * Layer definition identifier
     */
    @SerialName("__identifier")
    val identifier: String,

    /**
     * Layer opacity as Float [0-1]
     */
    @SerialName("__opacity")
    val opacity: Double,

    /**
     * Total layer X pixel offset, including both instance and definition offsets.
     */
    @SerialName("__pxTotalOffsetX")
    val pxTotalOffsetX: Int,

    /**
     * Total layer Y pixel offset, including both instance and definition offsets.
     */
    @SerialName("__pxTotalOffsetY")
    val pxTotalOffsetY: Int,

    /**
     * The definition UID of corresponding Tileset, if any.
     */
    @SerialName("__tilesetDefUid")
    val tilesetDefUid: Int? = null,

    /**
     * The relative path to corresponding Tileset, if any.
     */
    @SerialName("__tilesetRelPath")
    val tilesetRelPath: String? = null,

    /**
     * Layer type (possible values: IntGrid, Entities, Tiles or AutoLayer)
     */
    @SerialName("__type")
    val type: String,

    /**
     * An array containing all tiles generated by Auto-layer rules. The array is already sorted
     * in display order (ie. 1st tile is beneath 2nd, which is beneath 3rd etc.).<br/><br/>
     * Note: if multiple tiles are stacked in the same cell as the result of different rules,
     * all tiles behind opaque ones will be discarded.
     */
    val autoLayerTiles: List<TileInstance>,

    val entityInstances: List<EntityInstance>,
    val gridTiles: List<TileInstance>,

    /**
     * Unique layer instance identifier
     */
    val iid: String,

    /**
     * A list of all values in the IntGrid layer, stored in CSV format (Comma Separated
     * Values).<br/>  Order is from left to right, and top to bottom (ie. first row from left to
     * right, followed by second row, etc).<br/>  `0` means "empty cell" and IntGrid values
     * start at 1.<br/>  The array size is `__cWid` x `__cHei` cells.
     */
    @SerialName("intGridCsv")
    val intGridCSV: IntArray,

    /**
     * Reference the Layer definition UID
     */
    val layerDefUid: Int,

    /**
     * Reference to the UID of the level containing this layer instance
     */
    @SerialName("levelId")
    val levelID: Int,

    /**
     * An Array containing the UIDs of optional rules that were enabled in this specific layer
     * instance.
     */
    val optionalRules: IntArray,

    /**
     * This layer can use another tileset by overriding the tileset UID here.
     */
    val overrideTilesetUid: Int? = null,

    /**
     * X offset in pixels to render this layer, usually 0 (IMPORTANT: this should be added to
     * the `LayerDef` optional offset, see `__pxTotalOffsetX`)
     */
    val pxOffsetX: Int,

    /**
     * Y offset in pixels to render this layer, usually 0 (IMPORTANT: this should be added to
     * the `LayerDef` optional offset, see `__pxTotalOffsetY`)
     */
    val pxOffsetY: Int,

    /**
     * Random seed used for Auto-Layers rendering
     */
    val seed: Int,

    /**
     * Layer instance visibility
     */
    val visible: Boolean
)

/**
 * This structure represents a single tile from a given Tileset.
 */
@Serializable
data class TileInstance (
    /**
     * Internal data used by the editor.<br/>  For auto-layer tiles: `[ruleId, coordId]`.<br/>
     * For tile-layer tiles: `[coordId]`.
     */
    val d: IntArray,

    /**
     * "Flip bits", a 2-bits integer to represent the mirror transformations of the tile.<br/>
     * - Bit 0 = X flip<br/>   - Bit 1 = Y flip<br/>   Examples: f=0 (no flip), f=1 (X flip
     * only), f=2 (Y flip only), f=3 (both flips)
     */
    val f: Int,

    /**
     * Pixel coordinates of the tile in the **layer** (`[x,y]` format). Don't forget optional
     * layer offsets, if they exist!
     */
    val px: IntArray,

    /**
     * Pixel coordinates of the tile in the **tileset** (`[x,y]` format)
     */
    val src: IntArray,

    /**
     * The *Tile ID* in the corresponding tileset.
     */
    val t: Int
)

/**
 * This section contains all the level data. It can be found in 2 distinct forms, depending
 * on Project current settings:  - If "*Separate level files*" is **disabled** (default):
 * full level data is *embedded* inside the main Project JSON file, - If "*Separate level
 * files*" is **enabled**: level data is stored in *separate* standalone `.ldtkl` files (one
 * per level). In this case, the main Project JSON file will still contain most level data,
 * except heavy sections, like the `layerInstances` array (which will be null). The
 * `externalRelPath` string points to the `ldtkl` file.  A `ldtkl` file is just a JSON file
 * containing exactly what is described below.
 */
@Serializable
data class Level (
    /**
     * Background color of the level (same as `bgColor`, except the default value is
     * automatically used here if its value is `null`)
     */
    @SerialName("__bgColor")
    val bgColor: String,

    /**
     * Position informations of the background image, if there is one.
     */
    @SerialName("__bgPos")
    val bgPos: LevelBackgroundPosition? = null,

    /**
     * An array listing all other levels touching this one on the world map.<br/>  Only relevant
     * for world layouts where level spatial positioning is manual (ie. GridVania, Free). For
     * Horizontal and Vertical layouts, this array is always empty.
     */
    @SerialName("__neighbours")
    val neighbours: List<NeighbourLevel>,

    /**
     * The "guessed" color for this level in the editor, decided using either the background
     * color or an existing custom field.
     */
    @SerialName("__smartColor")
    val smartColor: String,

    /**
     * Background color of the level. If `null`, the project `defaultLevelBgColor` should be
     * used.
     */
    @SerialName("bgColor")
    val levelBgColor: String? = null,

    /**
     * Background image X pivot (0-1)
     */
    val bgPivotX: Double,

    /**
     * Background image Y pivot (0-1)
     */
    val bgPivotY: Double,

    /**
     * An enum defining the way the background image (if any) is positioned on the level. See
     * `__bgPos` for resulting position info. Possible values: &lt;`null`&gt;, `Unscaled`,
     * `Contain`, `Cover`, `CoverDirty`
     */
    @SerialName("bgPos")
    val levelBgPos: BgPos? = null,

    /**
     * The *optional* relative path to the level background image.
     */
    val bgRelPath: String? = null,

    /**
     * This value is not null if the project option "*Save levels separately*" is enabled. In
     * this case, this **relative** path points to the level Json file.
     */
    val externalRelPath: String? = null,

    /**
     * An array containing this level custom field values.
     */
    val fieldInstances: List<FieldInstance>,

    /**
     * User defined unique identifier
     */
    val identifier: String,

    /**
     * Unique instance identifier
     */
    val iid: String,

    /**
     * An array containing all Layer instances. **IMPORTANT**: if the project option "*Save
     * levels separately*" is enabled, this field will be `null`.<br/>  This array is **sorted
     * in display order**: the 1st layer is the top-most and the last is behind.
     */
    val layerInstances: List<LayerInstance>? = null,

    /**
     * Height of the level in pixels
     */
    val pxHei: Int,

    /**
     * Width of the level in pixels
     */
    val pxWid: Int,

    /**
     * Unique Int identifier
     */
    val uid: Int,

    /**
     * If TRUE, the level identifier will always automatically use the naming pattern as defined
     * in `Project.levelNamePattern`. Becomes FALSE if the identifier is manually modified by
     * user.
     */
    val useAutoIdentifier: Boolean,

    /**
     * Index that represents the "depth" of the level in the world. Default is 0, greater means
     * "above", lower means "below".<br/>  This value is mostly used for display only and is
     * intended to make stacking of levels easier to manage.
     */
    val worldDepth: Int,

    /**
     * World X coordinate in pixels.<br/>  Only relevant for world layouts where level spatial
     * positioning is manual (ie. GridVania, Free). For Horizontal and Vertical layouts, the
     * value is always -1 here.
     */
    val worldX: Int,

    /**
     * World Y coordinate in pixels.<br/>  Only relevant for world layouts where level spatial
     * positioning is manual (ie. GridVania, Free). For Horizontal and Vertical layouts, the
     * value is always -1 here.
     */
    val worldY: Int
)

/**
 * Level background image position info
 */
@Serializable
data class LevelBackgroundPosition (
    /**
     * An array of 4 float values describing the cropped sub-rectangle of the displayed
     * background image. This cropping happens when original is larger than the level bounds.
     * Array format: `[ cropX, cropY, cropWidth, cropHeight ]`
     */
    val cropRect: List<Double>,

    /**
     * An array containing the `[scaleX,scaleY]` values of the **cropped** background image,
     * depending on `bgPos` option.
     */
    val scale: List<Double>,

    /**
     * An array containing the `[x,y]` pixel coordinates of the top-left corner of the
     * **cropped** background image, depending on `bgPos` option.
     */
    val topLeftPx: IntArray
)

@Serializable
enum class BgPos(val value: String) {
    Contain("Contain"),
    Cover("Cover"),
    CoverDirty("CoverDirty"),
    Unscaled("Unscaled");

    companion object : KSerializer<BgPos> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.BgPos", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): BgPos = when (val value = decoder.decodeString()) {
            "Contain"    -> Contain
            "Cover"      -> Cover
            "CoverDirty" -> CoverDirty
            "Unscaled"   -> Unscaled
            else         -> throw IllegalArgumentException("BgPos could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: BgPos) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * Nearby level info
 */
@Serializable
data class NeighbourLevel (
    /**
     * A single lowercase character tipping on the level location (`n`orth, `s`outh, `w`est,
     * `e`ast).
     */
    val dir: String,

    /**
     * Neighbour Instance Identifier
     */
    val levelIid: String,
)

/**
 * **IMPORTANT**: this type is not used *yet* in current LDtk version. It's only presented
 * here as a preview of a planned feature.  A World contains multiple levels, and it has its
 * own layout settings.
 */
@Serializable
data class World (
    /**
     * Default new level height
     */
    val defaultLevelHeight: Int,

    /**
     * Default new level width
     */
    val defaultLevelWidth: Int,

    /**
     * User defined unique identifier
     */
    val identifier: String,

    /**
     * Unique instance identifer
     */
    val iid: String,

    /**
     * All levels from this world. The order of this array is only relevant in
     * `LinearHorizontal` and `linearVertical` world layouts (see `worldLayout` value).
     * Otherwise, you should refer to the `worldX`,`worldY` coordinates of each Level.
     */
    val levels: List<Level>,

    /**
     * Height of the world grid in pixels.
     */
    val worldGridHeight: Int,

    /**
     * Width of the world grid in pixels.
     */
    val worldGridWidth: Int,

    /**
     * An enum that describes how levels are organized in this project (ie. linearly or in a 2D
     * space). Possible values: `Free`, `GridVania`, `LinearHorizontal`, `LinearVertical`, `null`
     */
    val worldLayout: WorldLayout? = null
)

@Serializable
enum class WorldLayout(val value: String) {
    Free("Free"),
    GridVania("GridVania"),
    LinearHorizontal("LinearHorizontal"),
    LinearVertical("LinearVertical");

    companion object : KSerializer<WorldLayout> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.WorldLayout", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): WorldLayout = when (val value = decoder.decodeString()) {
            "Free"             -> Free
            "GridVania"        -> GridVania
            "LinearHorizontal" -> LinearHorizontal
            "LinearVertical"   -> LinearVertical
            else               -> throw IllegalArgumentException("WorldLayout could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: WorldLayout) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * Naming convention for Identifiers (first-letter uppercase, full uppercase etc.) Possible
 * values: `Capitalize`, `Uppercase`, `Lowercase`, `Free`
 */
@Serializable
enum class IdentifierStyle(val value: String) {
    Capitalize("Capitalize"),
    Free("Free"),
    Lowercase("Lowercase"),
    Uppercase("Uppercase");

    companion object : KSerializer<IdentifierStyle> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.IdentifierStyle", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): IdentifierStyle = when (val value = decoder.decodeString()) {
            "Capitalize" -> Capitalize
            "Free"       -> Free
            "Lowercase"  -> Lowercase
            "Uppercase"  -> Uppercase
            else         -> throw IllegalArgumentException("IdentifierStyle could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: IdentifierStyle) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * "Image export" option when saving project. Possible values: `None`, `OneImagePerLayer`,
 * `OneImagePerLevel`, `LayersAndLevels`
 */
@Serializable
enum class ImageExportMode(val value: String) {
    LayersAndLevels("LayersAndLevels"),
    None("None"),
    OneImagePerLayer("OneImagePerLayer"),
    OneImagePerLevel("OneImagePerLevel");

    companion object : KSerializer<ImageExportMode> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("com.soywiz.korge.tiled.ldtk.ImageExportMode", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): ImageExportMode = when (val value = decoder.decodeString()) {
            "LayersAndLevels"  -> LayersAndLevels
            "None"             -> None
            "OneImagePerLayer" -> OneImagePerLayer
            "OneImagePerLevel" -> OneImagePerLevel
            else               -> throw IllegalArgumentException("ImageExportMode could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: ImageExportMode) {
            return encoder.encodeString(value.value)
        }
    }
}
