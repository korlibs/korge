package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Entity.*
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Mainline.Key.BoneRef
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Mainline.Key.ObjectRef
import com.soywiz.korio.JvmField
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.xml.Xml
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readAll

/**
 * This class parses a SCML file and creates a [Data] instance.
 * If you want to keep track of what is going on during the build process of the objects parsed from the SCML file,
 * you could extend this class and override the load*() methods for pre or post processing.
 * This could be e.g. useful for a loading screen which responds to the current building or parsing state.
 * @author Trixt0r
 */
class SCMLReader {

	/**
	 * Returns the loaded SCML data.
	 * @return the SCML data.
	 */
	@JvmField
	var data: Data

	/**
	 * Creates a new SCML reader and will parse all objects in the given stream.
	 * @param stream the stream
	 */
	constructor(stream: SyncStream) {
		this.data = this.load(stream)
	}

	/**
	 * Creates a new SCML reader and will parse the given xml string.
	 * @param xml the xml string
	 */
	constructor(xml: String) {
		this.data = this.load(xml)
	}

	/**
	 * Parses the SCML object save in the given xml string and returns the build data object.
	 * @param xml the xml string
	 * *
	 * @return the built data
	 */
	protected fun load(xml: String): Data {
		return load(Xml(xml))
	}

	/**
	 * Parses the SCML objects saved in the given stream and returns the built data object.
	 * @param stream the stream from the SCML file
	 * *
	 * @return the built data
	 */
	protected fun load(stream: SyncStream): Data {
		return load(Xml(stream.readAll().toString(UTF8)))
	}

	/**
	 * Reads the data from the given root element, i.e. the spriter_data node.
	 * @param root
	 * *
	 * @return
	 */
	protected fun load(root: Xml): Data {
		val folders = root.children("folder").toList()
		val entities = root.children("entity").toList()
		val atlases = root.children("atlas")
			.flatMap { it.children("i") }
			.map { it.getString("name") ?: "" }

		data = Data(
			root.getString("scml_version") ?: "",
			root.getString("generator") ?: "",
			root.getString("generator_version") ?: "",
			Data.PixelMode[root.getInt("pixel_mode") ?: 0],
			folders.size,
			entities.size,
			atlases
		)
		loadFolders(folders)
		loadEntities(entities)
		return data
	}

	/**
	 * Iterates through the given folders and adds them to the current [Data] object.
	 * @param folders a list of folders to load
	 */
	protected fun loadFolders(folders: List<Xml>) {
		for (i in folders.indices) {
			val repo = folders[i]
			val files = repo.children("file").toList()
			val folder = Folder(repo.getInt("id") ?: 0, repo.getString("name") ?: ("no_name_$i"), files.size)
			loadFiles(files, folder)
			data.addFolder(folder)
		}
	}

	/**
	 * Iterates through the given files and adds them to the given [Folder] object.
	 * @param files a list of files to load
	 * *
	 * @param folder the folder containing the files
	 */
	protected fun loadFiles(files: List<Xml>, folder: Folder) {
		for (j in files.indices) {
			val f = files[j]
			val file = File(
				f.getInt("id") ?: 0, f.getString("name") ?: "",
				Dimension((f.getInt("width") ?: 0).toFloat(), (f.getInt("height") ?: 0).toFloat()),
				Point((f.getDouble("pivot_x") ?: 0.0).toFloat(), (f.getDouble("pivot_y") ?: 1.0).toFloat())
			)

			folder.addFile(file)
		}
	}

	/**
	 * Iterates through the given entities and adds them to the current [Data] object.
	 * @param entities a list of entities to load
	 */
	protected fun loadEntities(entities: List<Xml>) {
		for (i in entities.indices) {
			val e = entities[i]
			val infos = e.children("obj_info").toList()
			val charMaps = e.children("character_map").toList()
			val animations = e.children("animation").toList()
			val entity =
				Entity(e.getInt("id") ?: 0, e.getString("name") ?: "", animations.size, charMaps.size, infos.size)
			data.addEntity(entity)
			loadObjectInfos(infos, entity)
			loadCharacterMaps(charMaps, entity)
			loadAnimations(animations, entity)
		}
	}

	/**
	 * Iterates through the given object infos and adds them to the given [Entity] object.
	 * @param infos a list of infos to load
	 * *
	 * @param entity the entity containing the infos
	 */
	protected fun loadObjectInfos(infos: List<Xml>, entity: Entity) {
		for (i in infos.indices) {
			val info = infos[i]
			val objInfo = ObjectInfo(
				info.get("name", "info" + i),
				ObjectType.Companion.getObjectInfoFor(info.get("type", "")),
				Dimension(info.getFloat("w", 0f), info.getFloat("h", 0f))
			)
			entity.addInfo(objInfo)
			val frames = info.getChildByName("frames") ?: continue
			val frameIndices = frames.getChildrenByName("i")
			for (i1 in frameIndices.indices) {
				val index = frameIndices[i1]
				val folder = index.getInt("folder", 0)
				val file = index.getInt("file", 0)
				objInfo.frames.add(FileReference(folder, file))
			}
		}
	}

	/**
	 * Iterates through the given character maps and adds them to the given [Entity] object.
	 * @param maps a list of character maps to load
	 * *
	 * @param entity the entity containing the character maps
	 */
	protected fun loadCharacterMaps(maps: List<Xml>, entity: Entity) {
		for (i in maps.indices) {
			val map = maps[i]
			val charMap = CharacterMap(map.getInt("id", 0), map.getAttribute("name", "charMap" + i))
			entity.addCharacterMap(charMap)
			val mappings = map.getChildrenByName("map")
			for (i1 in mappings.indices) {
				val mapping = mappings[i1]
				val folder = mapping.getInt("folder", 0)
				val file = mapping.getInt("file", 0)
				charMap.put(
					FileReference(folder, file),
					FileReference(mapping.getInt("target_folder", folder), mapping.getInt("target_file", file))
				)
			}
		}
	}

	/**
	 * Iterates through the given animations and adds them to the given [Entity] object.
	 * @param animations a list of animations to load
	 * *
	 * @param entity the entity containing the animations maps
	 */
	protected fun loadAnimations(animations: List<Xml>, entity: Entity) {
		for (i in animations.indices) {
			val a = animations[i]
			val timelines = a.getChildrenByName("timeline")
			val mainline = a.getChildByName("mainline")
			val mainlineKeys = mainline!!.getChildrenByName("key")!!
			val animation = Animation(
				Mainline(mainlineKeys.size),
				a.getInt("id", 0), a.get("name", ""), a.getInt("length", 0),
				a.getBoolean("looping", true), timelines.size
			)
			entity.addAnimation(animation)
			loadMainlineKeys(mainlineKeys, animation.mainline)
			loadTimelines(timelines, animation, entity)
			animation.prepare()
		}
	}

	/**
	 * Iterates through the given mainline keys and adds them to the given [Mainline] object.
	 * @param keys a list of mainline keys
	 * *
	 * @param main the mainline
	 */
	protected fun loadMainlineKeys(keys: List<Xml>, main: Mainline) {
		for (i in main.keys.indices) {
			val k = keys[i]
			val objectRefs = k.getChildrenByName("object_ref")
			val boneRefs = k.getChildrenByName("bone_ref")
			val curve = Curve()
			curve.type = Curve.getType(k.get("curve_type", "linear"))
			curve.constraints[k.getFloat("c1", 0f), k.getFloat("c2", 0f), k.getFloat("c3", 0f)] = k.getFloat("c4", 0f)
			val key = Mainline.Key(
				k.getInt("id", 0), k.getInt("time", 0), curve,
				boneRefs.size, objectRefs.size
			)
			main.addKey(key)
			loadRefs(objectRefs, boneRefs, key)
		}
	}

	/**
	 * Iterates through the given bone and object references and adds them to the given [Mainline.Key] object.
	 * @param objectRefs a list of object references
	 * *
	 * @param boneRefs a list if bone references
	 * *
	 * @param key the mainline key
	 */
	protected fun loadRefs(objectRefs: List<Xml>, boneRefs: List<Xml>, key: Mainline.Key) {
		for (i in boneRefs.indices) {
			val e = boneRefs[i]
			val boneRef = BoneRef(
				e.getInt("id", 0), e.getInt("timeline", 0),
				e.getInt("key", 0), key.getBoneRef(e.getInt("parent", -1))
			)
			key.addBoneRef(boneRef)
		}

		for (i in objectRefs.indices) {
			val o = objectRefs[i]
			val objectRef = ObjectRef(
				o.getInt("id", 0), o.getInt("timeline", 0),
				o.getInt("key", 0), key.getBoneRef(o.getInt("parent", -1)), o.getInt("z_index", 0)
			)
			key.addObjectRef(objectRef)
		}
		key.objectRefs.sort()
	}

	/**
	 * Iterates through the given timelines and adds them to the given [Animation] object.
	 * @param timelines a list of timelines
	 * *
	 * @param animation the animation containing the timelines
	 * *
	 * @param entity entity for assigning the timeline an object info
	 */
	protected fun loadTimelines(timelines: List<Xml>, animation: Animation, entity: Entity) {
		for (i in timelines.indices) {
			val t = timelines[i]
			val keys = timelines[i].getChildrenByName("key")
			val name = t.get("name", "")
			val type = ObjectType.getObjectInfoFor(t.get("object_type", "sprite"))
			var info: ObjectInfo? = entity.getInfo(name)
			if (info == null) info = ObjectInfo(name, type, Dimension(0f, 0f))
			val timeline = Timeline(t.getInt("id", 0), name, info, keys.size)
			animation.addTimeline(timeline)
			loadTimelineKeys(keys, timeline)
		}
	}

	/**
	 * Iterates through the given timeline keys and adds them to the given [Timeline] object.
	 * @param keys a list if timeline keys
	 * *
	 * @param timeline the timeline containing the keys
	 */
	protected fun loadTimelineKeys(keys: List<Xml>, timeline: Timeline) {
		for (i in keys.indices) {
			val k = keys[i]
			val curve = Curve()
			curve.type = Curve.getType(k.get("curve_type", "linear"))
			curve.constraints[k.getFloat("c1", 0f), k.getFloat("c2", 0f), k.getFloat("c3", 0f)] = k.getFloat("c4", 0f)
			val key = Timeline.Key(k.getInt("id", 0), k.getInt("time", 0), k.getInt("spin", 1), curve)
			var obj: Xml? = k.getChildByName("bone")
			if (obj == null) obj = k.getChildByName("object")

			val position = Point(obj!!.getFloat("x", 0f), obj.getFloat("y", 0f))
			val scale = Point(obj.getFloat("scale_x", 1f), obj.getFloat("scale_y", 1f))
			var pivot = Point(
				obj.getFloat("pivot_x", 0f),
				obj.getFloat("pivot_y", if (timeline.objectInfo.type == ObjectType.Bone) .5f else 1f)
			)
			val angle = obj.getFloat("angle", 0f)
			var alpha = 1f
			var folder = -1
			var file = -1
			if (obj.name == "object") {
				if (timeline.objectInfo.type == ObjectType.Sprite) {
					alpha = obj.getFloat("a", 1f)
					folder = obj.getInt("folder", -1)
					file = obj.getInt("file", -1)
					val f = data.getFolder(folder).getFile(file)
					pivot = Point(obj.getFloat("pivot_x", f.pivot!!.x), obj.getFloat("pivot_y", f.pivot.y))
					timeline.objectInfo.size.set(f.size!!)
				}
			}
			val `object`: Timeline.Key.Object
			if (obj.name == "bone")
				`object` = Timeline.Key.Object(position, scale, pivot, angle, alpha, FileReference(folder, file))
			else
				`object` = Timeline.Key.Object(position, scale, pivot, angle, alpha, FileReference(folder, file))
			key.setObject(`object`)
			timeline.addKey(key)
		}
	}

	fun Xml.getChildrenByName(name: String) = this.children(name).toList()
	fun Xml.getChildByName(name: String) = this.child(name)
	fun Xml.get(name: String, default: String = "") = this.getString(name) ?: default
	fun Xml.getAttribute(name: String, default: String = "") = this.getString(name) ?: default
	fun Xml.getInt(name: String, default: Int) = this.getInt(name) ?: default
	fun Xml.getFloat(name: String, default: Float) = this.getDouble(name)?.toFloat() ?: default
	fun Xml.getBoolean(name: String, default: Boolean) = when (this.getString(name)) {
		null -> default
		"true", "TRUE", "1" -> true
		else -> false
	}

}

